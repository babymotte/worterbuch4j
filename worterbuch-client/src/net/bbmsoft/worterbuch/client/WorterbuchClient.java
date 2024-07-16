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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import net.bbmsoft.worterbuch.client.impl.Config;
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
import net.bbmsoft.worterbuch.client.model.PSubscribe;
import net.bbmsoft.worterbuch.client.model.Publish;
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
import net.bbmsoft.worterbuch.client.pending.Subscription;

public class WorterbuchClient implements AutoCloseable {

	private static Map<UUID, WorterbuchClient> instances;

	public static WorterbuchClient connect(final URI uri, final BiConsumer<Integer, String> onDisconnect,
			final Consumer<Throwable> onError) throws InterruptedException, TimeoutException {

		final var exec = new WrappingExecutor(
				Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "worterbuch-client-callbacks")), onError);
		return WorterbuchClient.connect(uri, Optional.empty(), exec, onDisconnect, onError);
	}

	public static WorterbuchClient connect(final URI uri, final String authToken,
			final BiConsumer<Integer, String> onDisconnect, final Consumer<Throwable> onError)
			throws InterruptedException, TimeoutException {

		final var exec = new WrappingExecutor(
				Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "worterbuch-client-callbacks")), onError);
		return WorterbuchClient.connect(uri, Optional.of(authToken), exec, onDisconnect, onError);
	}

	public static WorterbuchClient connect(final URI uri, final Optional<String> authToken,
			final Executor callbackExecutor, final BiConsumer<Integer, String> onDisconnect,
			final Consumer<Throwable> onError) throws InterruptedException, TimeoutException {

		Objects.requireNonNull(uri);
		Objects.requireNonNull(onDisconnect);
		Objects.requireNonNull(onError);

		final var exec = new WrappingExecutor(
				Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "worterbuch-client")), onError);
		final var queue = new SynchronousQueue<WorterbuchClient>();
		final var ticket = UUID.randomUUID();

		exec.execute(() -> WorterbuchClient.initWorterbuchClient(uri, authToken, onDisconnect, onError, exec, queue,
				ticket, Objects.requireNonNull(callbackExecutor)));

		final var wb = queue.poll(Config.CONNECT_TIMEOUT, TimeUnit.SECONDS);
		if (wb == null) {
			throw new TimeoutException();
		}
		return wb;
	}

	private static void initWorterbuchClient(final URI uri, final Optional<String> authtoken,
			final BiConsumer<Integer, String> onDisconnect, final Consumer<? super Throwable> onError,
			final ScheduledExecutorService exec, final SynchronousQueue<WorterbuchClient> queue, final UUID ticket,
			final Executor callbackExecutor) {

		if (uri.getScheme().equals("tcp")) {
			WorterbuchClient.initTcpWorterbuchClient(uri, authtoken, onDisconnect, onError, exec, queue, ticket,
					callbackExecutor);
		} else {
			WorterbuchClient.initWsWorterbuchClient(uri, authtoken, onDisconnect, onError, exec, queue, ticket,
					callbackExecutor);
		}

	}

	private static void initWsWorterbuchClient(final URI uri, final Optional<String> authtoken,
			final BiConsumer<Integer, String> onDisconnect, final Consumer<? super Throwable> onError,
			final ScheduledExecutorService exec, final SynchronousQueue<WorterbuchClient> queue, final UUID ticket,
			final Executor callbackExecutor) {

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

			try {
				queue.offer(WorterbuchClient.instances.remove(ticket), Config.CONNECT_TIMEOUT, TimeUnit.SECONDS);
			} catch (final InterruptedException e) {
				onError.accept(
						new WorterbuchException("Client thread interrupted while offreing wortebruch client", e));
			}

		};

		final var client = new WebSocketClient();
		try {
			client.start();
		} catch (final Exception e) {
			onError.accept(new WorterbuchException("Could not start WS client", e));
			return;
		}

		final var clientSocket = new WsClientSocket(client, uri, onError, authtoken);

		final var wb = new WorterbuchClient(exec, onWelcome, onDisconnect, onError, clientSocket);

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
//				exec.execute(() -> wb.authenticate(authtoken));
			}

			@Override
			public void onWebSocketError(final Throwable cause) {
				exec.execute(() -> wb.onError(cause));
			}
		};

		try {
			clientSocket.open(socket);

			wb.exec.scheduleAtFixedRate(wb::checkKeepalive, 0, 1, TimeUnit.SECONDS);

			WorterbuchClient.instances.put(ticket, wb);
		} catch (final Exception e) {
			onError.accept(new WorterbuchException("Could not start client.", e));
		}
	}

	private static void initTcpWorterbuchClient(final URI uri, final Optional<String> authtoken,
			final BiConsumer<Integer, String> onDisconnect, final Consumer<? super Throwable> onError,
			final ScheduledExecutorService exec, final SynchronousQueue<WorterbuchClient> queue, final UUID ticket,
			final Executor callbackExecutor) {

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

			try {
				queue.offer(WorterbuchClient.instances.remove(ticket), Config.CONNECT_TIMEOUT, TimeUnit.SECONDS);
			} catch (final InterruptedException e) {
				onError.accept(
						new WorterbuchException("Client thread interrupted while offreing wortebruch client", e));
			}

		};

		try {

			final var clientSocket = new TcpClientSocket(uri, onDisconnect, onError);

			final var wb = new WorterbuchClient(exec, onWelcome, onDisconnect, onError, clientSocket);

			clientSocket.open(msg -> wb.messageReceived(msg, callbackExecutor));

			wb.exec.scheduleAtFixedRate(wb::checkKeepalive, 0, 1, TimeUnit.SECONDS);

			WorterbuchClient.instances.put(ticket, wb);

		} catch (final IOException e) {
			onError.accept(new WorterbuchException("Could not start client.", e));
			return;
		}

	}

	private final ScheduledExecutorService exec;
	private final BiConsumer<Integer, String> onDisconnect;
	private final Consumer<? super Throwable> onError;
	private final ClientSocket client;

	private final Logger log = LoggerFactory.getLogger(WorterbuchClient.class);
	private final AtomicLong transactionId = new AtomicLong();
	private final ObjectMapper objectMapper = new ObjectMapper();

	private final Map<Long, PendingGet<?>> pendingGets = new HashMap<>();
	private final Map<Long, PendingPGet<?>> pendingPGets = new HashMap<>();
	private final Map<Long, PendingDelete<?>> pendingDeletes = new HashMap<>();
	private final Map<Long, PendingPDelete<?>> pendingPDeletes = new HashMap<>();
	private final Map<Long, PendingLsState> pendingLsStates = new HashMap<>();
	private final Map<Long, Subscription<?>> subscriptions = new HashMap<>();
	private final Map<Long, PSubscription<?>> pSubscriptions = new HashMap<>();
	private final Map<Long, LsSubscription> lsSubscriptions = new HashMap<>();

	private long lastKeepaliveSent = System.currentTimeMillis();
	private long lastKeepaliveReceived = System.currentTimeMillis();

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
		this.exec.execute(() -> this.doSet(tid, key, value, onError));
		return tid;
	}

	public <T> long publish(final String key, final T value, final Consumer<? super Throwable> onError) {
		final var tid = this.acquireTid();
		this.exec.execute(() -> this.doPublish(tid, key, value, onError));
		return tid;
	}

	public <T> Future<Optional<T>> get(final String key, final Class<T> type) {
		final var fut = new CompletableFuture<Optional<T>>();
		this.<T>get(key, type, val -> fut.complete(val), e -> fut.completeExceptionally(e));
		return fut;
	}

	public <T> long get(final String key, final Class<T> type, final Consumer<Optional<T>> callback,
			final Consumer<? super Throwable> onError) {
		final var tid = this.acquireTid();
		this.exec.execute(() -> this.doGet(tid, key, type, callback, onError));
		return tid;
	}

	public <T> Future<Optional<T[]>> getArray(final String key, final Class<T> elementType) {
		final var fut = new CompletableFuture<Optional<T[]>>();
		this.<T>getArray(key, elementType, val -> fut.complete(val), e -> fut.completeExceptionally(e));
		return fut;
	}

	public <T> long getArray(final String key, final Class<T> elementType, final Consumer<Optional<T[]>> callback,
			final Consumer<? super Throwable> onError) {
		final var tid = this.acquireTid();
		this.exec.execute(() -> this.doGet(tid, key, (GenericArrayType) () -> elementType, callback, onError));
		return tid;
	}

	public <T> Future<List<KeyValuePair<T>>> pGet(final String pattern, final Class<T> type) {
		final var fut = new CompletableFuture<List<KeyValuePair<T>>>();
		this.<T>pGet(pattern, type, val -> fut.complete(val), e -> fut.completeExceptionally(e));
		return fut;
	}

	public <T> long pGet(final String pattern, final Class<T> type, final Consumer<List<KeyValuePair<T>>> callback,
			final Consumer<? super Throwable> onError) {
		final var tid = this.acquireTid();
		this.exec.execute(() -> this.doPGet(tid, pattern, type, callback, onError));
		return tid;
	}

	public <T> Future<Optional<T>> delete(final String key, final Class<T> type) {
		final var fut = new CompletableFuture<Optional<T>>();
		this.<T>delete(key, type, val -> fut.complete(val), e -> fut.completeExceptionally(e));
		return fut;
	}

	public <T> long delete(final String key, final Class<T> type, final Consumer<Optional<T>> callback,
			final Consumer<? super Throwable> onError) {
		final var tid = this.acquireTid();
		this.exec.execute(() -> this.doDelete(tid, key, type, callback, onError));
		return tid;
	}

	public long delete(final String key, final Consumer<? super Throwable> onError) {
		return this.delete(key, null, null, onError);
	}

	public <T> Future<List<KeyValuePair<T>>> pDelete(final String pattern, final Class<T> type) {
		final var fut = new CompletableFuture<List<KeyValuePair<T>>>();
		this.<T>pDelete(pattern, type, val -> fut.complete(val), e -> fut.completeExceptionally(e));
		return fut;
	}

	public <T> long pDelete(final String pattern, final Class<T> type, final Consumer<List<KeyValuePair<T>>> callback,
			final Consumer<? super Throwable> onError) {
		final var tid = this.acquireTid();
		this.exec.execute(() -> this.doPDelete(tid, pattern, type, callback, onError));
		return tid;
	}

	public long pDelete(final String pattern, final Consumer<? super Throwable> onError) {
		return this.pDelete(pattern, null, null, onError);
	}

	public Future<List<String>> ls(final String parent) {
		final var fut = new CompletableFuture<List<String>>();
		this.ls(parent, val -> fut.complete(val), e -> fut.completeExceptionally(e));
		return fut;
	}

	public long ls(final String parent, final Consumer<List<String>> callback,
			final Consumer<? super Throwable> onError) {
		final var tid = this.acquireTid();
		this.exec.execute(() -> this.doLs(tid, parent, callback, onError));
		return tid;
	}

	public <T> long subscribe(final String key, final boolean unique, final boolean liveOnly, final Class<T> type,
			final Consumer<Optional<T>> callback, final Consumer<? super Throwable> onError) {
		final var tid = this.acquireTid();
		this.exec.execute(() -> this.doSubscribe(tid, key, unique, liveOnly, type, callback, onError));
		return tid;
	}

	public <T> long subscribeArray(final String key, final boolean unique, final boolean liveOnly,
			final Class<T> elementType, final Consumer<Optional<T[]>> callback,
			final Consumer<? super Throwable> onError) {
		final var tid = this.acquireTid();
		this.exec.execute(() -> this.doSubscribe(tid, key, unique, liveOnly, (GenericArrayType) () -> elementType,
				callback, onError));
		return tid;
	}

	public <T> long pSubscribe(final String pattern, final boolean unique, final boolean liveOnly,
			final Optional<Long> aggregateEvents, final Class<T> type, final Consumer<PStateEvent<T>> callback,
			final Consumer<? super Throwable> onError) {
		final var tid = this.acquireTid();
		this.exec.execute(
				() -> this.doPSubscribe(tid, pattern, unique, liveOnly, aggregateEvents, type, callback, onError));
		return tid;
	}

	public void unsubscribe(final long transactionId, final Consumer<? super Throwable> onError) {
		this.exec.execute(() -> this.doUnsubscribe(transactionId, onError));
	}

	public long subscribeLs(final String parent, final Consumer<List<String>> callback,
			final Consumer<? super Throwable> onError) {
		final var tid = this.acquireTid();
		this.exec.execute(() -> this.doSubscribeLs(tid, parent, callback, onError));
		return tid;
	}

	public void unsubscribeLs(final long transactionId, final Consumer<? super Throwable> onError) {
		this.exec.execute(() -> this.doUnsubscribeLs(transactionId, onError));
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

	private <T> void doSet(final long tid, final String key, final T value, final Consumer<? super Throwable> onError) {

		final var set = new Set();
		set.setTransactionId(tid);
		set.setKey(key);
		set.setValue(value);
		final var msg = new ClientMessage();
		msg.setSet(set);

		this.sendMessage(msg, onError);
	}

	private <T> void doPublish(final long tid, final String key, final T value,
			final Consumer<? super Throwable> onError) {

		final var pub = new Publish();
		pub.setTransactionId(tid);
		pub.setKey(key);
		pub.setValue(value);
		final var msg = new ClientMessage();
		msg.setPublish(pub);

		this.sendMessage(msg, onError);
	}

	private <T> void doGet(final long tid, final String key, final Type type, final Consumer<Optional<T>> callback,
			final Consumer<? super Throwable> onError) {

		this.pendingGets.put(tid, new PendingGet<>(callback, type));

		final var get = new Get();
		get.setTransactionId(tid);
		get.setKey(key);
		final var msg = new ClientMessage();
		msg.setGet(get);

		this.sendMessage(msg, onError);
	}

	private <T> void doPGet(final long tid, final String pattern, final Class<T> type,
			final Consumer<List<KeyValuePair<T>>> callback, final Consumer<? super Throwable> onError) {

		this.pendingPGets.put(tid, new PendingPGet<>(callback, type));

		final var pget = new PGet();
		pget.setTransactionId(tid);
		pget.setRequestPattern(pattern);
		final var msg = new ClientMessage();
		msg.setpGet(pget);

		this.sendMessage(msg, onError);
	}

	private <T> void doDelete(final long tid, final String key, final Class<T> type,
			final Consumer<Optional<T>> callback, final Consumer<? super Throwable> onError) {

		if (callback != null) {
			this.pendingDeletes.put(tid, new PendingDelete<>(callback, Objects.requireNonNull(type)));
		}

		final var del = new Delete();
		del.setTransactionId(tid);
		del.setKey(key);
		final var msg = new ClientMessage();
		msg.setDelete(del);

		this.sendMessage(msg, onError);
	}

	private <T> void doPDelete(final long tid, final String pattern, final Class<T> type,
			final Consumer<List<KeyValuePair<T>>> callback, final Consumer<? super Throwable> onError) {

		if (callback != null) {
			this.pendingPDeletes.put(tid, new PendingPDelete<>(callback, Objects.requireNonNull(type)));
		}

		final var pdel = new PDelete();
		pdel.setTransactionId(tid);
		pdel.setRequestPattern(pattern);
		final var msg = new ClientMessage();
		msg.setpDelete(pdel);

		this.sendMessage(msg, onError);
	}

	private void doLs(final long tid, final String parent, final Consumer<List<String>> callback,
			final Consumer<? super Throwable> onError) {

		this.pendingLsStates.put(tid, new PendingLsState(callback));

		final var ls = new Ls();
		ls.setTransactionId(tid);
		ls.setParent(parent);
		final var msg = new ClientMessage();
		msg.setLs(ls);

		this.sendMessage(msg, onError);
	}

	private <T> void doSubscribe(final long tid, final String key, final boolean unique, final boolean liveOnly,
			final Type type, final Consumer<Optional<T>> callback, final Consumer<? super Throwable> onError) {

		this.subscriptions.put(tid, new Subscription<>(callback, type));

		final var sub = new Subscribe();
		sub.setTransactionId(tid);
		sub.setKey(key);
		sub.setUnique(unique);
		sub.setLiveOnly(liveOnly);
		final var msg = new ClientMessage();
		msg.setSubscribe(sub);

		this.sendMessage(msg, onError);
	}

	private <T> void doPSubscribe(final long tid, final String pattern, final boolean unique, final boolean liveOnly,
			final Optional<Long> aggregateEvents, final Class<T> type, final Consumer<PStateEvent<T>> callback,
			final Consumer<? super Throwable> onError) {

		this.pSubscriptions.put(tid, new PSubscription<>(callback, type));

		final var psub = new PSubscribe();
		psub.setTransactionId(tid);
		psub.setRequestPattern(pattern);
		psub.setUnique(unique);
		psub.setLiveOnly(liveOnly);
		aggregateEvents.ifPresent(psub::setAggregateEvents);
		final var msg = new ClientMessage();
		msg.setpSubscribe(psub);

		this.sendMessage(msg, onError);
	}

	private void doUnsubscribe(final long transactionId, final Consumer<? super Throwable> onError) {

		this.subscriptions.remove(transactionId);
		this.pSubscriptions.remove(transactionId);

		final var unsub = new Unsubscribe();
		unsub.setTransactionId(transactionId);
		final var msg = new ClientMessage();
		msg.setUnsubscribe(unsub);

		this.sendMessage(msg, onError);
	}

	private void doSubscribeLs(final long tid, final String parent, final Consumer<List<String>> callback,
			final Consumer<? super Throwable> onError) {

		this.lsSubscriptions.put(tid, new LsSubscription(callback));

		final var lssub = new SubscribeLs();
		lssub.setTransactionId(tid);
		lssub.setParent(parent);
		final var msg = new ClientMessage();
		msg.setSubscribeLs(lssub);

		this.sendMessage(msg, onError);
	}

	private void doUnsubscribeLs(final long transactionId, final Consumer<? super Throwable> onError) {

		this.lsSubscriptions.remove(transactionId);

		final var unsub = new UnsubscribeLs();
		unsub.setTransactionId(transactionId);
		final var msg = new ClientMessage();
		msg.setUnsubscribeLs(unsub);

		this.sendMessage(msg, onError);
	}

	private void sendMessage(final ClientMessage msg, final Consumer<? super Throwable> onError) {

		try {
			final var json = msg != null ? this.objectMapper.writeValueAsString(msg) : "\"\"";
			this.client.sendString(json);
			this.lastKeepaliveSent = System.currentTimeMillis();
		} catch (final JsonProcessingException e) {
			onError.accept(new WorterbuchException("Could not serialize message", e));
		} catch (final IOException e) {
			onError.accept(new WorterbuchException("Could not send websocket message", e));
		}
	}

	void messageReceived(final String message, final Executor callbackExecutor) {

		this.lastKeepaliveReceived = System.currentTimeMillis();

		if (this.isKeepalive(message)) {
			return;
		}

		JsonNode parent;

		try {
			parent = this.objectMapper.readTree(message);

			final var ackContainer = parent.get("ack");
			if (ackContainer != null) {
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

					if (!handled) {
						this.log.error("Received error message from server: '{}'", errContainer);
					}

				} catch (final JsonProcessingException e) {
					this.log.error("Could not deserialize JSON '{}': {}", errContainer, e.getMessage());
				}
				return;
			}

			final var wcContainer = parent.get("welcome");
			if (wcContainer != null) {
				try {
					final var welcome = this.objectMapper.readValue(wcContainer.toString(), Welcome.class);
					this.log.info("Received welcome message.");
					if (this.onWelcome != null) {
						this.onWelcome.accept(welcome, this);
						this.onWelcome = null;
					}
				} catch (final JsonProcessingException e) {
					this.log.error("Could not deserialize JSON '{}': {}", wcContainer, e.getMessage());
				}
				return;
			}

			final var stateContainer = parent.get("state");
			if (stateContainer != null) {
				final var transactionId = stateContainer.get("transactionId").asLong();

				final var keyValueContainer = stateContainer.get("keyValue");
				final var deletedContainer = stateContainer.get("deleted");

				final var typeFactory = this.objectMapper.getTypeFactory();

				final var pendingGet = this.pendingGets.remove(transactionId);
				this.deliverPendingGet(pendingGet, keyValueContainer, typeFactory, callbackExecutor);

				final var pendingDelete = this.pendingDeletes.remove(transactionId);
				this.deliverPendingDelete(pendingDelete, deletedContainer, typeFactory, callbackExecutor);

				final var subscription = this.subscriptions.get(transactionId);
				this.deliverSubscription(subscription, keyValueContainer, deletedContainer, typeFactory,
						callbackExecutor);

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

					this.log.error("Could not deserialize JSON '{}': {}", childrenContainer, e.getMessage());

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
			this.log.error("Received invalid JSON message: '{}'", message);
		}

	}

	void onDisconnect(final int statusCode, final String reason) {
		if (!this.closing) {
			this.log.error("WebSocket connection to server closed.");
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

	void authorize(final Optional<String> authToken) {
		authToken.ifPresent(token -> {
			final var auth = new AuthorizationRequest(token);
			final var msg = new ClientMessage();
			msg.setAuthorizationRequest(auth);
			this.sendMessage(msg, this.onError);
		});
	}

	void onError(final Throwable cause) {
		this.onError.accept(cause);
	}

	private boolean isKeepalive(final String message) {
		return message.isBlank();
	}

	private <T> void deliverPendingGet(final PendingGet<T> pendingGet, final JsonNode keyValueContainer,
			final TypeFactory typeFactory, final Executor callbackExecutor) {
		if (pendingGet != null) {
			if (keyValueContainer != null) {
				final var kvpType = typeFactory.constructParametricType(KeyValuePair.class,
						typeFactory.constructType(pendingGet.type));
				try {
					final KeyValuePair<T> kvp = this.objectMapper.readValue(keyValueContainer.toString(), kvpType);
					callbackExecutor.execute(() -> pendingGet.callback.accept(Optional.ofNullable(kvp.getValue())));
				} catch (final JsonProcessingException e) {
					this.log.error("Could not deserialize JSON '{}': {}", keyValueContainer, e.getMessage());
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
				final var kvpType = typeFactory.constructParametricType(KeyValuePair.class, pendingDelete.type);
				try {
					final KeyValuePair<T> kvp = this.objectMapper.readValue(deletedContainer.toString(), kvpType);
					callbackExecutor.execute(() -> pendingDelete.callback.accept(Optional.ofNullable(kvp.getValue())));
				} catch (final JsonProcessingException e) {
					this.log.error("Could not deserialize JSON '{}': {}", deletedContainer, e.getMessage());
					callbackExecutor.execute(() -> pendingDelete.callback.accept(Optional.empty()));
				}
			} else {
				callbackExecutor.execute(() -> pendingDelete.callback.accept(Optional.empty()));
			}
		}
	}

	private <T> void deliverSubscription(final Subscription<T> subscription, final JsonNode keyValueContainer,
			final JsonNode deletedContainer, final TypeFactory typeFactory, final Executor callbackExecutor) {
		if (subscription != null) {
			final var kvpType = typeFactory.constructParametricType(KeyValuePair.class,
					typeFactory.constructType(subscription.type));
			if (keyValueContainer != null) {
				try {
					final KeyValuePair<T> kvp = this.objectMapper.readValue(keyValueContainer.toString(), kvpType);
					callbackExecutor.execute(() -> subscription.callback.accept(Optional.ofNullable(kvp.getValue())));
				} catch (final JsonProcessingException e) {
					this.log.error("Could not deserialize JSON '{}': {}", keyValueContainer, e.getMessage());
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
					this.log.error("Could not deserialize JSON '{}': {}", kvpsContainer, e.getMessage());
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
					this.log.error("Could not deserialize JSON '{}': {}", deletedContainer, e.getMessage());
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
					this.log.error("Could not deserialize JSON '{}': {}", kvpsContainer, e.getMessage());
					callbackExecutor.execute(() -> pSubscription.callback.accept(new PStateEvent<>(null, null)));
				}
			} else if (deletedContainer != null) {
				try {
					final List<KeyValuePair<T>> kvps = this.objectMapper.readValue(deletedContainer.toString(),
							kvpsType);
					final var event = new PStateEvent<>(null, kvps);
					callbackExecutor.execute(() -> pSubscription.callback.accept(event));
				} catch (final JsonProcessingException e) {
					this.log.error("Could not deserialize JSON '{}': {}", deletedContainer, e.getMessage());
					callbackExecutor.execute(() -> pSubscription.callback.accept(new PStateEvent<>(null, null)));
				}
			}
		}
	}

	private void checkKeepalive() {
		final var now = System.currentTimeMillis();

		final var lag = this.lastKeepaliveReceived - this.lastKeepaliveSent;

		if ((now - this.lastKeepaliveSent) >= 1000) {
			this.sendMessage(null, e -> this.onError.accept(new WorterbuchException("Could not send keepalive", e)));
		}

		if (lag >= 2000) {
			this.log.warn("Server has been inactive for {} seconds …", lag / 1000);
		}
		if (lag >= Config.KEEPALIVE_TIMEOUT) {
			this.log.warn("Server has been inactive for too long, disconnecting.");
			this.close();
		}
	}

	private void doClose() {
		this.closing = true;
		this.log.info("Closing worterbuch client.");
		try {
			this.client.close();
		} catch (final Exception e) {
			this.onError.accept(new WorterbuchException("Error closing worterbuch client", e));
		}
	}

	private boolean protocolVersionIsSupported(final String protocolVersion) {
		final var supportedVersions = Arrays.asList("0.8");
		return supportedVersions.contains(protocolVersion);
	}
}
