package net.bbmsoft.worterbuch.tcp.client;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import java.util.function.Consumer;
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
import net.bbmsoft.worterbuch.tcp.client.messages.AckMessage;
import net.bbmsoft.worterbuch.tcp.client.messages.ClientMessageEncoder;
import net.bbmsoft.worterbuch.tcp.client.messages.ErrMessage;
import net.bbmsoft.worterbuch.tcp.client.messages.HandshakeMessage;
import net.bbmsoft.worterbuch.tcp.client.messages.MessageType;
import net.bbmsoft.worterbuch.tcp.client.messages.PStateMessage;
import net.bbmsoft.worterbuch.tcp.client.messages.ServerMessage;
import net.bbmsoft.worterbuch.tcp.client.messages.ServerMessageDecoder;
import net.bbmsoft.worterbuch.tcp.client.messages.StateMessage;

@Component(scope = ServiceScope.PROTOTYPE)
public class WorterbuchAsyncTcpClient implements AsyncWorterbuchClient {

	private static final List<String> PROTOS = Collections.unmodifiableList(Arrays.asList("tcp"));

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private final ExecutorService executor = Executors
			.newSingleThreadExecutor(r -> new Thread(r, this.getClass().getSimpleName()));

	private final Map<Long, BlockingQueue<Optional<String>>> pendingGetRequests = new ConcurrentHashMap<>();
	private final Map<Long, BlockingQueue<Map<String, String>>> pendingPGetRequests = new ConcurrentHashMap<>();
	private final Map<Long, BlockingQueue<Void>> pendingSetRequests = new ConcurrentHashMap<>();
	private final Map<Long, SubscribeRequest> pendingSubscribeRequests = new ConcurrentHashMap<>();
	private final Map<Long, Consumer<Optional<Event>>> subscriptions = new ConcurrentHashMap<>();
	private final Object stateLock = new Object();

	private volatile Runnable onConnectionLost;

	// this field may only be accessed from within blocks synchronized on stateLock
	private State state = State.DISCONNECTED;

	// these fields may only be accessed from within the executor thread
	private final ClientMessageEncoder enc = new ClientMessageEncoder();
	private Socket socket;
	private BufferedOutputStream tx;
	private RxThread rxThread;
	private long transactionID = 0;

	@Override
	public List<String> supportedProtocols() {
		return WorterbuchAsyncTcpClient.PROTOS;
	}

