package net.bbmsoft.worterbuch.tcp.client;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bbmsoft.worterbuch.client.api.AsyncWorterbuchClient;
import net.bbmsoft.worterbuch.tcp.client.error.DecodeException;
import net.bbmsoft.worterbuch.tcp.client.error.EncoderException;
import net.bbmsoft.worterbuch.tcp.client.futures.RequestFuture;
import net.bbmsoft.worterbuch.tcp.client.futures.ShutdownFuture;
import net.bbmsoft.worterbuch.tcp.client.messages.ClientMessage;
import net.bbmsoft.worterbuch.tcp.client.messages.ServerMessage;

@Component(scope = ServiceScope.PROTOTYPE)
public class WorterbuchAsyncTcpClient implements AsyncWorterbuchClient {

	private static final List<String> PROTOS = Collections.unmodifiableList(Arrays.asList("tcp"));

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private final ExecutorService executor = Executors
			.newSingleThreadExecutor(r -> new Thread(r, this.getClass().getSimpleName()));

	private final Map<Long, BlockingQueue<Map<String, String>>> pendingGetRequests = new ConcurrentHashMap<>();
	private final Map<Long, BlockingQueue<Object>> pendingSetRequests = new ConcurrentHashMap<>();
	private final Map<Long, BlockingQueue<BlockingQueue<Optional<Event>>>> pendingSubscribeRequests = new ConcurrentHashMap<>();
	private final Map<Long, BlockingQueue<Optional<Event>>> subscriptions = new ConcurrentHashMap<>();
	private final Object stateLock = new Object();

	private volatile Runnable onConnectionLost;

	// this field may only be accessed from within blocks synchronized on stateLock
	private State state = State.DISCONNECTED;

	// these fields may only be accessed from within the executor thread
	private Socket socket;
	private BufferedOutputStream tx;
	private RxThread rxThread;
	private long transactionID = 0;

	@Override
	public List<String> supportedProtocols() {
		return WorterbuchAsyncTcpClient.PROTOS;
	}

	@Override
	public Future<Void> connect(final URI uri) {
		this.transitionState(State.CONNECTING, State.DISCONNECTED);

		final var proto = uri.getScheme();
		if (!WorterbuchAsyncTcpClient.PROTOS.contains(proto)) {
			throw new IllegalArgumentException("Unsupported protocol: " + proto);
		}

		final var host = uri.getHost();

		final var port = uri.getPort();
		if (port == -1) {
			this.log.warn("No port specified in URI, using Wörterbuch default TCP port 4242");
		}

		return this.executor.submit(() -> this.doConnect(host, port != -1 ? port : 4242));
	}

	@Override
	public Future<Boolean> disconnect() {
		this.transitionState(State.DISCONNECTING, State.CONNECTING, State.CONNECTED);

		this.executor.submit(this::doDisconnect);
		this.executor.shutdown();

		return new ShutdownFuture(this.executor);

	}

	@Override
	public Future<Map<String, String>> get(final String pattern) {
		this.assertState(State.CONNECTING, State.CONNECTED);
		final var result = this.executor.submit(() -> this.doGet(pattern));
		return new RequestFuture<>(result);
	}

	@Override
	public Future<Object> set(final String key, final String value) {
		this.assertState(State.CONNECTING, State.CONNECTED);
		final var result = this.executor.submit(() -> this.doSet(key, value));
		return new RequestFuture<>(result);
	}

	@Override
	public Future<BlockingQueue<Optional<Event>>> subscribe(final String pattern) {
		this.assertState(State.CONNECTING, State.CONNECTED);
		final var result = this.executor.submit(() -> this.doSubscribe(pattern));
		return new RequestFuture<>(result);
	}

	@Override
	public void onConnectionLost(final Runnable action) {
		this.onConnectionLost = action;
	}

