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
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import net.bbmsoft.worterbuch.client.error.SerializationFailed;
import net.bbmsoft.worterbuch.client.impl.Config;
import net.bbmsoft.worterbuch.client.impl.Constants;
import net.bbmsoft.worterbuch.client.impl.TcpClientSocket;
import net.bbmsoft.worterbuch.client.impl.WrappingExecutor;
import net.bbmsoft.worterbuch.client.impl.WsClientSocket;
import net.bbmsoft.worterbuch.client.model.AuthorizationRequest;
import net.bbmsoft.worterbuch.client.model.ClientMessage;
import net.bbmsoft.worterbuch.client.model.Delete;
import net.bbmsoft.worterbuch.client.model.Err;
import net.bbmsoft.worterbuch.client.model.Get;
import net.bbmsoft.worterbuch.client.model.Ls;
import net.bbmsoft.worterbuch.client.model.PDelete;
import net.bbmsoft.worterbuch.client.model.PGet;
import net.bbmsoft.worterbuch.client.model.PLs;
import net.bbmsoft.worterbuch.client.model.PSubscribe;
import net.bbmsoft.worterbuch.client.model.Publish;
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
import net.bbmsoft.worterbuch.client.pending.PendingDelete;
import net.bbmsoft.worterbuch.client.pending.PendingGet;
import net.bbmsoft.worterbuch.client.pending.PendingLsState;
import net.bbmsoft.worterbuch.client.pending.PendingPDelete;
import net.bbmsoft.worterbuch.client.pending.PendingPGet;
import net.bbmsoft.worterbuch.client.pending.PendingSPubInit;
import net.bbmsoft.worterbuch.client.pending.Subscription;

public class WorterbuchClient implements AutoCloseable {

	private final static Logger log = LoggerFactory.getLogger(WorterbuchClient.class);