	@Override
	public Future<Handshake> connect(final URI uri) {
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
	public Future<Optional<String>> get(final String key) {
		this.assertState(State.CONNECTING, State.CONNECTED);
		final var result = this.executor.submit(() -> this.doGet(key));
		return new RequestFuture<>(result);
	}

	@Override
	public Future<Map<String, String>> pget(final String pattern) {
		this.assertState(State.CONNECTING, State.CONNECTED);
		final var result = this.executor.submit(() -> this.doPGet(pattern));
		return new RequestFuture<>(result);
	}

	@Override
	public Future<Void> set(final String key, final String value) {
		this.assertState(State.CONNECTING, State.CONNECTED);
		final var result = this.executor.submit(() -> this.doSet(key, value));
		return new RequestFuture<>(result);
	}

	@Override
	public Future<Void> subscribe(final String key, final Consumer<Optional<Event>> onEvent, boolean unique) {
		this.assertState(State.CONNECTING, State.CONNECTED);
		final var result = this.executor.submit(() -> this.doSubscribe(key, onEvent, unique));
		return new RequestFuture<>(result);
	}

	@Override
	public Future<Void> psubscribe(final String pattern, final Consumer<Optional<Event>> onEvent, boolean unique) {
		this.assertState(State.CONNECTING, State.CONNECTED);
		final var result = this.executor.submit(() -> this.doPSubscribe(pattern, onEvent, unique));
		return new RequestFuture<>(result);
	}

	@Override
	public void onConnectionLost(final Runnable action) {
		this.onConnectionLost = action;
	}

	Handshake doConnect(final String host, final int port) throws IOException, DecodeException {

		this.log.info("Connecting to server tcp://{}:{} …", host, port);

		Handshake handshake;

		try {
			this.socket = new Socket(host, port);

			InputStream inputStream = this.socket.getInputStream();
			OutputStream outputStream = this.socket.getOutputStream();

			var dec = new ServerMessageDecoder();
			var handshakeMessage = dec.read(inputStream);

			if (handshakeMessage.isEmpty()) {
				throw new IOException("Connection closed before handshake was received.");
			} else if (!MessageType.HSHK.equals(handshakeMessage.get().type())) {
				throw new IOException("Server sent invalid handshake message: " + handshakeMessage.get());
			} else {
				handshake = ((HandshakeMessage) handshakeMessage.get()).handshake();
			}

			this.rxThread = this.createRxThread(dec, inputStream);
			this.tx = new BufferedOutputStream(outputStream);
			this.transitionState(State.CONNECTED, State.CONNECTING);
			this.rxThread.start();
			this.log.info("Connected.");
		} catch (final IOException e) {
			this.log.error("Connection failed: {}", e.getMessage());
			this.transitionState(State.ERROR);
			throw e;
		}

		return handshake;
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

		return new Void();
	}

	private BlockingQueue<Optional<String>> doGet(final String key) throws EncoderException, IOException {
		this.assertState(State.CONNECTED);

		final var queue = new LinkedBlockingQueue<Optional<String>>(1);
		final var transactionID = this.transactionID++;

		this.pendingGetRequests.put(transactionID, queue);

		try {
			this.sendGetRequest(transactionID, key);
		} catch (EncoderException | IOException e) {
			this.pendingGetRequests.remove(transactionID);
			throw e;
		}

		return queue;
	}

	private BlockingQueue<Map<String, String>> doPGet(final String pattern) throws EncoderException, IOException {
		this.assertState(State.CONNECTED);

		final var queue = new LinkedBlockingQueue<Map<String, String>>(1);
		final var transactionID = this.transactionID++;

		this.pendingPGetRequests.put(transactionID, queue);

		try {
			this.sendPGetRequest(transactionID, pattern);
		} catch (EncoderException | IOException e) {
			this.pendingPGetRequests.remove(transactionID);
			throw e;
		}

		return queue;
	}

	private BlockingQueue<Void> doSet(final String key, final String value) throws EncoderException, IOException {
		this.assertState(State.CONNECTED);

		final var queue = new LinkedBlockingQueue<Void>(1);
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

	private BlockingQueue<Void> doSubscribe(final String key, final Consumer<Optional<Event>> onEvent, boolean unique)
			throws EncoderException, IOException {
		this.assertState(State.CONNECTED);

		final var queue = new LinkedBlockingQueue<Void>(1);
		final var transactionID = this.transactionID++;

		this.pendingSubscribeRequests.put(transactionID, new SubscribeRequest(onEvent, queue));

		try {
			this.sendSubscribeRequest(transactionID, key, unique);
		} catch (EncoderException | IOException e) {
			this.pendingGetRequests.remove(transactionID);
			throw e;
		}

		return queue;
	}

	private BlockingQueue<Void> doPSubscribe(final String pattern, final Consumer<Optional<Event>> onEvent, boolean unique)
			throws EncoderException, IOException {
		this.assertState(State.CONNECTED);

		final var queue = new LinkedBlockingQueue<Void>(1);
		final var transactionID = this.transactionID++;

		this.pendingSubscribeRequests.put(transactionID, new SubscribeRequest(onEvent, queue));

		try {
			this.sendPSubscribeRequest(transactionID, pattern, unique);
		} catch (EncoderException | IOException e) {
			this.pendingGetRequests.remove(transactionID);
			throw e;
		}

		return queue;
	}

	private RxThread createRxThread(ServerMessageDecoder dec, final InputStream inputStream) {

		final var name = this.getClass().getSimpleName() + "-TCP-rx";
		final var thread = new RxThread(name, dec, inputStream, this.log);

		return thread;
	}

	private void sendGetRequest(final long transactionID, final String key) throws EncoderException, IOException {
		final var bytes = this.enc.encodeGet(transactionID, key);
		this.tx.write(bytes);
		this.tx.flush();
	}

	private void sendPGetRequest(final long transactionID, final String pattern) throws EncoderException, IOException {
		final var bytes = this.enc.encodePGet(transactionID, pattern);
		this.tx.write(bytes);
		this.tx.flush();
	}

	private void sendSetRequest(final long transactionID, final String key, final String value)
			throws EncoderException, IOException {
		final var bytes = this.enc.encodeSet(transactionID, key, value);
		this.tx.write(bytes);
		this.tx.flush();
	}

	private void sendSubscribeRequest(final long transactionID, final String key, boolean unique) throws EncoderException, IOException {
		final var bytes = this.enc.encodeSubscribe(transactionID, key, unique);
		this.tx.write(bytes);
		this.tx.flush();
	}

	private void sendPSubscribeRequest(final long transactionID, final String pattern, boolean unique)
			throws EncoderException, IOException {
		final var bytes = this.enc.encodePSubscribe(transactionID, pattern, unique);
		this.tx.write(bytes);
		this.tx.flush();
	}

	void distribute(final ServerMessage message) throws InterruptedException {

		switch (message.type()) {
		case PSTATE:
			this.distributePState((PStateMessage) message);
			break;
		case ACK:
			this.distributeAck((AckMessage) message);
			break;
		case STATE:
			this.distributeState((StateMessage) message);
			break;
		case ERR:
			this.distributeErr((ErrMessage) message);
			break;
		case HSHK:
			throw new IllegalStateException("Server should not be sending handshakes anymore!");
		default: {
			// ignore client messages
		}
		}
	}

	private void connectionLost() {
		boolean disconnected;
		synchronized (this.stateLock) {
			disconnected = (this.state == State.DISCONNECTING) || (this.state == State.DISCONNECTED);
			if (!disconnected) {
				this.disconnect();
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

	private void distributeState(final StateMessage msg) throws InterruptedException {

		final var key = msg.key();
		final var value = msg.value();

		final var consumer = this.pendingGetRequests.remove(msg.transactionId());
		if (consumer != null) {
			consumer.put(value);
		}

		final var subscription = this.subscriptions.get(msg.transactionId());
		if (subscription != null) {
			value.ifPresent(v -> subscription.accept(Optional.of(new Event(key, v))));
		}

	}

	private void distributeAck(final AckMessage msg) throws InterruptedException {
		{
			final var consumer = this.pendingSetRequests.remove(msg.transactionId());
			if (consumer != null) {
				consumer.put(new Void());
			}
		}

		{
			final var consumer = this.pendingSubscribeRequests.remove(msg.transactionId());
			if (consumer != null) {
				this.subscriptions.put(msg.transactionId(), consumer.onEvent);
				consumer.result.put(new Void());
			}
		}
	}

	private void distributePState(final PStateMessage msg) throws InterruptedException {
		final var consumer = this.pendingPGetRequests.remove(msg.transactionId());

		final var values = msg.keyValuePairs();

		if (consumer != null) {
			consumer.put(values);
		}

		final var subscription = this.subscriptions.get(msg.transactionId());
		if (subscription != null) {
			for (final var entry : values.entrySet()) {
				subscription.accept(Optional.of(new Event(entry.getKey(), entry.getValue())));
			}
		}
	}

	private void distributeErr(final ErrMessage msg) {
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
		private final ServerMessageDecoder dec;

		private volatile boolean stopped;
		private volatile Thread thread;

		RxThread(final String name, ServerMessageDecoder dec, final InputStream inputStream, final Logger log) {
			this.inputStream = inputStream;
			this.log = log;
			this.dec = dec;
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
					final var message = this.dec.read(this.inputStream);
					if (message.isEmpty()) {
						this.log.info("Connection closed, terminating receiver thread.");
						WorterbuchAsyncTcpClient.this.connectionLost();
						break;
					} else {
						WorterbuchAsyncTcpClient.this.distribute(message.get());
					}
				} catch (final DecodeException e) {
					this.log.warn("Received malformed message from server, terminating receiver thread.");
					WorterbuchAsyncTcpClient.this.connectionLost();
					break;
				} catch (final InterruptedException e) {
					this.log.warn("Receiver thread interrupted, terminating.");
					break;
				}
			}
		}
	}

	private record SubscribeRequest(Consumer<Optional<Event>> onEvent, BlockingQueue<Void> result) {
	}

}
