/*
 *  Worterbuch Java client library
 *
 *  Copyright (C) 2024 Michael Bachmann
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.bbmsoft.worterbuch.client;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bbmsoft.worterbuch.client.api.Constants;
import net.bbmsoft.worterbuch.client.api.TypedKeyValuePair;
import net.bbmsoft.worterbuch.client.api.TypedPStateEvent;
import net.bbmsoft.worterbuch.client.api.WorterbuchClient;
import net.bbmsoft.worterbuch.client.api.WorterbuchException;
import net.bbmsoft.worterbuch.client.api.util.Tuple;
import net.bbmsoft.worterbuch.client.error.SerializationFailed;
import net.bbmsoft.worterbuch.client.error.WorterbuchError;
import net.bbmsoft.worterbuch.client.impl.Config;
import net.bbmsoft.worterbuch.client.impl.TcpClientSocket;
import net.bbmsoft.worterbuch.client.impl.WrappingExecutor;
import net.bbmsoft.worterbuch.client.impl.WsClientSocket;
import net.bbmsoft.worterbuch.client.model.AuthorizationRequest;
import net.bbmsoft.worterbuch.client.model.CGet;
import net.bbmsoft.worterbuch.client.model.CSet;
import net.bbmsoft.worterbuch.client.model.ClientMessage;
import net.bbmsoft.worterbuch.client.model.Delete;
import net.bbmsoft.worterbuch.client.model.Err;
import net.bbmsoft.worterbuch.client.model.Get;
import net.bbmsoft.worterbuch.client.model.KeyValuePair;
import net.bbmsoft.worterbuch.client.model.Lock;
import net.bbmsoft.worterbuch.client.model.Ls;
import net.bbmsoft.worterbuch.client.model.PDelete;
import net.bbmsoft.worterbuch.client.model.PGet;
import net.bbmsoft.worterbuch.client.model.PLs;
import net.bbmsoft.worterbuch.client.model.PSubscribe;
import net.bbmsoft.worterbuch.client.model.ProtocolSwitchRequest;
import net.bbmsoft.worterbuch.client.model.Publish;
import net.bbmsoft.worterbuch.client.model.ReleaseLock;
import net.bbmsoft.worterbuch.client.model.SPub;
import net.bbmsoft.worterbuch.client.model.SPubInit;
import net.bbmsoft.worterbuch.client.model.Set;
import net.bbmsoft.worterbuch.client.model.Subscribe;
import net.bbmsoft.worterbuch.client.model.SubscribeLs;
import net.bbmsoft.worterbuch.client.model.Unsubscribe;
import net.bbmsoft.worterbuch.client.model.UnsubscribeLs;
import net.bbmsoft.worterbuch.client.model.Welcome;
import net.bbmsoft.worterbuch.client.pending.LsSubscription;
import net.bbmsoft.worterbuch.client.pending.PSubscription;
import net.bbmsoft.worterbuch.client.pending.PendingCGet;
import net.bbmsoft.worterbuch.client.pending.PendingCSet;
import net.bbmsoft.worterbuch.client.pending.PendingDelete;
import net.bbmsoft.worterbuch.client.pending.PendingGet;
import net.bbmsoft.worterbuch.client.pending.PendingLock;
import net.bbmsoft.worterbuch.client.pending.PendingLsState;
import net.bbmsoft.worterbuch.client.pending.PendingPDelete;
import net.bbmsoft.worterbuch.client.pending.PendingPGet;
import net.bbmsoft.worterbuch.client.pending.PendingReleaseLock;
import net.bbmsoft.worterbuch.client.pending.PendingSPubInit;
import net.bbmsoft.worterbuch.client.pending.Subscription;

public class WorterbuchClientImpl implements WorterbuchClient {

	private final static Logger log = LoggerFactory.getLogger(WorterbuchClientImpl.class);

	public static WorterbuchClientImpl connect(final Iterable<URI> uris, final BiConsumer<Integer, String> onDisconnect,
			final Consumer<Throwable> onError) throws InterruptedException, TimeoutException, WorterbuchException {

		final var callbackExecutor = new WrappingExecutor(
				Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "worterbuch-client-callbacks")), onError);

		return WorterbuchClientImpl.connect(uris, Optional.empty(), callbackExecutor, onDisconnect, onError);
	}

	public static WorterbuchClientImpl connect(final Iterable<URI> uris, final String authToken,
			final BiConsumer<Integer, String> onDisconnect, final Consumer<Throwable> onError)
			throws InterruptedException, TimeoutException, WorterbuchException {

		final var callbackExecutor = new WrappingExecutor(
				Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "worterbuch-client-callbacks")), onError);
		return WorterbuchClientImpl.connect(uris, Optional.of(authToken), callbackExecutor, onDisconnect, onError);
	}

	public static WorterbuchClientImpl connect(final Iterable<URI> uris, final Optional<String> authToken,
			final Executor callbackExecutor, final BiConsumer<Integer, String> onDisconnect,
			final Consumer<Throwable> onError) throws TimeoutException, WorterbuchException {

		Objects.requireNonNull(uris);
		Objects.requireNonNull(onDisconnect);
		Objects.requireNonNull(onError);

		final var exec = new WrappingExecutor(
				Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "worterbuch-client")), onError);

		WorterbuchClientImpl wb = null;

		for (final URI uri : uris) {
			try {
				wb = WorterbuchClientImpl.initWorterbuchClient(uri, authToken, onDisconnect, onError, exec,
						Objects.requireNonNull(callbackExecutor));
				break;
			} catch (final Throwable e) {
				WorterbuchClientImpl.log.warn("Could not connect to server {}: {}", uri, e.getMessage());
			}
		}

		if (wb == null) {
			throw new WorterbuchException("Could not connect to any server.");
		}

		return wb;
	}

	private static WorterbuchClientImpl initWorterbuchClient(final URI uri, final Optional<String> authtoken,
			final BiConsumer<Integer, String> onDisconnect, final Consumer<? super Throwable> onError,
			final ScheduledExecutorService exec, final Executor callbackExecutor) throws Throwable {

		if (uri.getScheme().equals("tcp")) {
			return WorterbuchClientImpl.initTcpWorterbuchClient(uri, authtoken, onDisconnect, onError, exec,
					callbackExecutor);
		} else {
			return WorterbuchClientImpl.initWsWorterbuchClient(uri, authtoken, onDisconnect, onError, exec,
					callbackExecutor);
		}

	}

	private static void onWelcome(final Welcome welcome, final WorterbuchClientImpl client,
			final LinkedBlockingQueue<Optional<Throwable>> latch, final Optional<String> authToken) {
		try {
			final var serverInfo = welcome.getInfo();

			final var protoVersion = client.compatibleProtocolVersion(serverInfo.getSupportedProtocolVersions());

			if (protoVersion.isEmpty()) {

				try {
					latch.offer(Optional.of(new WorterbuchException(
							"Protocol version " + Constants.PROTOCOL_VERSION + " is not supported by the server.")),
							Config.CONNECT_TIMEOUT, TimeUnit.SECONDS);
				} catch (final InterruptedException e1) {
					Thread.currentThread().interrupt();
				}

			} else {

				client.clientId = welcome.getClientId();

				client.switchProtocol(protoVersion.get());

				if (serverInfo.isAuthorizationRequired()) {
					client.authorize(authToken);
				}
			}
		} catch (final Throwable e) {
			try {
				latch.offer(Optional.of(e), Config.CONNECT_TIMEOUT, TimeUnit.SECONDS);
			} catch (final InterruptedException e1) {
				Thread.currentThread().interrupt();
			}
		} finally {
			try {
				latch.offer(Optional.empty(), Config.CONNECT_TIMEOUT, TimeUnit.SECONDS);
			} catch (final InterruptedException e1) {
				Thread.currentThread().interrupt();
			}
		}

	}

	private static WorterbuchClientImpl initWsWorterbuchClient(final URI uri, final Optional<String> authtoken,
			final BiConsumer<Integer, String> onDisconnect, final Consumer<? super Throwable> onError,
			final ScheduledExecutorService exec, final Executor callbackExecutor) throws Throwable {

		final var clientSocket = new WsClientSocket(uri, onError, authtoken);

		final var preConnectError = new LinkedBlockingQueue<Optional<Throwable>>();
		final var handshakeLatch = new LinkedBlockingQueue<Optional<Throwable>>();
		final var wb = new WorterbuchClientImpl(exec,
				(welcome, client) -> WorterbuchClientImpl.onWelcome(welcome, client, handshakeLatch, authtoken),
				onDisconnect, onError, clientSocket);

		final Consumer<Throwable> preConnectErrorHandler = e -> {
			try {
				preConnectError.offer(Optional.of(e), Config.CONNECT_TIMEOUT, TimeUnit.SECONDS);
			} catch (final InterruptedException e1) {
				Thread.currentThread().interrupt();
			}
		};
		final Consumer<Throwable> postConnectErrorHandler = e -> exec.execute(() -> wb.onError(e));
		final var errorHandler = new AtomicReference<>(preConnectErrorHandler);

		final var socket = new WebSocketAdapter() {
			@Override
			public void onWebSocketText(final String message) {
				exec.execute(() -> wb.messageReceived(message, callbackExecutor));
			}

			@Override
			public void onWebSocketClose(final int statusCode, final String reason) {
				exec.execute(() -> wb.onDisconnect(statusCode, reason));
			}

			@Override
			public void onWebSocketConnect(final Session sess) {
				errorHandler.set(postConnectErrorHandler);
				try {
					preConnectError.offer(Optional.empty(), Config.CONNECT_TIMEOUT, TimeUnit.SECONDS);
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}

			@Override
			public void onWebSocketError(final Throwable cause) {
				errorHandler.get().accept(cause);
			}
		};

		clientSocket.open(socket);

		final var error = preConnectError.poll(Config.CONNECT_TIMEOUT, TimeUnit.SECONDS);
		if (error == null) {
			throw new WorterbuchException("connection attempt timed out");
		} else if (error.isPresent()) {
			throw error.get();
		}

		final var handshakeError = handshakeLatch.poll(Config.CONNECT_TIMEOUT, TimeUnit.SECONDS);
		if (handshakeError == null) {
			throw new WorterbuchException("connection attempt timed out");
		} else if (handshakeError.isPresent()) {
			throw handshakeError.get();
		}

		return wb;
	}

	private static WorterbuchClientImpl initTcpWorterbuchClient(final URI uri, final Optional<String> authtoken,
			final BiConsumer<Integer, String> onDisconnect, final Consumer<? super Throwable> onError,
			final ScheduledExecutorService exec, final Executor callbackExecutor) throws Throwable {

		final var clientSocket = new TcpClientSocket(uri, onDisconnect, onError, Config.CHANNEL_BUFFER_SIZE);

		final var handshakeLatch = new LinkedBlockingQueue<Optional<Throwable>>();
		final var wb = new WorterbuchClientImpl(exec,
				(welcome, client) -> WorterbuchClientImpl.onWelcome(welcome, client, handshakeLatch, authtoken),
				onDisconnect, onError, clientSocket);

		clientSocket.open(msg -> wb.messageReceived(msg, callbackExecutor), Config.SEND_TIMEOUT, TimeUnit.SECONDS);

		final var handshakeError = handshakeLatch.poll(Config.CONNECT_TIMEOUT, TimeUnit.SECONDS);
		if (handshakeError == null) {
			throw new WorterbuchException("connection attempt timed out");
		} else if (handshakeError.isPresent()) {
			throw handshakeError.get();
		}

		return wb;

	}

	private final ScheduledExecutorService exec;
	private final BiConsumer<Integer, String> onDisconnect;
	private final Consumer<? super Throwable> onError;
	private final ClientSocket client;

	private final AtomicLong transactionId = new AtomicLong();
	private final ObjectMapper objectMapper = new ObjectMapper();

	private final Map<Long, PendingGet<?>> pendingGets = new ConcurrentHashMap<>();
	private final Map<Long, PendingCGet<?>> pendingCGets = new ConcurrentHashMap<>();
	private final Map<Long, PendingCSet> pendingCSets = new ConcurrentHashMap<>();
	private final Map<Long, PendingPGet<?>> pendingPGets = new ConcurrentHashMap<>();
	private final Map<Long, PendingSPubInit<?>> pendingSPubInits = new ConcurrentHashMap<>();
	private final Map<Long, PendingDelete<?>> pendingDeletes = new ConcurrentHashMap<>();
	private final Map<Long, PendingPDelete<?>> pendingPDeletes = new ConcurrentHashMap<>();
	private final Map<Long, PendingLsState> pendingLsStates = new ConcurrentHashMap<>();
	private final Map<Long, Subscription<?>> subscriptions = new ConcurrentHashMap<>();
	private final Map<Long, PSubscription<?>> pSubscriptions = new ConcurrentHashMap<>();
	private final Map<Long, LsSubscription> lsSubscriptions = new ConcurrentHashMap<>();
	private final Map<Long, PendingLock> pendingLocks = new ConcurrentHashMap<>();
	private final Map<Long, PendingReleaseLock> pendingReleaseLocks = new ConcurrentHashMap<>();

	private final AtomicBoolean welcomeReceived = new AtomicBoolean();
	private final AtomicBoolean closing = new AtomicBoolean();

	private boolean disconnected;
	private final BiConsumer<Welcome, WorterbuchClientImpl> onWelcome;

	private String clientId;

	WorterbuchClientImpl(final ScheduledExecutorService exec, final BiConsumer<Welcome, WorterbuchClientImpl> onWelcome,
			final BiConsumer<Integer, String> onDisconnect, final Consumer<? super Throwable> onError,
			final ClientSocket client) {
		this.exec = exec;
		this.onWelcome = onWelcome;
		this.onDisconnect = onDisconnect;
		this.onError = onError;
		this.client = client;
	}

	@Override
	public void close() {
		this.exec.execute(this::doClose);
	}

	private long acquireTid() {
		final var nextTid = this.transactionId.incrementAndGet();
		return nextTid;
	}

	@Override
	public <T> void set(final String key, final T value) {
		final var tid = this.acquireTid();
		String json;
		try {
			json = this.toJson(this.setMessage(tid, key, value));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doSet(json, this.onError));
	}

	@Override
	public <T> void publish(final String key, final T value) {
		final var tid = this.acquireTid();
		String json;
		try {
			json = this.toJson(this.publishMessage(tid, key, value));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doPublish(json, this.onError));
	}

	@Override
	public <T> CompletableFuture<Long> initPubStream(final String key) {
		final var fut = new CompletableFuture<Long>();
		final var tid = this.acquireTid();
		String json;
		try {
			json = this.toJson(this.initPubStreamMessage(tid, key));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doInitPubStream(tid, json, fut));
		return fut;
	}

	@Override
	public <T> void streamPub(final long transactionId, final T value) {
		String json;
		try {
			json = this.toJson(this.streamPubMessage(transactionId, value));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doStreamPub(json, this.onError));
	}

	@Override
	public <T> CompletableFuture<Optional<T>> get(final String key, final Class<T> type) {
		return this.get(key, (Type) type);
	}

	@Override
	public <T> CompletableFuture<Optional<T>> get(final String key, final Type type) {
		final var fut = new CompletableFuture<Optional<T>>();
		final var tid = this.acquireTid();
		String json;
		try {
			json = this.toJson(this.getMessage(tid, key));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doGet(tid, json, type, fut));
		return fut;
	}

	@Override
	public <T> CompletableFuture<Optional<List<T>>> getList(final String key, final Class<T> elementType) {
		final var fut = new CompletableFuture<Optional<List<T>>>();
		final var tid = this.acquireTid();
		String json;
		try {
			json = this.toJson(this.getMessage(tid, key));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doGet(tid, json,
				TypeFactory.defaultInstance().constructCollectionType(List.class, elementType), fut));
		return fut;
	}

	@Override
	public <T> CompletableFuture<List<TypedKeyValuePair<T>>> pGet(final String pattern, final Class<T> type) {
		return this.pGet(pattern, (Type) type);
	}

	@Override
	public <T> CompletableFuture<List<TypedKeyValuePair<T>>> pGet(final String pattern, final Type type) {
		final var fut = new CompletableFuture<List<TypedKeyValuePair<T>>>();
		final var tid = this.acquireTid();
		String json;
		try {
			json = this.toJson(this.pgetMessage(tid, pattern));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doPGet(tid, json, type, fut));
		return fut;
	}

	@Override
	public <T> CompletableFuture<Optional<T>> delete(final String key, final Class<T> type) {
		return this.delete(key, (Type) type);
	}

	@Override
	public <T> CompletableFuture<Optional<T>> delete(final String key, final Type type) {
		final var fut = new CompletableFuture<Optional<T>>();
		final var tid = this.acquireTid();
		String json;
		try {
			json = this.toJson(this.deleteMessage(tid, key));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doDelete(tid, json, type, fut));
		return fut;
	}

	@Override
	public void delete(final String key) {
		this.delete(key, null);
	}

	@Override
	public <T> CompletableFuture<List<TypedKeyValuePair<T>>> pDelete(final String pattern, final Class<T> type) {
		final var fut = new CompletableFuture<List<TypedKeyValuePair<T>>>();
		final var tid = this.acquireTid();
		String json;
		try {
			json = this.toJson(this.pDeleteMessage(tid, pattern, false));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doPDelete(tid, json, type, fut));
		return fut;
	}

	@Override
	public void pDelete(final String pattern) {
		this.pDelete(pattern, null);
	}

	@Override
	public CompletableFuture<List<String>> ls(final String parent) {
		final var fut = new CompletableFuture<List<String>>();
		final var tid = this.acquireTid();
		String json;
		try {
			json = this.toJson(this.lsMessage(tid, parent));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doLs(tid, json, fut));
		return fut;
	}

	@Override
	public CompletableFuture<List<String>> pLs(final String parentPattern) {
		final var fut = new CompletableFuture<List<String>>();
		final var tid = this.acquireTid();
		String json;
		try {
			json = this.toJson(this.pLsMessage(tid, parentPattern));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doPLs(tid, json, fut));
		return fut;
	}

	@Override
	public <T> long subscribe(final String key, final boolean unique, final boolean liveOnly, final Class<T> type,
			final Consumer<Optional<T>> callback) {
		return this.subscribe(key, unique, liveOnly, (Type) type, callback);
	}

	@Override
	public <T> long subscribe(final String key, final boolean unique, final boolean liveOnly, final Type type,
			final Consumer<Optional<T>> callback) {
		final var tid = this.acquireTid();
		String json;
		try {
			json = this.toJson(this.subscribeMessage(tid, key, unique, liveOnly));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doSubscribe(tid, json, type, callback, this.onError));
		return tid;
	}

	@Override
	public <T> long subscribeList(final String key, final boolean unique, final boolean liveOnly,
			final Class<T> elementType, final Consumer<Optional<List<T>>> callback) {
		final var tid = this.acquireTid();
		String json;
		try {
			json = this.toJson(this.subscribeMessage(tid, key, unique, liveOnly));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doSubscribe(tid, json,
				TypeFactory.defaultInstance().constructCollectionType(List.class, elementType), callback,
				this.onError));
		return tid;
	}

	@Override
	public <T> long pSubscribe(final String pattern, final boolean unique, final boolean liveOnly,
			final Optional<Long> aggregateEvents, final Class<T> type, final Consumer<TypedPStateEvent<T>> callback) {
		return this.pSubscribe(pattern, unique, liveOnly, aggregateEvents, (Type) type, callback);
	}

	@Override
	public <T> long pSubscribe(final String pattern, final boolean unique, final boolean liveOnly,
			final Optional<Long> aggregateEvents, final Type type, final Consumer<TypedPStateEvent<T>> callback) {
		final var tid = this.acquireTid();
		String json;
		try {
			json = this.toJson(this.pSubMessage(tid, pattern, unique, liveOnly, aggregateEvents));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.<T>doPSubscribe(tid, json, type, callback, this.onError));
		return tid;
	}

	@Override
	public void unsubscribe(final long transactionId) {
		String json;
		try {
			json = this.toJson(this.unsubscribeMessage(transactionId));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doUnsubscribe(transactionId, json, this.onError));
	}

	@Override
	public long subscribeLs(final String parent, final Consumer<List<String>> callback) {
		final var tid = this.acquireTid();
		String json;
		try {
			json = this.toJson(this.subscribeLsMessage(tid, parent));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doSubscribeLs(tid, json, callback, this.onError));
		return tid;
	}

	@Override
	public void unsubscribeLs(final long transactionId) {
		String json;
		try {
			json = this.toJson(this.unsubscribeLsMessage(transactionId));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doUnsubscribeLs(transactionId, json, this.onError));
	}

	@Override
	public <T> CompletableFuture<Tuple<Optional<T>, Long>> cGet(final String key, final Class<T> type) {
		return this.cGet(key, (Type) type);
	}

	@Override
	public <T> CompletableFuture<Tuple<Optional<T>, Long>> cGet(final String key, final Type type) {
		return this.doCGet(key, type);
	}

	private <T> CompletableFuture<Tuple<Optional<T>, Long>> doCGet(final String key, final Type type) {
		final var fut = new CompletableFuture<Tuple<Optional<T>, Long>>();
		final var tid = this.acquireTid();
		String json;
		try {
			json = this.toJson(this.cGetMessage(tid, key));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doCGet(tid, json, type, fut));
		return fut;
	}

	@Override
	public <T> CompletableFuture<Void> cSet(final String key, final T value, final long version) {
		final var fut = new CompletableFuture<Void>();
		final var tid = this.acquireTid();
		String json;
		try {
			json = this.toJson(this.cSetMessage(tid, key, value, version));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doCSet(tid, json, fut));
		return fut;
	}

	@Override
	public <T, V> void update(final String key, final Function<Optional<T>, V> transform, final Class<T> type) {
		this.update(key, transform, (Type) type);
	}

	@Override
	public <T, V> void update(final String key, final Function<Optional<T>, V> transform, final Type type) {
		this.tryUpdate(key, transform, type, 0);
	}

	@Override
	public <T> void update(final String key, final Supplier<T> seed, final Consumer<T> update, final Class<T> type) {
		this.update(key, seed, update, (Type) type);
	}

	@Override
	public <T> void update(final String key, final Supplier<T> seed, final Consumer<T> update, final Type type) {
		this.<T, T>tryUpdate(key, v -> {
			final var val = v.orElseGet(seed);
			update.accept(val);
			return val;
		}, type, 0);
	}

	@Override
	public <T> void updateList(final String key, final Consumer<List<T>> update, final Class<T> type) {
		this.<List<T>, List<T>>tryUpdate(key, arr -> {
			final var list = arr.isPresent() ? new ArrayList<T>(arr.get()) : new ArrayList<T>();
			update.accept(list);
			return list;
		}, TypeFactory.defaultInstance().constructCollectionType(List.class, type), 0);
	}

	private <T, V> void tryUpdate(final String key, final Function<Optional<T>, V> transform, final Type type,
			final int counter) {

		if (counter > 100) {
			WorterbuchClientImpl.log.error("Unsuccessfully tried to update {} 100 times, giving up.", key);
			return;
		}

		// TODO cache value so we don't have to get it from the server every time
		this.<T>doCGet(key, type).thenAccept(v -> {
			final var value = v.first();
			final var version = v.second();
			final var newValue = transform.apply(value);
			this.cSet(key, newValue, version).exceptionally(ex -> {
				this.tryUpdate(key, transform, type, counter + 1);
				return null;
			});
		});
	}

	@Override
	public CompletableFuture<Boolean> lock(final String key) {
		final var fut = new CompletableFuture<Boolean>();
		final var tid = this.acquireTid();
		String json;
		try {
			json = this.toJson(this.lockMessage(tid, key));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doLock(tid, json, fut));
		return fut;
	}

	@Override
	public CompletableFuture<Boolean> releaseLock(final String key) {
		final var fut = new CompletableFuture<Boolean>();
		final var tid = this.acquireTid();
		String json;
		try {
			json = this.toJson(this.releaseLockMessage(tid, key));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doReleaseLock(tid, json, fut));
		return fut;
	}

	@Override
	@SuppressFBWarnings(value = "EI_EXPOSE_REP")
	public ObjectMapper getObjectMapper() {
		return this.objectMapper;
	}

	@Override
	public String getClientId() {
		return this.clientId;
	}

	@Override
	public CompletableFuture<Optional<List<String>>> getGraveGoods() {
		return this.getList("$SYS/clients/" + this.getClientId() + "/graveGoods", String.class);
	}

	@Override
	public CompletableFuture<Optional<List<KeyValuePair>>> getLastWill() {
		return this.getList("$SYS/clients/" + this.getClientId() + "/lastWill", KeyValuePair.class);
	}

	@Override
	public void setGraveGoods(final List<String> graveGoods) throws WorterbuchException {

		try {
			this.cSet("$SYS/clients/" + this.getClientId() + "/graveGoods", graveGoods, 0).get(5, TimeUnit.SECONDS);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			this.onError.accept(e);
		} catch (final ExecutionException e) {
			if (e.getCause() instanceof final WorterbuchError err) {
				if (err.errorMessage.getErrorCode() == 0b00010010) {
					throw new WorterbuchException(
							"grave goods are already set, please use updateGraveGoods(…) instead to make sure nothing gets accidentally overwritten");
				}
			}
			this.onError.accept(e);
		} catch (final TimeoutException e) {
			this.onError.accept(e);
		}
	}

	@Override
	public void updateGraveGoods(final Consumer<List<String>> update) {
		this.updateList("$SYS/clients/" + this.getClientId() + "/graveGoods", update, String.class);
	}

	@Override
	public void setLastWill(final List<KeyValuePair> lastWill) throws WorterbuchException {
		try {
			this.cSet("$SYS/clients/" + this.getClientId() + "/lastWill", lastWill, 0).get(5, TimeUnit.SECONDS);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			this.onError.accept(e);
		} catch (final ExecutionException e) {
			if (e.getCause() instanceof final WorterbuchError err) {
				if (err.errorMessage.getErrorCode() == 0b00010010) {
					throw new WorterbuchException(
							"last will is already set, please use updateLastWill(…) instead to make sure nothing gets accidentally overwritten");
				}
			}
			this.onError.accept(e);
		} catch (final TimeoutException e) {
			this.onError.accept(e);
		}
	}

	@Override
	public void updateLastWill(final Consumer<List<KeyValuePair>> update) {
		this.updateList("$SYS/clients/" + this.getClientId() + "/lastWill", update, KeyValuePair.class);
	}

	@Override
	public void setClientName(final String name) {
		this.set("$SYS/clients/" + this.getClientId() + "/clientName", name);
	}

	private <T> ClientMessage setMessage(final long tid, final String key, final T value) {
		final var set = new Set(tid, key, value);
		final var msg = new ClientMessage();
		msg.setSet(set);
		return msg;
	}

	private <T> void doSet(final String jsonMessage, final Consumer<? super Throwable> onError) {
		this.sendMessage(jsonMessage, onError);
	}

	private <T> ClientMessage publishMessage(final long tid, final String key, final T value) {
		final var pub = new Publish(tid, key, value);
		final var msg = new ClientMessage();
		msg.setPublish(pub);
		return msg;
	}

	private <T> void doPublish(final String jsonMessage, final Consumer<? super Throwable> onError) {
		this.sendMessage(jsonMessage, onError);
	}

	private ClientMessage initPubStreamMessage(final long tid, final String key) {
		final var sPubInit = new SPubInit(tid, key);
		final var msg = new ClientMessage();
		msg.setsPubInit(sPubInit);
		return msg;
	}

	private <T> void doInitPubStream(final long tid, final String jsonMessage, final CompletableFuture<Long> fut) {
		this.pendingSPubInits.put(tid, new PendingSPubInit<>(fut));
		this.sendMessage(jsonMessage, fut::completeExceptionally);
	}

	private <T> ClientMessage streamPubMessage(final long tid, final T value) {
		final var sPub = new SPub(tid, value);
		final var msg = new ClientMessage();
		msg.setsPub(sPub);
		return msg;
	}

	private <T> void doStreamPub(final String jsonMessage, final Consumer<? super Throwable> onError) {
		this.sendMessage(jsonMessage, onError);
	}

	private ClientMessage getMessage(final long tid, final String key) {
		final var get = new Get(tid, key);
		final var msg = new ClientMessage();
		msg.setGet(get);
		return msg;
	}

	private ClientMessage cGetMessage(final long tid, final String key) {
		final var cGet = new CGet(tid, key);
		final var msg = new ClientMessage();
		msg.setcGet(cGet);
		return msg;
	}

	private <T> ClientMessage cSetMessage(final long tid, final String key, final T value, final Long version) {
		final var cSet = new CSet(tid, key, value, version);
		final var msg = new ClientMessage();
		msg.setcSet(cSet);
		return msg;
	}

	private <T> void doGet(final long tid, final String jsonMessage, final Type type,
			final CompletableFuture<Optional<T>> fut) {
		this.pendingGets.put(tid, new PendingGet<>(fut, type));
		this.sendMessage(jsonMessage, fut::completeExceptionally);
	}

	private <T> void doCGet(final long tid, final String jsonMessage, final Type type,
			final CompletableFuture<Tuple<Optional<T>, Long>> fut) {
		this.pendingCGets.put(tid, new PendingCGet<>(fut, type));
		this.sendMessage(jsonMessage, fut::completeExceptionally);
	}

	private <T> void doCSet(final long tid, final String jsonMessage, final CompletableFuture<Void> fut) {
		this.pendingCSets.put(tid, new PendingCSet(fut));
		this.sendMessage(jsonMessage, fut::completeExceptionally);
	}

	private ClientMessage pgetMessage(final long tid, final String pattern) {
		final var pget = new PGet(tid, pattern);
		final var msg = new ClientMessage();
		msg.setpGet(pget);
		return msg;
	}

	private <T> void doPGet(final long tid, final String jsonMessage, final Type type,
			final CompletableFuture<List<TypedKeyValuePair<T>>> fut) {
		this.pendingPGets.put(tid, new PendingPGet<>(fut, type));
		this.sendMessage(jsonMessage, fut::completeExceptionally);
	}

	private ClientMessage deleteMessage(final long tid, final String key) {
		final var del = new Delete(tid, key);
		final var msg = new ClientMessage();
		msg.setDelete(del);
		return msg;
	}

	private <T> void doDelete(final long tid, final String jsonMessage, final Type type,
			final CompletableFuture<Optional<T>> fut) {
		if (type != null) {
			this.pendingDeletes.put(tid, new PendingDelete<>(fut, Objects.requireNonNull(type)));
		}
		this.sendMessage(jsonMessage, fut::completeExceptionally);
	}

	private ClientMessage pDeleteMessage(final long tid, final String pattern, final boolean quiet) {
		final var pdel = new PDelete(tid, pattern, quiet);
		final var msg = new ClientMessage();
		msg.setpDelete(pdel);
		return msg;
	}

	private <T> void doPDelete(final long tid, final String jsonMessage, final Class<T> type,
			final CompletableFuture<List<TypedKeyValuePair<T>>> fut) {
		if (type != null) {
			this.pendingPDeletes.put(tid, new PendingPDelete<>(fut, Objects.requireNonNull(type)));
		}
		this.sendMessage(jsonMessage, fut::completeExceptionally);
	}

	private ClientMessage lsMessage(final long tid, final String parent) {
		final var ls = new Ls(tid, parent);
		final var msg = new ClientMessage();
		msg.setLs(ls);
		return msg;
	}

	private void doLs(final long tid, final String jsonMessage, final CompletableFuture<List<String>> fut) {
		this.pendingLsStates.put(tid, new PendingLsState(fut));
		this.sendMessage(jsonMessage, fut::completeExceptionally);
	}

	private ClientMessage pLsMessage(final long tid, final String parentPattern) {
		final var pLs = new PLs(tid, parentPattern);
		final var msg = new ClientMessage();
		msg.setpLs(pLs);
		return msg;
	}

	private void doPLs(final long tid, final String jsonMessage, final CompletableFuture<List<String>> fut) {
		this.pendingLsStates.put(tid, new PendingLsState(fut));
		this.sendMessage(jsonMessage, this.onError);
	}

	private ClientMessage subscribeMessage(final long tid, final String key, final boolean unique,
			final boolean liveOnly) {
		final var sub = new Subscribe(tid, key, unique, liveOnly);
		final var msg = new ClientMessage();
		msg.setSubscribe(sub);
		return msg;
	}

	private <T> void doSubscribe(final long tid, final String jsonMessage, final Type type,
			final Consumer<Optional<T>> callback, final Consumer<? super Throwable> onError) {
		this.subscriptions.put(tid, new Subscription<>(callback, type));
		this.sendMessage(jsonMessage, onError);
	}

	private ClientMessage pSubMessage(final long tid, final String pattern, final boolean unique,
			final boolean liveOnly, final Optional<Long> aggregateEvents) {
		final var psub = new PSubscribe(tid, pattern, unique, liveOnly, aggregateEvents.orElse(0L));
		final var msg = new ClientMessage();
		msg.setpSubscribe(psub);
		return msg;
	}

	private <T> void doPSubscribe(final long tid, final String jsonMessage, final Type type,
			final Consumer<TypedPStateEvent<T>> callback, final Consumer<? super Throwable> onError) {
		this.pSubscriptions.put(tid, new PSubscription<>(callback, type));
		this.sendMessage(jsonMessage, onError);
	}

	private ClientMessage unsubscribeMessage(final long transactionId) {
		final var unsub = new Unsubscribe(transactionId);
		final var msg = new ClientMessage();
		msg.setUnsubscribe(unsub);
		return msg;
	}

	private void doUnsubscribe(final long transactionId, final String jsonMessage,
			final Consumer<? super Throwable> onError) {
		this.subscriptions.remove(transactionId);
		this.pSubscriptions.remove(transactionId);
		this.sendMessage(jsonMessage, onError);
	}

	private ClientMessage subscribeLsMessage(final long tid, final String parent) {
		final var lssub = new SubscribeLs(tid, parent);
		final var msg = new ClientMessage();
		msg.setSubscribeLs(lssub);
		return msg;
	}

	private void doSubscribeLs(final long tid, final String jsonMessage, final Consumer<List<String>> callback,
			final Consumer<? super Throwable> onError) {
		this.lsSubscriptions.put(tid, new LsSubscription(callback));
		this.sendMessage(jsonMessage, onError);
	}

	private ClientMessage unsubscribeLsMessage(final long transactionId) {
		final var unsub = new UnsubscribeLs(transactionId);
		final var msg = new ClientMessage();
		msg.setUnsubscribeLs(unsub);
		return msg;
	}

	private void doUnsubscribeLs(final long transactionId, final String jsonMessage,
			final Consumer<? super Throwable> onError) {
		this.lsSubscriptions.remove(transactionId);
		this.sendMessage(jsonMessage, onError);
	}

	private ClientMessage lockMessage(final long tid, final String key) {
		final var lock = new Lock(tid, key);
		final var msg = new ClientMessage();
		msg.setLock(lock);
		return msg;
	}

	private void doLock(final long tid, final String jsonMessage, final CompletableFuture<Boolean> fut) {
		this.pendingLocks.put(tid, new PendingLock(fut));
		this.sendMessage(jsonMessage, this.onError);
	}

	private ClientMessage releaseLockMessage(final long tid, final String key) {
		final var releaseLock = new ReleaseLock(tid, key);
		final var msg = new ClientMessage();
		msg.setReleaseLock(releaseLock);
		return msg;
	}

	private void doReleaseLock(final long tid, final String jsonMessage, final CompletableFuture<Boolean> fut) {
		this.pendingReleaseLocks.put(tid, new PendingReleaseLock(fut));
		this.sendMessage(jsonMessage, this.onError);
	}

	private String toJson(final ClientMessage msg) throws JsonProcessingException {
		return msg != null ? this.objectMapper.writeValueAsString(msg) : "\"\"";
	}

	private void sendMessage(final String json, final Consumer<? super Throwable> onError) {

		try {
			this.client.sendString(json);
		} catch (final IOException e) {
			onError.accept(new WorterbuchException("Could not send message", e));
		} catch (final InterruptedException e) {
			onError.accept(new WorterbuchException("Interrupted while sending message", e));
		}
	}

	void messageReceived(final String message, final Executor callbackExecutor) {

		if (this.isKeepalive(message)) {
			return;
		}

		JsonNode parent;

		try {
			parent = this.objectMapper.readTree(message);

			final var ackContainer = parent.get("ack");
			if (ackContainer != null) {

				final var transactionId = ackContainer.get("transactionId").asLong();

				final var pendingSPubInit = this.pendingSPubInits.remove(transactionId);
				if (pendingSPubInit != null) {
					callbackExecutor.execute(() -> pendingSPubInit.callback().complete(transactionId));
				}

				final var pendingCSet = this.pendingCSets.remove(transactionId);
				if (pendingCSet != null) {
					callbackExecutor.execute(() -> pendingCSet.callback().complete(null));
				}

				final var pendingLock = this.pendingLocks.remove(transactionId);
				if (pendingLock != null) {
					callbackExecutor.execute(() -> pendingLock.callback().complete(true));
				}

				final var pendingReleaseLock = this.pendingReleaseLocks.remove(transactionId);
				if (pendingReleaseLock != null) {
					callbackExecutor.execute(() -> pendingReleaseLock.callback().complete(true));
				}

				return;
			}

			final var errContainer = parent.get("err");
			if (errContainer != null) {
				try {
					final var err = this.objectMapper.readValue(errContainer.toString(), Err.class);
					final var transactionId = err.getTransactionId();
					var handled = false;

					final var pendingGet = this.pendingGets.remove(transactionId);
					if (pendingGet != null) {
						handled = true;
						callbackExecutor.execute(() -> pendingGet.callback().complete(Optional.empty()));
					}

					final var pendingCGet = this.pendingCGets.remove(transactionId);
					if (pendingCGet != null) {
						handled = true;
						callbackExecutor
								.execute(() -> pendingCGet.callback().complete(new Tuple<>(Optional.empty(), 0L)));
					}

					final var pendingCSet = this.pendingCSets.remove(transactionId);
					if (pendingCSet != null) {
						handled = true;
						callbackExecutor
								.execute(() -> pendingCSet.callback().completeExceptionally(new WorterbuchError(err)));
					}

					final var pendingPGet = this.pendingPGets.remove(transactionId);
					if (pendingPGet != null) {
						handled = true;
						callbackExecutor
								.execute(() -> pendingPGet.callback().completeExceptionally(new WorterbuchError(err)));
					}

					final var pendingDelete = this.pendingDeletes.remove(transactionId);
					if (pendingDelete != null) {
						handled = true;
						callbackExecutor.execute(() -> pendingDelete.callback().complete(Optional.empty()));
					}

					final var pendingPDelete = this.pendingPDeletes.remove(transactionId);
					if (pendingPDelete != null) {
						handled = true;
						callbackExecutor.execute(
								() -> pendingPDelete.callback().completeExceptionally(new WorterbuchError(err)));
					}

					final var pendingLs = this.pendingLsStates.remove(transactionId);
					if (pendingLs != null) {
						handled = true;
						callbackExecutor
								.execute(() -> pendingLs.callback().completeExceptionally(new WorterbuchError(err)));
					}

					final var pendingSPubinit = this.pendingSPubInits.remove(transactionId);
					if (pendingSPubinit != null) {
						handled = true;
						callbackExecutor.execute(
								() -> pendingSPubinit.callback().completeExceptionally(new WorterbuchError(err)));
					}

					final var pendingLock = this.pendingLocks.remove(transactionId);
					if (pendingLock != null) {
						handled = true;
						callbackExecutor.execute(() -> pendingLock.callback().complete(false));
					}

					final var pendingReleaseLock = this.pendingReleaseLocks.remove(transactionId);
					if (pendingReleaseLock != null) {
						handled = true;
						callbackExecutor.execute(() -> pendingReleaseLock.callback().complete(false));
					}

					if (!handled) {
						WorterbuchClientImpl.log.error("Received error message from server: '{}'", err);
					}

				} catch (final JsonProcessingException e) {
					WorterbuchClientImpl.log.error("Could not deserialize JSON '{}': {}", errContainer, e.getMessage());
				}
				return;
			}

			final var wcContainer = parent.get("welcome");
			if (wcContainer != null) {
				try {
					final var welcome = this.objectMapper.readValue(wcContainer.toString(), Welcome.class);
					WorterbuchClientImpl.log.info("Received welcome message.");
					final var process = !this.welcomeReceived.getAndSet(true);
					if (process) {
						this.onWelcome.accept(welcome, this);
					}
				} catch (final JsonProcessingException e) {
					WorterbuchClientImpl.log.error("Could not deserialize JSON '{}': {}", wcContainer, e.getMessage());
					this.onError.accept(e);
				}
				return;
			}

			final var stateContainer = parent.get("state");
			if (stateContainer != null) {
				final var transactionId = stateContainer.get("transactionId").asLong();

				final var valueContainer = stateContainer.get("value");
				final var deletedContainer = stateContainer.get("deleted");

				final var typeFactory = this.objectMapper.getTypeFactory();

				final var pendingGet = this.pendingGets.remove(transactionId);
				this.deliverPendingGet(pendingGet, valueContainer, typeFactory, callbackExecutor);

				final var pendingDelete = this.pendingDeletes.remove(transactionId);
				this.deliverPendingDelete(pendingDelete, deletedContainer, typeFactory, callbackExecutor);

				final var subscription = this.subscriptions.get(transactionId);
				this.deliverSubscription(subscription, valueContainer, deletedContainer, typeFactory, callbackExecutor);

				return;
			}

			final var cStateContainer = parent.get("cState");
			if (cStateContainer != null) {
				final var transactionId = cStateContainer.get("transactionId").asLong();

				final var valueContainer = cStateContainer.get("value");
				final var versionContainer = cStateContainer.get("version");

				final var typeFactory = this.objectMapper.getTypeFactory();

				final var pendingCGet = this.pendingCGets.remove(transactionId);
				this.deliverPendingCGet(pendingCGet, valueContainer, versionContainer, typeFactory, callbackExecutor);

				return;
			}

			final var pStateContainer = parent.get("pState");
			if (pStateContainer != null) {
				final var transactionId = pStateContainer.get("transactionId").asLong();

				final var kvpsContainer = pStateContainer.get("keyValuePairs");
				final var deletedContainer = pStateContainer.get("deleted");

				final var typeFactory = this.objectMapper.getTypeFactory();

				final var pendingPGet = this.pendingPGets.remove(transactionId);
				this.deliverPendingPGet(pendingPGet, kvpsContainer, typeFactory, callbackExecutor);

				final var pendingPDelete = this.pendingPDeletes.remove(transactionId);
				this.deliverPendingPDelete(pendingPDelete, deletedContainer, typeFactory, callbackExecutor);

				final var pSubscription = this.pSubscriptions.get(transactionId);
				this.deliverPSubscription(pSubscription, kvpsContainer, deletedContainer, typeFactory,
						callbackExecutor);

				return;
			}

			final var lsContainer = parent.get("lsState");
			if (lsContainer != null) {
				final var transactionId = lsContainer.get("transactionId").asLong();

				final var pendingLs = this.pendingLsStates.remove(transactionId);
				final var lsSubscription = this.lsSubscriptions.get(transactionId);

				final var childrenContainer = lsContainer.get("children");

				try {
					final var children = this.objectMapper.readValue(childrenContainer.toString(),
							new TypeReference<List<String>>() {
							});

					if (pendingLs != null) {
						callbackExecutor.execute(() -> pendingLs.callback().complete(children));
					}

					if (lsSubscription != null) {
						callbackExecutor.execute(() -> lsSubscription.callback().accept(children));
					}
				} catch (final JsonProcessingException e) {

					WorterbuchClientImpl.log.error("Could not deserialize JSON '{}': {}", childrenContainer,
							e.getMessage());

					if (pendingLs != null) {
						callbackExecutor.execute(() -> pendingLs.callback().complete(Collections.emptyList()));
					}

					if (lsSubscription != null) {
						callbackExecutor.execute(() -> lsSubscription.callback().accept(Collections.emptyList()));
					}
				}

				return;
			}

		} catch (final JsonProcessingException e) {
			WorterbuchClientImpl.log.error("Received invalid JSON message: '{}'", message);
		}

	}

	void onDisconnect(final int statusCode, final String reason) {
		if (!this.closing.get()) {
			WorterbuchClientImpl.log.error("WebSocket connection to server closed.");
			this.pendingGets.clear();
			this.pendingPGets.clear();
			this.pendingDeletes.clear();
			this.pendingPDeletes.clear();
			this.pendingLsStates.clear();
			this.subscriptions.clear();
			this.pSubscriptions.clear();
			this.lsSubscriptions.clear();
			try {
				this.close();
			} catch (final Exception e) {
				this.onError.accept(e);
			}
		}
		if (!this.disconnected) {
			this.disconnected = true;
			this.onDisconnect.accept(statusCode, reason);
		}
	}

	private ClientMessage protoSwitchMessage(final int version) {
		final var req = new ProtocolSwitchRequest(version);
		final var msg = new ClientMessage();
		msg.setProtocolSwitchRequest(req);
		return msg;
	}

	private ClientMessage authMessage(final String token) {
		final var auth = new AuthorizationRequest(token);
		final var msg = new ClientMessage();
		msg.setAuthorizationRequest(auth);
		return msg;
	}

	void switchProtocol(final int protoVersion) {
		String jsonMessage;
		try {
			jsonMessage = this.toJson(this.protoSwitchMessage(protoVersion));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.sendMessage(jsonMessage, this.onError);
	}

	void authorize(final Optional<String> authToken) {
		authToken.ifPresent(token -> {
			String jsonMessage;
			try {
				jsonMessage = this.toJson(this.authMessage(token));
			} catch (final JsonProcessingException e) {
				throw new SerializationFailed(e);
			}
			this.sendMessage(jsonMessage, this.onError);
		});
	}

	void onError(final Throwable cause) {
		this.onError.accept(cause);
	}

	private boolean isKeepalive(final String message) {
		return message.isBlank();
	}

	private <T> void deliverPendingGet(final PendingGet<T> pendingGet, final JsonNode valueContainer,
			final TypeFactory typeFactory, final Executor callbackExecutor) {
		if (pendingGet != null) {
			if (valueContainer != null) {
				final var kvpType = typeFactory.constructType(pendingGet.type());
				try {
					final var value = this.objectMapper.<T>readValue(valueContainer.toString(), kvpType);
					callbackExecutor.execute(() -> pendingGet.callback().complete(Optional.ofNullable(value)));
				} catch (final JsonProcessingException e) {
					WorterbuchClientImpl.log.error("Could not deserialize JSON '{}': {}", valueContainer,
							e.getMessage());
					callbackExecutor.execute(() -> pendingGet.callback().completeExceptionally(e));
				}
			} else {
				callbackExecutor.execute(() -> pendingGet.callback().complete(Optional.empty()));
			}
		}
	}

	private <T> void deliverPendingCGet(final PendingCGet<T> pendingCGet, final JsonNode valueContainer,
			final JsonNode versionContainer, final TypeFactory typeFactory, final Executor callbackExecutor) {
		if ((pendingCGet != null) && (versionContainer != null)) {
			if (valueContainer != null) {
				final var kvpType = typeFactory.constructType(pendingCGet.type());
				try {
					final var value = this.objectMapper.<T>readValue(valueContainer.toString(), kvpType);
					final var version = this.objectMapper.readValue(versionContainer.toString(), Long.class);
					callbackExecutor.execute(
							() -> pendingCGet.callback().complete(new Tuple<>(Optional.ofNullable(value), version)));
				} catch (final JsonProcessingException e) {
					WorterbuchClientImpl.log.error("Could not deserialize JSON '{}': {}", valueContainer,
							e.getMessage());
					callbackExecutor.execute(() -> pendingCGet.callback().completeExceptionally(e));
				}
			} else {
				callbackExecutor.execute(() -> pendingCGet.callback().complete(new Tuple<>(Optional.empty(), 0L)));
			}
		}
	}

	private <T> void deliverPendingDelete(final PendingDelete<T> pendingDelete, final JsonNode deletedContainer,
			final TypeFactory typeFactory, final Executor callbackExecutor) {
		if (pendingDelete != null) {
			if (deletedContainer != null) {
				final var kvpType = typeFactory.constructType(pendingDelete.type());
				try {
					final var value = this.objectMapper.<T>readValue(deletedContainer.toString(), kvpType);
					callbackExecutor.execute(() -> pendingDelete.callback().complete(Optional.ofNullable(value)));
				} catch (final JsonProcessingException e) {
					WorterbuchClientImpl.log.error("Could not deserialize JSON '{}': {}", deletedContainer,
							e.getMessage());
					callbackExecutor.execute(() -> pendingDelete.callback().completeExceptionally(e));
				}
			} else {
				callbackExecutor.execute(() -> pendingDelete.callback().complete(Optional.empty()));
			}
		}
	}

	private <T> void deliverSubscription(final Subscription<T> subscription, final JsonNode valueContainer,
			final JsonNode deletedContainer, final TypeFactory typeFactory, final Executor callbackExecutor) {
		if (subscription != null) {
			final var kvpType = typeFactory.constructType(subscription.type());
			if (valueContainer != null) {
				try {
					final var value = this.objectMapper.<T>readValue(valueContainer.toString(), kvpType);
					callbackExecutor.execute(() -> subscription.callback().accept(Optional.ofNullable(value)));
				} catch (final JsonProcessingException e) {
					WorterbuchClientImpl.log.error("Could not deserialize JSON '{}': {}", valueContainer,
							e.getMessage());
					callbackExecutor.execute(() -> subscription.callback().accept(Optional.empty()));
				}
			} else {
				callbackExecutor.execute(() -> subscription.callback().accept(Optional.empty()));
			}
		}
	}

	private <T> void deliverPendingPGet(final PendingPGet<T> pendingPGet, final JsonNode kvpsContainer,
			final TypeFactory typeFactory, final Executor callbackExecutor) {
		if (pendingPGet != null) {
			if (kvpsContainer != null) {
				final var valueType = typeFactory.constructParametricType(TypedKeyValuePair.class,
						typeFactory.constructType(pendingPGet.type()));
				final var kvpsType = typeFactory.constructParametricType(List.class, valueType);
				try {
					final List<TypedKeyValuePair<T>> kvps = this.objectMapper.readValue(kvpsContainer.toString(),
							kvpsType);
					callbackExecutor.execute(() -> pendingPGet.callback().complete(kvps));
				} catch (final JsonProcessingException e) {
					WorterbuchClientImpl.log.error("Could not deserialize JSON '{}': {}", kvpsContainer,
							e.getMessage());
					callbackExecutor.execute(() -> pendingPGet.callback().completeExceptionally(e));
				}
			} else {
				callbackExecutor.execute(() -> pendingPGet.callback().complete(Collections.emptyList()));
			}
		}
	}

	private <T> void deliverPendingPDelete(final PendingPDelete<T> pendingPDelete, final JsonNode deletedContainer,
			final TypeFactory typeFactory, final Executor callbackExecutor) {
		if (pendingPDelete != null) {
			if (deletedContainer != null) {
				final var valueType = typeFactory.constructParametricType(TypedKeyValuePair.class,
						typeFactory.constructType(pendingPDelete.type()));
				final var kvpsType = typeFactory.constructParametricType(List.class, valueType);
				try {
					final List<TypedKeyValuePair<T>> kvps = this.objectMapper.readValue(deletedContainer.toString(),
							kvpsType);
					callbackExecutor.execute(() -> pendingPDelete.callback().complete(kvps));
				} catch (final JsonProcessingException e) {
					WorterbuchClientImpl.log.error("Could not deserialize JSON '{}': {}", deletedContainer,
							e.getMessage());
					callbackExecutor.execute(() -> pendingPDelete.callback().completeExceptionally(e));
				}
			} else {
				callbackExecutor.execute(() -> pendingPDelete.callback().complete(Collections.emptyList()));
			}
		}
	}

	private <T> void deliverPSubscription(final PSubscription<T> pSubscription, final JsonNode kvpsContainer,
			final JsonNode deletedContainer, final TypeFactory typeFactory, final Executor callbackExecutor) {
		if (pSubscription != null) {
			final var valueType = typeFactory.constructParametricType(TypedKeyValuePair.class,
					typeFactory.constructType(pSubscription.type()));
			final var kvpsType = typeFactory.constructParametricType(List.class, valueType);
			if (kvpsContainer != null) {
				try {
					final List<TypedKeyValuePair<T>> kvps = this.objectMapper.readValue(kvpsContainer.toString(),
							kvpsType);
					final var event = new TypedPStateEvent<>(kvps, null);
					callbackExecutor.execute(() -> pSubscription.callback().accept(event));
				} catch (final JsonProcessingException e) {
					WorterbuchClientImpl.log.error("Could not deserialize JSON '{}': {}", kvpsContainer,
							e.getMessage());
					callbackExecutor.execute(() -> pSubscription.callback().accept(new TypedPStateEvent<>(null, null)));
				}
			} else if (deletedContainer != null) {
				try {
					final List<TypedKeyValuePair<T>> kvps = this.objectMapper.readValue(deletedContainer.toString(),
							kvpsType);
					final var event = new TypedPStateEvent<>(null, kvps);
					callbackExecutor.execute(() -> pSubscription.callback().accept(event));
				} catch (final JsonProcessingException e) {
					WorterbuchClientImpl.log.error("Could not deserialize JSON '{}': {}", deletedContainer,
							e.getMessage());
					callbackExecutor.execute(() -> pSubscription.callback().accept(new TypedPStateEvent<>(null, null)));
				}
			}
		}
	}

	private void doClose() {
		final var closed = this.closing.getAndSet(true);
		if (!closed) {
			WorterbuchClientImpl.log.info("Closing worterbuch client.");
			try {
				this.client.close();
			} catch (final Exception e) {
				this.onError.accept(new WorterbuchException("Error closing worterbuch client", e));
			}
		}
	}

	private Optional<Integer> compatibleProtocolVersion(final List<List<Object>> supportedVersions) {
		final var major = Constants.PROTOCOL_VERSION.major();
		final var minor = Constants.PROTOCOL_VERSION.minor();

		for (final List<Object> version : supportedVersions) {
			if (version.size() < 2) {
				continue;
			}
			final var majS = version.get(0);
			final var minS = version.get(1);
			if (majS instanceof final Number majorServer) {
				if (minS instanceof final Number minorServer) {
					if ((majorServer.intValue() == major) && (minorServer.intValue() >= minor)) {
						return Optional.of(major);
					}
				}
			}
		}

		return Optional.empty();
	}
}
