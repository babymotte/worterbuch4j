package net.bbmsoft.worterbuch.client.impl;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bbmsoft.worterbuch.client.api.Constants;
import net.bbmsoft.worterbuch.client.api.WorterbuchClient;
import net.bbmsoft.worterbuch.client.api.WorterbuchException;
import net.bbmsoft.worterbuch.client.error.Error;
import net.bbmsoft.worterbuch.client.error.WorterbuchError;
import net.bbmsoft.worterbuch.client.model.Welcome;

public class Connector {

	private final static Logger log = LoggerFactory.getLogger(Connector.class);
	private final Iterable<URI> uris;
	private final Optional<String> authToken;
	private final Executor callbackExecutor;
	private final BiConsumer<Integer, String> onDisconnect;
	private final Consumer<WorterbuchException> onError;
	private final WrappingExecutor exec;

	public Connector(final Iterable<URI> uris, final Optional<String> authToken,
			final Optional<ScheduledExecutorService> callbackExecutor, final BiConsumer<Integer, String> onDisconnect,
			final Consumer<WorterbuchException> onError) {

		Objects.requireNonNull(uris);
		Objects.requireNonNull(authToken);
		Objects.requireNonNull(onDisconnect);
		Objects.requireNonNull(onError);

		this.uris = uris;
		this.authToken = authToken;
		this.onDisconnect = onDisconnect;
		this.onError = onError;
		this.exec = new WrappingExecutor(
				Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "worterbuch-client")), this.onError);
		this.callbackExecutor = new WrappingExecutor(callbackExecutor.orElseGet(
				() -> Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "worterbuch-client-callbacks"))),
				this.onError);
	}

	public WorterbuchClient connect() throws TimeoutException, WorterbuchException {

		WorterbuchClient client = null;

		for (final URI uri : this.uris) {
			try {
				client = this.initWorterbuchClient(uri);
				break;
			} catch (final Throwable e) {
				Connector.log.warn("Could not connect to server {}: {}", uri, e.getMessage());
			}
		}

		if (client == null) {
			throw new WorterbuchException("Could not connect to any server.");
		} else {
			return client;
		}
	}

	private WorterbuchClient initWorterbuchClient(final URI uri) throws Throwable {

		if (uri.getScheme().equals("tcp")) {
			return this.initTcpWorterbuchClient(uri);
		} else {
			return this.initWsWorterbuchClient(uri, this.authToken);
		}

	}

	private WorterbuchClient initWsWorterbuchClient(final URI uri, final Optional<String> authtoken) throws Throwable {

		final var clientSocket = new WsClientSocket(uri, this.onError, authtoken);

		final var preConnectError = new LinkedBlockingQueue<Optional<Throwable>>();
		final var handshakeLatch = new LinkedBlockingQueue<Optional<Throwable>>();
		final var wb = new WorterbuchClientImpl(clientSocket, this.exec, this.onError);

		wb.start(w -> this.onWelcome(w, wb, handshakeLatch));

		final Consumer<WorterbuchException> preConnectErrorHandler = e -> {
			try {
				preConnectError.offer(Optional.of(e), Config.CONNECT_TIMEOUT, TimeUnit.SECONDS);
			} catch (final InterruptedException e1) {
				Thread.currentThread().interrupt();
			}
		};

		final Consumer<WorterbuchException> postConnectErrorHandler = e -> this.exec
				.execute(() -> this.onError.accept(e));

		final var errorHandler = new AtomicReference<>(preConnectErrorHandler);

		final var socket = new WebSocketAdapter() {
			@Override
			public void onWebSocketText(final String message) {
				Connector.this.exec.execute(() -> {
					wb.messageReceived(message, Connector.this.callbackExecutor);
				});
			}

			@Override
			public void onWebSocketClose(final int statusCode, final String reason) {
				Connector.this.exec.execute(() -> {
					wb.close();
					Connector.this.onDisconnect.accept(statusCode, reason);
				});
			}

			@Override
			public void onWebSocketConnect(final Session sess) {
				try {
					preConnectError.offer(Optional.empty(), Config.CONNECT_TIMEOUT, TimeUnit.SECONDS);
					errorHandler.set(postConnectErrorHandler);
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}

			@Override
			public void onWebSocketError(final Throwable cause) {
				errorHandler.get().accept(new WorterbuchException("error in websocket connection", cause));
			}
		};

		clientSocket.open(socket);

		final var error = preConnectError.poll(Config.CONNECT_TIMEOUT, TimeUnit.SECONDS);
		if (error == null) {
			wb.close();
			throw new WorterbuchException("connection attempt timed out");
		} else if (error.isPresent()) {
			wb.close();
			throw error.get();
		}

		final var handshakeError = handshakeLatch.poll(Config.CONNECT_TIMEOUT, TimeUnit.SECONDS);
		if (handshakeError == null) {
			wb.close();
			throw new WorterbuchException("connection attempt timed out");
		} else if (handshakeError.isPresent()) {
			wb.close();
			throw handshakeError.get();
		}

		return wb;
	}

	private WorterbuchClient initTcpWorterbuchClient(final URI uri) throws Throwable {

		final var clientSocket = new TcpClientSocket(uri, this.onDisconnect, this.onError, Config.CHANNEL_BUFFER_SIZE);

		final var handshakeLatch = new LinkedBlockingQueue<Optional<Throwable>>();
		final var wb = new WorterbuchClientImpl(clientSocket, this.exec, this.onError);

		wb.start(w -> this.onWelcome(w, wb, handshakeLatch));

		clientSocket.open(msg -> wb.messageReceived(msg, this.callbackExecutor), Config.SEND_TIMEOUT, TimeUnit.SECONDS);

		final var handshakeError = handshakeLatch.poll(Config.CONNECT_TIMEOUT, TimeUnit.SECONDS);
		if (handshakeError == null) {
			throw new WorterbuchException("connection attempt timed out");
		} else if (handshakeError.isPresent()) {
			throw handshakeError.get();
		}

		return wb;

	}

	private void onWelcome(final Welcome welcome, final WorterbuchClientImpl client,
			final LinkedBlockingQueue<Optional<Throwable>> latch) {
		final var serverInfo = welcome.getInfo();

		final var protoVersion = Protocol.compatibleProtocolVersion(serverInfo.getSupportedProtocolVersions());

		if (protoVersion.isEmpty()) {

			latch.add(Optional.of(new WorterbuchException(
					"Protocol version " + Constants.PROTOCOL_VERSION + " is not supported by the server.")));

		} else {

			client.clientId = welcome.getClientId();

			client.switchProtocol(protoVersion.get()).result().thenAccept(swres -> {
				if (swres instanceof final Error<Void> err) {
					latch.add(Optional.of(new WorterbuchException(new WorterbuchError(err.err()))));
					return;
				}

				if (serverInfo.isAuthorizationRequired()) {
					if (this.authToken.isEmpty()) {
						this.onError.accept(new WorterbuchException(
								"server requires authorization but no auth token was provided"));
						return;
					}
					client.authorize(this.authToken.get()).result().thenAccept(aures -> {
						if (aures instanceof final Error<Void> err) {
							latch.add(Optional.of(new WorterbuchException(new WorterbuchError(err.err()))));
							return;
						}
						latch.add(Optional.empty());
					});
				} else {
					latch.add(Optional.empty());
				}
			});

		}

	}
}