	Void doConnect(final String host, final int port) throws IOException {

		this.log.info("Connecting to server tcp://{}:{} …", host, port);

		try {
			this.socket = new Socket(host, port);
			this.rxThread = this.createRxThread(this.socket.getInputStream());
			this.tx = new BufferedOutputStream(this.socket.getOutputStream());
			this.transitionState(State.CONNECTED, State.CONNECTING);
			this.rxThread.start();
			this.log.info("Connected.");
		} catch (final IOException e) {
			this.log.error("Connection failed: {}", e.getMessage());
			this.transitionState(State.ERROR);
			throw e;
		}

		return null;
	}

	Void doDisconnect() throws IOException {
		this.log.info("Disconnecting …");

		this.rxThread.stop();

		try {
			this.socket.close();
			this.transitionState(State.DISCONNECTED, State.DISCONNECTING);
			this.log.info("Disconnected.");
		} catch (final IOException e) {
			this.transitionState(State.ERROR);
			throw e;
		}

		return null;
	}

	private BlockingQueue<Map<String, String>> doGet(final String pattern) throws EncoderException, IOException {
		this.assertState(State.CONNECTED);

		final var queue = new LinkedBlockingQueue<Map<String, String>>(1);
		final var transactionID = this.transactionID++;

		this.pendingGetRequests.put(transactionID, queue);

		try {
			this.sendGetRequest(transactionID, pattern);
		} catch (EncoderException | IOException e) {
			this.pendingGetRequests.remove(transactionID);
			throw e;
		}

		return queue;
	}

	private BlockingQueue<Object> doSet(final String key, final String value) throws EncoderException, IOException {
		this.assertState(State.CONNECTED);

		final var queue = new LinkedBlockingQueue<>(1);
		final var transactionID = this.transactionID++;

		this.pendingSetRequests.put(transactionID, queue);

		try {
			this.sendSetRequest(transactionID, key, value);
		} catch (EncoderException | IOException e) {
			this.pendingGetRequests.remove(transactionID);
			throw e;
		}

		return queue;
	}

	private BlockingQueue<BlockingQueue<Optional<Event>>> doSubscribe(final String pattern)
			throws EncoderException, IOException {
		this.assertState(State.CONNECTED);

		final var queue = new LinkedBlockingQueue<BlockingQueue<Optional<Event>>>(1);
		final var transactionID = this.transactionID++;

		this.pendingSubscribeRequests.put(transactionID, queue);

		try {
			this.sendSubscribeRequest(transactionID, pattern);
		} catch (EncoderException | IOException e) {
			this.pendingGetRequests.remove(transactionID);
			throw e;
		}

		return queue;
	}

	private RxThread createRxThread(final InputStream inputStream) {

		final var name = this.getClass().getSimpleName() + "-TCP-rx";
		final var thread = new RxThread(name, inputStream, this.log);

		return thread;
	}

	private void sendGetRequest(final long transactionID, final String pattern) throws EncoderException, IOException {
		final var bytes = ClientMessage.encodeGet(transactionID, pattern);
		this.tx.write(bytes);
		this.tx.flush();
		this.log.info("Sent GET request.");
	}

	private void sendSetRequest(final long transactionID, final String key, final String value)
			throws EncoderException, IOException {
		final var bytes = ClientMessage.encodeSet(transactionID, key, value);
		this.tx.write(bytes);
		this.tx.flush();
		this.log.info("Sent SET request.");
	}

	private void sendSubscribeRequest(final long transactionID, final String pattern)
			throws EncoderException, IOException {
		final var bytes = ClientMessage.encodeSubscribe(transactionID, pattern);
		this.tx.write(bytes);
		this.tx.flush();
		this.log.info("Sent SUBSCRIBE request.");
	}

	void distribute(final Optional<ServerMessage> message) throws InterruptedException {

		if (message.isEmpty()) {
			this.connectionLost();
		} else {
			final var msg = message.get();
			switch (msg.type()) {
			case STATE:
				this.distributeState(msg);
				break;
			case ACK:
				this.distributeAck(msg);
				break;
			case EVENT:
				this.distributeEvent(msg);
				break;
			case ERR:
				this.distributeErr(msg);
				break;
			default: {
				// ignore client messages
			}
			}
		}
	}