	public static WorterbuchClient connect(final Iterable<URI> uris, final BiConsumer<Integer, String> onDisconnect,
			final Consumer<Throwable> onError) throws InterruptedException, TimeoutException, WorterbuchException {

		final var callbackExecutor = new WrappingExecutor(
				Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "worterbuch-client-callbacks")), onError);

		return WorterbuchClient.connect(uris, Optional.empty(), callbackExecutor, onDisconnect, onError);
	}

	public static WorterbuchClient connect(final Iterable<URI> uris, final String authToken,
			final BiConsumer<Integer, String> onDisconnect, final Consumer<Throwable> onError)
			throws InterruptedException, TimeoutException, WorterbuchException {

		final var callbackExecutor = new WrappingExecutor(
				Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "worterbuch-client-callbacks")), onError);
		return WorterbuchClient.connect(uris, Optional.of(authToken), callbackExecutor, onDisconnect, onError);
	}

	public static WorterbuchClient connect(final Iterable<URI> uris, final Optional<String> authToken,
			final Executor callbackExecutor, final BiConsumer<Integer, String> onDisconnect,
			final Consumer<Throwable> onError) throws TimeoutException, WorterbuchException {

		Objects.requireNonNull(uris);
		Objects.requireNonNull(onDisconnect);
		Objects.requireNonNull(onError);

		final var exec = new WrappingExecutor(
				Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "worterbuch-client")), onError);

		WorterbuchClient wb = null;

		for (final URI uri : uris) {
			try {
				wb = WorterbuchClient.initWorterbuchClient(uri, authToken, onDisconnect, onError, exec,
						Objects.requireNonNull(callbackExecutor));
				break;
			} catch (final Throwable e) {
				WorterbuchClient.log.warn("Could not connect to server {}: {}", uri, e.getMessage());
			}
		}

		if (wb == null) {
			throw new WorterbuchException("Could not connect to any server.");
		}

		return wb;
	}

	private static WorterbuchClient initWorterbuchClient(final URI uri, final Optional<String> authtoken,
			final BiConsumer<Integer, String> onDisconnect, final Consumer<? super Throwable> onError,
			final ScheduledExecutorService exec, final Executor callbackExecutor) throws Throwable {

		if (uri.getScheme().equals("tcp")) {
			return WorterbuchClient.initTcpWorterbuchClient(uri, authtoken, onDisconnect, onError, exec,
					callbackExecutor);
		} else {
			return WorterbuchClient.initWsWorterbuchClient(uri, authtoken, onDisconnect, onError, exec,
					callbackExecutor);
		}

	}

	private static WorterbuchClient initWsWorterbuchClient(final URI uri, final Optional<String> authtoken,
			final BiConsumer<Integer, String> onDisconnect, final Consumer<? super Throwable> onError,
			final ScheduledExecutorService exec, final Executor callbackExecutor) throws Throwable {

		final var latch = new CountDownLatch(1);

		final BiConsumer<Welcome, WorterbuchClient> onWelcome = (welcome, client) -> {

			final var serverInfo = welcome.getInfo();

			if (!client.protocolVersionIsSupported(serverInfo.getProtocolVersion())) {
				client.onError.accept(new WorterbuchException(
						"Protocol version " + serverInfo.getProtocolVersion() + " is not supported by this client."));
			}

			client.clientId = welcome.getClientId();

			if (serverInfo.isAuthorizationRequired()) {
				client.authorize(authtoken);
			}

			latch.countDown();

		};

		final var clientSocket = new WsClientSocket(uri, onError, authtoken);

		final var wb = new WorterbuchClient(exec, onWelcome, onDisconnect, onError, clientSocket);

		final var error = new LinkedBlockingQueue<Optional<Throwable>>();
		final Consumer<Throwable> preConnectErrorHandler = e -> {
			try {
				error.offer(Optional.of(e), Config.CONNECT_TIMEOUT, TimeUnit.SECONDS);
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
					error.offer(Optional.empty(), Config.CONNECT_TIMEOUT, TimeUnit.SECONDS);
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

		final var err = error.poll(Config.CONNECT_TIMEOUT, TimeUnit.SECONDS);
		if (err.isPresent()) {
			throw err.get();
		}

		if (!latch.await(Config.CONNECT_TIMEOUT, TimeUnit.SECONDS)) {
			throw new WorterbuchException("did not receive welcome message");
		}

		return wb;
	}

	private static WorterbuchClient initTcpWorterbuchClient(final URI uri, final Optional<String> authtoken,
			final BiConsumer<Integer, String> onDisconnect, final Consumer<? super Throwable> onError,
			final ScheduledExecutorService exec, final Executor callbackExecutor) throws Throwable {

		final var latch = new CountDownLatch(1);

		final BiConsumer<Welcome, WorterbuchClient> onWelcome = (welcome, client) -> {

			final var serverInfo = welcome.getInfo();

			if (!client.protocolVersionIsSupported(serverInfo.getProtocolVersion())) {
				client.onError.accept(new WorterbuchException(
						"Protocol version " + serverInfo.getProtocolVersion() + " is not supported by this client."));
			}

			client.clientId = welcome.getClientId();

			if (serverInfo.isAuthorizationRequired()) {
				client.authorize(authtoken);
			}

			latch.countDown();

		};

		final var clientSocket = new TcpClientSocket(uri, onDisconnect, onError, Config.CHANNEL_BUFFER_SIZE);

		final var wb = new WorterbuchClient(exec, onWelcome, onDisconnect, onError, clientSocket);

		clientSocket.open(msg -> wb.messageReceived(msg, callbackExecutor), Config.SEND_TIMEOUT, TimeUnit.SECONDS);

		if (!latch.await(Config.CONNECT_TIMEOUT, TimeUnit.SECONDS)) {
			throw new WorterbuchException("did not receive welcome message");
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
	private final Map<Long, PendingPGet<?>> pendingPGets = new ConcurrentHashMap<>();
	private final Map<Long, PendingSPubInit<?>> pendingSPubInits = new ConcurrentHashMap<>();
	private final Map<Long, PendingDelete<?>> pendingDeletes = new ConcurrentHashMap<>();
	private final Map<Long, PendingPDelete<?>> pendingPDeletes = new ConcurrentHashMap<>();
	private final Map<Long, PendingLsState> pendingLsStates = new ConcurrentHashMap<>();
	private final Map<Long, Subscription<?>> subscriptions = new ConcurrentHashMap<>();
	private final Map<Long, PSubscription<?>> pSubscriptions = new ConcurrentHashMap<>();
	private final Map<Long, LsSubscription> lsSubscriptions = new ConcurrentHashMap<>();

	private boolean closing;
	private boolean disconnected;
	private BiConsumer<Welcome, WorterbuchClient> onWelcome;

	private String clientId;

	WorterbuchClient(final ScheduledExecutorService exec, final BiConsumer<Welcome, WorterbuchClient> onWelcome,
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

	public <T> long set(final String key, final T value, final Consumer<? super Throwable> onError) {
		final var tid = this.acquireTid();
		String json;
		try {
			json = this.toJson(this.setMessage(tid, key, value));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doSet(json, onError));
		return tid;
	}

	public <T> long publish(final String key, final T value, final Consumer<? super Throwable> onError) {
		final var tid = this.acquireTid();
		String json;
		try {
			json = this.toJson(this.publishMessage(tid, key, value));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doPublish(json, onError));
		return tid;
	}

	public <T> Future<Long> initPubStream(final String key, final Consumer<? super Throwable> onError) {
		final var fut = new CompletableFuture<Long>();
		final var tid = this.acquireTid();
		this.exec.execute(() -> this.doInitPubStream(tid, key, res -> {
			if (res) {
				fut.complete(tid);
			} else {
				fut.cancel(false);
			}
		}, onError));
		return fut;
	}

	public <T> long initPubStream(final String key, final Consumer<Long> callback,
			final Consumer<? super Throwable> onError) {
		final var tid = this.acquireTid();
		String json;
		try {
			json = this.toJson(this.initPubStreamMessage(tid, key));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doInitPubStream(tid, json, res -> {
			if (res) {
				callback.accept(tid);
			} else {
				callback.accept(-1L);
			}
		}, onError));
		return tid;
	}

	public <T> void streamPub(final long transactionId, final T value, final Consumer<? super Throwable> onError) {
		String json;
		try {
			json = this.toJson(this.streamPubMessage(transactionId, value));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doStreamPub(json, onError));
	}

	public <T> Future<Optional<T>> get(final String key, final Class<T> type) {
		final var fut = new CompletableFuture<Optional<T>>();
		try {
			this.<T>get(key, type, val -> fut.complete(val), e -> fut.completeExceptionally(e));
		} catch (final SerializationFailed e) {
			throw new SerializationFailed(e);
		}
		return fut;
	}

	public <T> long get(final String key, final Class<T> type, final Consumer<Optional<T>> callback,
			final Consumer<? super Throwable> onError) {
		final var tid = this.acquireTid();
		String json;
		try {
			json = this.toJson(this.getMessage(tid, key));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doGet(tid, json, type, callback, onError));
		return tid;
	}

	public <T> Future<Optional<T[]>> getArray(final String key, final Class<T> elementType) {
		final var fut = new CompletableFuture<Optional<T[]>>();
		try {
			this.<T>getArray(key, elementType, val -> fut.complete(val), e -> fut.completeExceptionally(e));
		} catch (final SerializationFailed e) {
			throw new SerializationFailed(e);
		}
		return fut;
	}

	public <T> long getArray(final String key, final Class<T> elementType, final Consumer<Optional<T[]>> callback,
			final Consumer<? super Throwable> onError) {
		final var tid = this.acquireTid();
		String json;
		try {
			json = this.toJson(this.getMessage(tid, key));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doGet(tid, json, (GenericArrayType) () -> elementType, callback, onError));
		return tid;
	}

	public <T> Future<List<KeyValuePair<T>>> pGet(final String pattern, final Class<T> type) {
		final var fut = new CompletableFuture<List<KeyValuePair<T>>>();
		try {
			this.<T>pGet(pattern, type, val -> fut.complete(val), e -> fut.completeExceptionally(e));
		} catch (final SerializationFailed e) {
			throw new SerializationFailed(e);
		}
		return fut;
	}

	public <T> long pGet(final String pattern, final Class<T> type, final Consumer<List<KeyValuePair<T>>> callback,
			final Consumer<? super Throwable> onError) {
		final var tid = this.acquireTid();
		String json;
		try {
			json = this.toJson(this.pgetMessage(tid, pattern));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doPGet(tid, json, type, callback, onError));
		return tid;
	}

	public <T> Future<Optional<T>> delete(final String key, final Class<T> type) {
		final var fut = new CompletableFuture<Optional<T>>();
		try {
			this.<T>delete(key, type, val -> fut.complete(val), e -> fut.completeExceptionally(e));
		} catch (final SerializationFailed e) {
			throw new SerializationFailed(e);
		}
		return fut;
	}

	public <T> long delete(final String key, final Class<T> type, final Consumer<Optional<T>> callback,
			final Consumer<? super Throwable> onError) {
		final var tid = this.acquireTid();
		String json;
		try {
			json = this.toJson(this.deleteMessage(tid, key));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doDelete(tid, json, type, callback, onError));
		return tid;
	}

	public long delete(final String key, final Consumer<? super Throwable> onError) {
		return this.delete(key, null, null, onError);
	}

	public <T> Future<List<KeyValuePair<T>>> pDelete(final String pattern, final Class<T> type) {
		final var fut = new CompletableFuture<List<KeyValuePair<T>>>();
		try {
			this.<T>pDelete(pattern, type, val -> fut.complete(val), e -> fut.completeExceptionally(e));
		} catch (final SerializationFailed e) {
			throw new SerializationFailed(e);
		}
		return fut;
	}

	public Future<Void> pDelete(final String pattern) {
		final var fut = new CompletableFuture<Void>();
		try {
			this.pDelete(pattern, () -> fut.complete(null), e -> fut.completeExceptionally(e));
		} catch (final SerializationFailed e) {
			throw new SerializationFailed(e);
		}
		return fut;
	}

	public <T> long pDelete(final String pattern, final Class<T> type, final Consumer<List<KeyValuePair<T>>> callback,
			final Consumer<? super Throwable> onError) {
		final var tid = this.acquireTid();
		String json;
		try {
			json = this.toJson(this.pDeleteMessage(tid, pattern, false));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doPDelete(tid, json, type, callback, onError));
		return tid;
	}

	public long pDelete(final String pattern, final Runnable callback, final Consumer<? super Throwable> onError) {
		final var tid = this.acquireTid();
		String json;
		try {
			json = this.toJson(this.pDeleteMessage(tid, pattern, true));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doPDelete(tid, json, Void.class, val -> callback.run(), onError));
		return tid;
	}

	public Future<List<String>> ls(final String parent) {
		final var fut = new CompletableFuture<List<String>>();
		try {
			this.ls(parent, val -> fut.complete(val), e -> fut.completeExceptionally(e));
		} catch (final SerializationFailed e) {
			throw new SerializationFailed(e);
		}
		return fut;
	}

	public long ls(final String parent, final Consumer<List<String>> callback,
			final Consumer<? super Throwable> onError) {
		final var tid = this.acquireTid();
		String json;
		try {
			json = this.toJson(this.lsMessage(tid, parent));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doLs(tid, json, callback, onError));
		return tid;
	}

	public Future<List<String>> pLs(final String parentPattern) {
		final var fut = new CompletableFuture<List<String>>();
		try {
			this.pLs(parentPattern, val -> fut.complete(val), e -> fut.completeExceptionally(e));
		} catch (final SerializationFailed e) {
			throw new SerializationFailed(e);
		}
		return fut;
	}

	public long pLs(final String parentPattern, final Consumer<List<String>> callback,
			final Consumer<? super Throwable> onError) {
		final var tid = this.acquireTid();
		String json;
		try {
			json = this.toJson(this.pLsMessage(tid, parentPattern));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doPLs(tid, json, callback, onError));
		return tid;
	}

	public <T> long subscribe(final String key, final boolean unique, final boolean liveOnly, final Class<T> type,
			final Consumer<Optional<T>> callback, final Consumer<? super Throwable> onError) {
		final var tid = this.acquireTid();
		String json;
		try {
			json = this.toJson(this.subscribeMessage(tid, key, unique, liveOnly));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doSubscribe(tid, json, type, callback, onError));
		return tid;
	}

	public <T> long subscribeArray(final String key, final boolean unique, final boolean liveOnly,
			final Class<T> elementType, final Consumer<Optional<T[]>> callback,
			final Consumer<? super Throwable> onError) {
		final var tid = this.acquireTid();
		String json;
		try {
			json = this.toJson(this.subscribeMessage(tid, key, unique, liveOnly));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doSubscribe(tid, json, (GenericArrayType) () -> elementType, callback, onError));
		return tid;
	}

	public <T> long pSubscribe(final String pattern, final boolean unique, final boolean liveOnly,
			final Optional<Long> aggregateEvents, final Class<T> type, final Consumer<PStateEvent<T>> callback,
			final Consumer<? super Throwable> onError) {
		final var tid = this.acquireTid();
		String json;
		try {
			json = this.toJson(this.pSubMessage(tid, pattern, unique, liveOnly, aggregateEvents));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doPSubscribe(tid, json, type, callback, onError));
		return tid;
	}

	public void unsubscribe(final long transactionId, final Consumer<? super Throwable> onError) {
		String json;
		try {
			json = this.toJson(this.unsubscribeMessage(transactionId));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doUnsubscribe(transactionId, json, onError));
	}

	public long subscribeLs(final String parent, final Consumer<List<String>> callback,
			final Consumer<? super Throwable> onError) {
		final var tid = this.acquireTid();
		String json;
		try {
			json = this.toJson(this.subscribeLsMessage(tid, parent));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doSubscribeLs(tid, json, callback, onError));
		return tid;
	}

	public void unsubscribeLs(final long transactionId, final Consumer<? super Throwable> onError) {
		String json;
		try {
			json = this.toJson(this.unsubscribeLsMessage(transactionId));
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
		this.exec.execute(() -> this.doUnsubscribeLs(transactionId, json, onError));
	}

	public ObjectMapper getObjectMapper() {
		return this.objectMapper;
	}

	public String getClientId() {
		return this.clientId;
	}

	public Future<Optional<String[]>> getGraveGoods() {
		return this.getArray("$SYS/clients/" + this.getClientId() + "/graveGoods", String.class);
	}

	@SuppressWarnings("rawtypes")
	public Future<Optional<KeyValuePair[]>> getLastWill() {
		return this.getArray("$SYS/clients/" + this.getClientId() + "/lastWill", KeyValuePair.class);
	}

	public long setGraveGoods(final String[] graveGoods, final Consumer<? super Throwable> onError) {
		if (graveGoods == null) {
			return this.delete("$SYS/clients/" + this.getClientId() + "/graveGoods", onError);
		} else {
			return this.set("$SYS/clients/" + this.getClientId() + "/graveGoods", graveGoods, onError);
		}
	}

	public long setLastWill(final KeyValuePair<?>[] lastWill, final Consumer<? super Throwable> onError) {
		if (lastWill == null) {
			return this.delete("$SYS/clients/" + this.getClientId() + "/lastWill", onError);
		} else {
			return this.set("$SYS/clients/" + this.getClientId() + "/lastWill", lastWill, onError);
		}
	}

	public long setClientName(final String name) {
		return this.set("$SYS/clients/" + this.getClientId() + "/clientName", name, this.onError);
	}

	private <T> ClientMessage setMessage(final long tid, final String key, final T value) {
		final var set = new Set();
		set.setTransactionId(tid);
		set.setKey(key);
		set.setValue(value);
		final var msg = new ClientMessage();
		msg.setSet(set);
		return msg;
	}

	private <T> void doSet(final String jsonMessage, final Consumer<? super Throwable> onError) {
		this.sendMessage(jsonMessage, onError);
	}

	private <T> ClientMessage publishMessage(final long tid, final String key, final T value) {
		final var pub = new Publish();
		pub.setTransactionId(tid);
		pub.setKey(key);
		pub.setValue(value);
		final var msg = new ClientMessage();
		msg.setPublish(pub);
		return msg;
	}

	private <T> void doPublish(final String jsonMessage, final Consumer<? super Throwable> onError) {
		this.sendMessage(jsonMessage, onError);
	}

	private ClientMessage initPubStreamMessage(final long tid, final String key) {
		final var sPubInit = new SPubInit();
		sPubInit.setTransactionId(tid);
		sPubInit.setKey(key);
		final var msg = new ClientMessage();
		msg.setsPubInit(sPubInit);
		return msg;
	}

	private <T> void doInitPubStream(final long tid, final String jsonMessage, final Consumer<Boolean> callback,
			final Consumer<? super Throwable> onError) {
		this.pendingSPubInits.put(tid, new PendingSPubInit<>(callback));
		this.sendMessage(jsonMessage, onError);
	}

	private <T> ClientMessage streamPubMessage(final long tid, final T value) {
		final var sPub = new SPub();
		sPub.setTransactionId(tid);
		sPub.setValue(value);
		final var msg = new ClientMessage();
		msg.setsPub(sPub);
		return msg;
	}

	private <T> void doStreamPub(final String jsonMessage, final Consumer<? super Throwable> onError) {
		this.sendMessage(jsonMessage, onError);
	}

	private ClientMessage getMessage(final long tid, final String key) {
		final var get = new Get();
		get.setTransactionId(tid);
		get.setKey(key);
		final var msg = new ClientMessage();
		msg.setGet(get);
		return msg;
	}

	private <T> void doGet(final long tid, final String jsonMessage, final Type type,
			final Consumer<Optional<T>> callback, final Consumer<? super Throwable> onError) {
		this.pendingGets.put(tid, new PendingGet<>(callback, type));
		this.sendMessage(jsonMessage, onError);
	}

	private ClientMessage pgetMessage(final long tid, final String pattern) {
		final var pget = new PGet();
		pget.setTransactionId(tid);
		pget.setRequestPattern(pattern);
		final var msg = new ClientMessage();
		msg.setpGet(pget);
		return msg;
	}

	private <T> void doPGet(final long tid, final String jsonMessage, final Class<T> type,
			final Consumer<List<KeyValuePair<T>>> callback, final Consumer<? super Throwable> onError) {
		this.pendingPGets.put(tid, new PendingPGet<>(callback, type));
		this.sendMessage(jsonMessage, onError);
	}

	private ClientMessage deleteMessage(final long tid, final String key) {
		final var del = new Delete();
		del.setTransactionId(tid);
		del.setKey(key);
		final var msg = new ClientMessage();
		msg.setDelete(del);
		return msg;
	}

	private <T> void doDelete(final long tid, final String jsonMessage, final Class<T> type,
			final Consumer<Optional<T>> callback, final Consumer<? super Throwable> onError) {
		if (callback != null) {
			this.pendingDeletes.put(tid, new PendingDelete<>(callback, Objects.requireNonNull(type)));
		}
		this.sendMessage(jsonMessage, onError);
	}

	private ClientMessage pDeleteMessage(final long tid, final String pattern, final boolean quiet) {
		final var pdel = new PDelete();
		pdel.setTransactionId(tid);
		pdel.setRequestPattern(pattern);
		pdel.setQuiet(quiet);
		final var msg = new ClientMessage();
		msg.setpDelete(pdel);
		return msg;
	}

	private <T> void doPDelete(final long tid, final String jsonMessage, final Class<T> type,
			final Consumer<List<KeyValuePair<T>>> callback, final Consumer<? super Throwable> onError) {
		if (callback != null) {
			this.pendingPDeletes.put(tid, new PendingPDelete<>(callback, Objects.requireNonNull(type)));
		}
		this.sendMessage(jsonMessage, onError);
	}

	private ClientMessage lsMessage(final long tid, final String parent) {
		final var ls = new Ls();
		ls.setTransactionId(tid);
		ls.setParent(parent);
		final var msg = new ClientMessage();
		msg.setLs(ls);
		return msg;
	}

	private void doLs(final long tid, final String jsonMessage, final Consumer<List<String>> callback,
			final Consumer<? super Throwable> onError) {
		this.pendingLsStates.put(tid, new PendingLsState(callback));
		this.sendMessage(jsonMessage, onError);
	}

	private ClientMessage pLsMessage(final long tid, final String parentPattern) {
		final var pLs = new PLs();
		pLs.setTransactionId(tid);
		pLs.setParentPattern(parentPattern);
		final var msg = new ClientMessage();
		msg.setpLs(pLs);
		return msg;
	}

	private void doPLs(final long tid, final String jsonMessage, final Consumer<List<String>> callback,
			final Consumer<? super Throwable> onError) {
		this.pendingLsStates.put(tid, new PendingLsState(callback));
		this.sendMessage(jsonMessage, onError);
	}

	private ClientMessage subscribeMessage(final long tid, final String key, final boolean unique,
			final boolean liveOnly) {
		final var sub = new Subscribe();
		sub.setTransactionId(tid);
		sub.setKey(key);
		sub.setUnique(unique);
		sub.setLiveOnly(liveOnly);
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
		final var psub = new PSubscribe();
		psub.setTransactionId(tid);
		psub.setRequestPattern(pattern);
		psub.setUnique(unique);
		psub.setLiveOnly(liveOnly);
		aggregateEvents.ifPresent(psub::setAggregateEvents);
		final var msg = new ClientMessage();
		msg.setpSubscribe(psub);
		return msg;
	}

	private <T> void doPSubscribe(final long tid, final String jsonMessage, final Class<T> type,
			final Consumer<PStateEvent<T>> callback, final Consumer<? super Throwable> onError) {
		this.pSubscriptions.put(tid, new PSubscription<>(callback, type));
		this.sendMessage(jsonMessage, onError);
	}

	private ClientMessage unsubscribeMessage(final long transactionId) {
		final var unsub = new Unsubscribe();
		unsub.setTransactionId(transactionId);
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
		final var lssub = new SubscribeLs();
		lssub.setTransactionId(tid);
		lssub.setParent(parent);
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
		final var unsub = new UnsubscribeLs();
		unsub.setTransactionId(transactionId);
		final var msg = new ClientMessage();
		msg.setUnsubscribeLs(unsub);
		return msg;
	}

	private void doUnsubscribeLs(final long transactionId, final String jsonMessage,
			final Consumer<? super Throwable> onError) {
		this.lsSubscriptions.remove(transactionId);
		this.sendMessage(jsonMessage, onError);
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
					callbackExecutor.execute(() -> pendingSPubInit.callback.accept(false));
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
						callbackExecutor.execute(() -> pendingGet.callback.accept(Optional.empty()));
					}

					final var pendingPGet = this.pendingPGets.remove(transactionId);
					if (pendingPGet != null) {
						handled = true;
						callbackExecutor.execute(() -> pendingPGet.callback.accept(Collections.emptyList()));
					}

					final var pendingDelete = this.pendingDeletes.remove(transactionId);
					if (pendingDelete != null) {
						handled = true;
						callbackExecutor.execute(() -> pendingDelete.callback.accept(Optional.empty()));
					}

					final var pendingPDelete = this.pendingPDeletes.remove(transactionId);
					if (pendingPDelete != null) {
						handled = true;
						callbackExecutor.execute(() -> pendingPDelete.callback.accept(Collections.emptyList()));
					}

					final var pendingLs = this.pendingLsStates.remove(transactionId);
					if (pendingLs != null) {
						handled = true;
						callbackExecutor.execute(() -> pendingLs.callback.accept(Collections.emptyList()));
					}

					final var pendingSPubinit = this.pendingSPubInits.remove(transactionId);
					if (pendingSPubinit != null) {
						handled = true;
						callbackExecutor.execute(() -> pendingSPubinit.callback.accept(false));
					}

					if (!handled) {
						WorterbuchClient.log.error("Received error message from server: '{}'", errContainer);
					}

				} catch (final JsonProcessingException e) {
					WorterbuchClient.log.error("Could not deserialize JSON '{}': {}", errContainer, e.getMessage());
				}
				return;
			}

			final var wcContainer = parent.get("welcome");
			if (wcContainer != null) {
				try {
					final var welcome = this.objectMapper.readValue(wcContainer.toString(), Welcome.class);
					WorterbuchClient.log.info("Received welcome message.");
					if (this.onWelcome != null) {
						this.onWelcome.accept(welcome, this);
						this.onWelcome = null;
					}
				} catch (final JsonProcessingException e) {
					WorterbuchClient.log.error("Could not deserialize JSON '{}': {}", wcContainer, e.getMessage());
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
						callbackExecutor.execute(() -> pendingLs.callback.accept(children));
					}

					if (lsSubscription != null) {
						callbackExecutor.execute(() -> lsSubscription.callback.accept(children));
					}
				} catch (final JsonProcessingException e) {

					WorterbuchClient.log.error("Could not deserialize JSON '{}': {}", childrenContainer,
							e.getMessage());

					if (pendingLs != null) {
						callbackExecutor.execute(() -> pendingLs.callback.accept(Collections.emptyList()));
					}

					if (lsSubscription != null) {
						callbackExecutor.execute(() -> lsSubscription.callback.accept(Collections.emptyList()));
					}
				}

				return;
			}

		} catch (final JsonProcessingException e) {
			WorterbuchClient.log.error("Received invalid JSON message: '{}'", message);
		}

	}

	void onDisconnect(final int statusCode, final String reason) {
		if (!this.closing) {
			WorterbuchClient.log.error("WebSocket connection to server closed.");
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

	private ClientMessage authMessage(final String token) {
		final var auth = new AuthorizationRequest(token);
		final var msg = new ClientMessage();
		msg.setAuthorizationRequest(auth);
		return msg;
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
				final var kvpType = typeFactory.constructType(pendingGet.type);
				try {
					final var value = this.objectMapper.<T>readValue(valueContainer.toString(), kvpType);
					callbackExecutor.execute(() -> pendingGet.callback.accept(Optional.ofNullable(value)));
				} catch (final JsonProcessingException e) {
					WorterbuchClient.log.error("Could not deserialize JSON '{}': {}", valueContainer, e.getMessage());
					callbackExecutor.execute(() -> pendingGet.callback.accept(Optional.empty()));
				}
			} else {
				callbackExecutor.execute(() -> pendingGet.callback.accept(Optional.empty()));
			}
		}
	}

	private <T> void deliverPendingDelete(final PendingDelete<T> pendingDelete, final JsonNode deletedContainer,
			final TypeFactory typeFactory, final Executor callbackExecutor) {
		if (pendingDelete != null) {
			if (deletedContainer != null) {
				final var kvpType = typeFactory.constructType(pendingDelete.type);
				try {
					final var value = this.objectMapper.<T>readValue(deletedContainer.toString(), kvpType);
					callbackExecutor.execute(() -> pendingDelete.callback.accept(Optional.ofNullable(value)));
				} catch (final JsonProcessingException e) {
					WorterbuchClient.log.error("Could not deserialize JSON '{}': {}", deletedContainer, e.getMessage());
					callbackExecutor.execute(() -> pendingDelete.callback.accept(Optional.empty()));
				}
			} else {
				callbackExecutor.execute(() -> pendingDelete.callback.accept(Optional.empty()));
			}
		}
	}

	private <T> void deliverSubscription(final Subscription<T> subscription, final JsonNode valueContainer,
			final JsonNode deletedContainer, final TypeFactory typeFactory, final Executor callbackExecutor) {
		if (subscription != null) {
			final var kvpType = typeFactory.constructType(subscription.type);
			if (valueContainer != null) {
				try {
					final var value = this.objectMapper.<T>readValue(valueContainer.toString(), kvpType);
					callbackExecutor.execute(() -> subscription.callback.accept(Optional.ofNullable(value)));
				} catch (final JsonProcessingException e) {
					WorterbuchClient.log.error("Could not deserialize JSON '{}': {}", valueContainer, e.getMessage());
					callbackExecutor.execute(() -> subscription.callback.accept(Optional.empty()));
				}
			} else {
				callbackExecutor.execute(() -> subscription.callback.accept(Optional.empty()));
			}
		}
	}

	private <T> void deliverPendingPGet(final PendingPGet<T> pendingPGet, final JsonNode kvpsContainer,
			final TypeFactory typeFactory, final Executor callbackExecutor) {
		if (pendingPGet != null) {
			if (kvpsContainer != null) {
				final var kvpsType = typeFactory.constructParametricType(List.class,
						typeFactory.constructParametricType(KeyValuePair.class, pendingPGet.type));
				try {
					final List<KeyValuePair<T>> kvps = this.objectMapper.readValue(kvpsContainer.toString(), kvpsType);
					callbackExecutor.execute(() -> pendingPGet.callback.accept(kvps));
				} catch (final JsonProcessingException e) {
					WorterbuchClient.log.error("Could not deserialize JSON '{}': {}", kvpsContainer, e.getMessage());
					callbackExecutor.execute(() -> pendingPGet.callback.accept(Collections.emptyList()));
				}
			} else {
				callbackExecutor.execute(() -> pendingPGet.callback.accept(Collections.emptyList()));
			}
		}
	}

	private <T> void deliverPendingPDelete(final PendingPDelete<T> pendingPDelete, final JsonNode deletedContainer,
			final TypeFactory typeFactory, final Executor callbackExecutor) {
		if (pendingPDelete != null) {
			if (deletedContainer != null) {
				final var kvpsType = typeFactory.constructParametricType(List.class,
						typeFactory.constructParametricType(KeyValuePair.class, pendingPDelete.type));
				try {
					final List<KeyValuePair<T>> kvps = this.objectMapper.readValue(deletedContainer.toString(),
							kvpsType);
					callbackExecutor.execute(() -> pendingPDelete.callback.accept(kvps));
				} catch (final JsonProcessingException e) {
					WorterbuchClient.log.error("Could not deserialize JSON '{}': {}", deletedContainer, e.getMessage());
					callbackExecutor.execute(() -> pendingPDelete.callback.accept(Collections.emptyList()));
				}
			} else {
				callbackExecutor.execute(() -> pendingPDelete.callback.accept(Collections.emptyList()));
			}
		}
	}

	private <T> void deliverPSubscription(final PSubscription<T> pSubscription, final JsonNode kvpsContainer,
			final JsonNode deletedContainer, final TypeFactory typeFactory, final Executor callbackExecutor) {
		if (pSubscription != null) {
			final var kvpsType = typeFactory.constructParametricType(List.class,
					typeFactory.constructParametricType(KeyValuePair.class, pSubscription.type));
			if (kvpsContainer != null) {
				try {
					final List<KeyValuePair<T>> kvps = this.objectMapper.readValue(kvpsContainer.toString(), kvpsType);
					final var event = new PStateEvent<>(kvps, null);
					callbackExecutor.execute(() -> pSubscription.callback.accept(event));
				} catch (final JsonProcessingException e) {
					WorterbuchClient.log.error("Could not deserialize JSON '{}': {}", kvpsContainer, e.getMessage());
					callbackExecutor.execute(() -> pSubscription.callback.accept(new PStateEvent<>(null, null)));
				}
			} else if (deletedContainer != null) {
				try {
					final List<KeyValuePair<T>> kvps = this.objectMapper.readValue(deletedContainer.toString(),
							kvpsType);
					final var event = new PStateEvent<>(null, kvps);
					callbackExecutor.execute(() -> pSubscription.callback.accept(event));
				} catch (final JsonProcessingException e) {
					WorterbuchClient.log.error("Could not deserialize JSON '{}': {}", deletedContainer, e.getMessage());
					callbackExecutor.execute(() -> pSubscription.callback.accept(new PStateEvent<>(null, null)));
				}
			}
		}
	}

	private void doClose() {
		this.closing = true;
		WorterbuchClient.log.info("Closing worterbuch client.");
		try {
			this.client.close();
		} catch (final Exception e) {
			this.onError.accept(new WorterbuchException("Error closing worterbuch client", e));
		}
	}

	private boolean protocolVersionIsSupported(final String protocolVersion) {
		return Constants.SUPPORTED_PROTOCOL_VERSIONS.contains(protocolVersion);
	}
}