	private void connectionLost() {
		boolean disconnected;
		synchronized (this.stateLock) {
			disconnected = (this.state == State.DISCONNECTING) || (this.state == State.DISCONNECTED);
			if (!disconnected) {
				this.transitionState(State.DISCONNECTED);
			}
		}

		if (!disconnected) {
			this.log.error("Connection to server closed, shutting down.");
			final var onConnectionLost = this.onConnectionLost;
			if (onConnectionLost != null) {
				onConnectionLost.run();
			}
		}
	}

	private void distributeState(final ServerMessage msg) throws InterruptedException {
		final var consumer = this.pendingGetRequests.remove(msg.transactionID());
		if (consumer != null) {
			consumer.put(msg.keyValuePairs().get());
		}
	}

	private void distributeAck(final ServerMessage msg) throws InterruptedException {
		{
			final var consumer = this.pendingSetRequests.remove(msg.transactionID());
			if (consumer != null) {
				consumer.put(new Object());
			}
		}

		{
			final var consumer = this.pendingSubscribeRequests.remove(msg.transactionID());
			if (consumer != null) {
				final var events = new LinkedBlockingQueue<Optional<Event>>(1000);
				this.subscriptions.put(msg.transactionID(), events);
				consumer.put(events);
			}
		}
	}

	private void distributeEvent(final ServerMessage msg) throws InterruptedException {
		final var consumer = this.subscriptions.get(msg.transactionID());
		if (consumer != null) {
			for (final var entry : msg.keyValuePairs().get().entrySet()) {
				consumer.put(Optional.of(new Event(entry.getKey(), entry.getValue())));
			}
		}
	}

	private void distributeErr(final ServerMessage msg) {
		// TODO Auto-generated method stub

	}

	private void assertState(final State... legalSourceStates) {
		final List<State> legalStateList = Arrays.asList(legalSourceStates);
		synchronized (this.state) {
			if (!legalStateList.contains(this.state)) {
				throw new IllegalStateException(
						"Client is currently " + this.state + ", this method may only be called when " + String.join(
								" or ", legalStateList.stream().map(State::toString).collect(Collectors.toList())));
			}
		}
	}

	private void transitionState(final State targetState, final State... legalSourceStates) {
		final List<State> legalStateList = Arrays.asList(legalSourceStates);
		synchronized (this.state) {
			if (!legalStateList.isEmpty() && !legalStateList.contains(this.state)) {
				throw new IllegalStateException(
						"Client is currently " + this.state + ", this method may only be called when " + String.join(
								" or ", legalStateList.stream().map(State::toString).collect(Collectors.toList())));
			}
			this.state = targetState;
		}
	}

	private class RxThread {

		private final InputStream inputStream;
		private final Logger log;

		private volatile boolean stopped;
		private volatile Thread thread;

		RxThread(final String name, final InputStream inputStream, final Logger log) {
			this.inputStream = inputStream;
			this.log = log;
			this.thread = new Thread(this::run, name);
		}

		public void start() {
			this.thread.start();
		}

		public void stop() {
			this.stopped = true;
		}

		private void run() {
			while (!this.stopped && !this.thread.isInterrupted()) {
				try {
					final var message = ServerMessage.read(this.inputStream);
					if (message.isEmpty()) {
						this.log.info("Connection closed, terminating receiver thread.");
						break;
					} else {
						WorterbuchAsyncTcpClient.this.distribute(message);
					}
				} catch (final DecodeException e) {
					this.log.warn("Received malformed message from server, terminating receiver thread.");
					break;
				} catch (final InterruptedException e) {
					this.log.warn("Receiver thread interrupted, terminating.");
					break;
				}
			}
		}
	}
}
