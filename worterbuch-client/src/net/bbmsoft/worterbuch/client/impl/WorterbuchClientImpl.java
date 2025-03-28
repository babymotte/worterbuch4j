package net.bbmsoft.worterbuch.client.impl;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bbmsoft.worterbuch.client.api.ErrorCode;
import net.bbmsoft.worterbuch.client.api.TypedKeyValuePair;
import net.bbmsoft.worterbuch.client.api.TypedPStateEvent;
import net.bbmsoft.worterbuch.client.api.WorterbuchClient;
import net.bbmsoft.worterbuch.client.api.util.Tuple;
import net.bbmsoft.worterbuch.client.api.util.type.TypeUtil;
import net.bbmsoft.worterbuch.client.error.WorterbuchError;
import net.bbmsoft.worterbuch.client.error.WorterbuchException;
import net.bbmsoft.worterbuch.client.model.Err;
import net.bbmsoft.worterbuch.client.model.KeyValuePair;
import net.bbmsoft.worterbuch.client.model.Welcome;
import net.bbmsoft.worterbuch.client.pending.LsSubscription;
import net.bbmsoft.worterbuch.client.pending.PSubscription;
import net.bbmsoft.worterbuch.client.pending.PendingAck;
import net.bbmsoft.worterbuch.client.pending.PendingAuth;
import net.bbmsoft.worterbuch.client.pending.PendingCGet;
import net.bbmsoft.worterbuch.client.pending.PendingDelete;
import net.bbmsoft.worterbuch.client.pending.PendingGet;
import net.bbmsoft.worterbuch.client.pending.PendingLs;
import net.bbmsoft.worterbuch.client.pending.PendingPDelete;
import net.bbmsoft.worterbuch.client.pending.PendingPGet;
import net.bbmsoft.worterbuch.client.pending.Subscription;
import net.bbmsoft.worterbuch.client.response.Error;
import net.bbmsoft.worterbuch.client.response.Future;
import net.bbmsoft.worterbuch.client.response.Ok;
import net.bbmsoft.worterbuch.client.response.Response;

public class WorterbuchClientImpl implements WorterbuchClient {

	private static final Logger log = LoggerFactory.getLogger(WorterbuchClientImpl.class);

	private final ClientSocket client;
	private final Consumer<WorterbuchException> onError;
	private final AtomicLong transactionId;
	private final ObjectMapper objectMapper;
	private final MessageSerDe messageSerde;
	private final MessageSender messageSender;

	private final AtomicReference<Consumer<Welcome>> onWelcome;

	private final AtomicReference<PendingAuth> pendingAuth;

	private final Map<Long, PendingAck> pendingAcks;

	private final Map<Long, PendingLs> pendingLss;
	private final Map<Long, PendingGet<?>> pendingGets;
	private final Map<Long, PendingPGet<?>> pendingPGets;
	private final Map<Long, PendingDelete<?>> pendingDeletes;
	private final Map<Long, PendingPDelete<?>> pendingPDeletes;
	private final Map<Long, PendingCGet<?>> pendingCGets;

	private final Map<Long, Subscription<?>> subscriptions;
	private final Map<Long, PSubscription<?>> pSubscriptions;
	private final Map<Long, LsSubscription> lsSubscriptions;

	private final Map<String, Tuple<?, Long>> casCache;

	public volatile String clientId;

	WorterbuchClientImpl(final ClientSocket client, final Executor exec, final Consumer<WorterbuchException> onError) {

		this.client = Objects.requireNonNull(client);
		this.onError = Objects.requireNonNull(onError);
		this.transactionId = new AtomicLong();
		this.objectMapper = new ObjectMapper();
		this.messageSerde = new MessageSerDe(this.objectMapper);
		this.messageSender = new MessageSender(Objects.requireNonNull(client), Objects.requireNonNull(exec),
				Objects.requireNonNull(onError));

		this.onWelcome = new AtomicReference<>();

		this.pendingAuth = new AtomicReference<>();

		this.pendingAcks = new ConcurrentHashMap<>();

		this.pendingGets = new ConcurrentHashMap<>();
		this.pendingPGets = new ConcurrentHashMap<>();
		this.pendingDeletes = new ConcurrentHashMap<>();
		this.pendingPDeletes = new ConcurrentHashMap<>();
		this.pendingLss = new ConcurrentHashMap<>();
		this.pendingCGets = new ConcurrentHashMap<>();

		this.subscriptions = new ConcurrentHashMap<>();
		this.pSubscriptions = new ConcurrentHashMap<>();
		this.lsSubscriptions = new ConcurrentHashMap<>();

		this.casCache = new ConcurrentHashMap<>();
	}

	@Override
	public void close() {
		WorterbuchClientImpl.log.info("Closing worterbuch client.");
		try {
			this.client.close();
		} catch (final Exception e) {
			this.onError.accept(new WorterbuchException("Error closing worterbuch client", e));
		}
	}

	@Override
	public Future<Void> set(final String key, final Object value) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Response<Void>>();
		final var msg = MessageBuilder.setMessage(tid, key, value);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingAcks.put(tid, new PendingAck(msg, fut));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public Future<Void> publish(final String key, final Object value) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Response<Void>>();
		final var msg = MessageBuilder.publishMessage(tid, key, value);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingAcks.put(tid, new PendingAck(msg, fut));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public Future<Void> initPubStream(final String key) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Response<Void>>();
		final var msg = MessageBuilder.sPubInitMessage(tid, key);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingAcks.put(tid, new PendingAck(msg, fut));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public Future<Void> streamPub(final long transactionId, final Object value) {
		final var fut = new CompletableFuture<Response<Void>>();
		final var msg = MessageBuilder.sPubMessage(transactionId, value);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingAcks.put(transactionId, new PendingAck(msg, fut));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, transactionId);
	}

	@Override
	public <T> Future<T> get(final String key, final TypeReference<T> type) {
		return this.get(key, TypeFactory.defaultInstance().constructType(type));
	}

	@Override
	public <T> Future<T> get(final String key, final Class<T> type) {
		return this.get(key, (Type) type);
	}

	private <T> Future<T> get(final String key, final Type type) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Response<T>>();
		final var msg = MessageBuilder.getMessage(tid, key);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingGets.put(tid, new PendingGet<>(msg, fut, type));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public <T> Future<List<TypedKeyValuePair<T>>> pGet(final String pattern, final TypeReference<T> type) {
		return this.pGet(pattern, TypeFactory.defaultInstance().constructType(type));
	}

	@Override
	public <T> Future<List<TypedKeyValuePair<T>>> pGet(final String pattern, final Class<T> type) {
		return this.pGet(pattern, (Type) type);
	}

	private <T> Future<List<TypedKeyValuePair<T>>> pGet(final String pattern, final Type type) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Response<List<TypedKeyValuePair<T>>>>();
		final var msg = MessageBuilder.pGetMessage(tid, pattern);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingPGets.put(tid, new PendingPGet<>(msg, fut, type));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public <T> Future<T> delete(final String key, final TypeReference<T> type) {
		return this.delete(key, TypeFactory.defaultInstance().constructType(type));
	}

	@Override
	public <T> Future<T> delete(final String key, final Class<T> type) {
		return this.delete(key, (Type) type);
	}

	private <T> Future<T> delete(final String key, final Type type) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Response<T>>();
		final var msg = MessageBuilder.deleteMessage(tid, key);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingDeletes.put(tid, new PendingDelete<>(msg, fut, type));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public Future<Void> delete(final String key) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Response<Void>>();
		final var msg = MessageBuilder.deleteMessage(tid, key);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingDeletes.put(tid, new PendingDelete<>(msg, fut, null));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public <T> Future<List<TypedKeyValuePair<T>>> pDelete(final String pattern, final TypeReference<T> type) {
		return this.pDelete(pattern, TypeFactory.defaultInstance().constructType(type));
	}

	@Override
	public <T> Future<List<TypedKeyValuePair<T>>> pDelete(final String pattern, final Class<T> type) {
		return this.pDelete(pattern, (Type) type);
	}

	private <T> Future<List<TypedKeyValuePair<T>>> pDelete(final String pattern, final Type type) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Response<List<TypedKeyValuePair<T>>>>();
		final var msg = MessageBuilder.pDeleteMessage(tid, pattern, false);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingPDeletes.put(tid, new PendingPDelete<>(msg, fut, type));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public Future<Void> pDelete(final String pattern) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Response<Void>>();
		final var msg = MessageBuilder.pDeleteMessage(tid, pattern, true);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingAcks.put(tid, new PendingAck(msg, fut));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public Future<List<String>> ls(final String parent) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Response<List<String>>>();
		final var msg = MessageBuilder.lsMessage(tid, parent);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingLss.put(tid, new PendingLs(msg, fut));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public Future<List<String>> pLs(final String parentPattern) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Response<List<String>>>();
		final var msg = MessageBuilder.pLsMessage(tid, parentPattern);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingLss.put(tid, new PendingLs(msg, fut));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public <T> Future<Void> subscribe(final String key, final boolean unique, final boolean liveOnly,
			final TypeReference<T> type, final Consumer<Optional<T>> callback) {
		return this.subscribe(key, unique, liveOnly, TypeFactory.defaultInstance().constructType(type), callback);
	}

	@Override
	public <T> Future<Void> subscribe(final String key, final boolean unique, final boolean liveOnly,
			final Class<T> type, final Consumer<Optional<T>> callback) {
		return this.subscribe(key, unique, liveOnly, (Type) type, callback);

	}

	private <T> Future<Void> subscribe(final String key, final boolean unique, final boolean liveOnly, final Type type,
			final Consumer<Optional<T>> callback) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Response<Void>>();
		final var msg = MessageBuilder.subscribeMessage(tid, key, unique, liveOnly);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingAcks.put(tid, new PendingAck(msg, fut));
		this.subscriptions.put(tid, new Subscription<>(msg, callback, type));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public <T> Future<Void> pSubscribe(final String pattern, final boolean unique, final boolean liveOnly,
			final Optional<Long> aggregateEvents, final TypeReference<T> type,
			final Consumer<TypedPStateEvent<T>> callback) {
		return this.pSubscribe(pattern, unique, liveOnly, aggregateEvents,
				TypeFactory.defaultInstance().constructType(type), callback);
	}

	@Override
	public <T> Future<Void> pSubscribe(final String pattern, final boolean unique, final boolean liveOnly,
			final Optional<Long> aggregateEvents, final Class<T> type, final Consumer<TypedPStateEvent<T>> callback) {
		return this.pSubscribe(pattern, unique, liveOnly, aggregateEvents, (Type) type, callback);
	}

	private <T> Future<Void> pSubscribe(final String pattern, final boolean unique, final boolean liveOnly,
			final Optional<Long> aggregateEvents, final Type type, final Consumer<TypedPStateEvent<T>> callback) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Response<Void>>();
		final var msg = MessageBuilder.pSubscribeMessage(tid, pattern, unique, liveOnly, aggregateEvents);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingAcks.put(tid, new PendingAck(msg, fut));
		this.pSubscriptions.put(tid, new PSubscription<>(msg, callback, type));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public Future<Void> unsubscribe(final long transactionId) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Response<Void>>();
		final var msg = MessageBuilder.unsubscribeMessage(tid);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingAcks.put(tid, new PendingAck(msg, fut));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public Future<Void> subscribeLs(final String parent, final Consumer<List<String>> callback) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Response<Void>>();
		final var msg = MessageBuilder.subscribeLsMessage(tid, parent);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingAcks.put(tid, new PendingAck(msg, fut));
		this.lsSubscriptions.put(tid, new LsSubscription(msg, callback));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public Future<Void> unsubscribeLs(final long transactionId) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Response<Void>>();
		final var msg = MessageBuilder.unsubscribeLsMessage(tid);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingAcks.put(tid, new PendingAck(msg, fut));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public <T> Future<Void> cSet(final String key, final T value, final long version) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Response<Void>>();
		final var msg = MessageBuilder.cSetMessage(tid, key, value, version);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingAcks.put(tid, new PendingAck(msg, fut));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public <T> Future<Tuple<T, Long>> cGet(final String key, final TypeReference<T> type) {
		return this.cGet(key, TypeFactory.defaultInstance().constructType(type));
	}

	@Override
	public <T> Future<Tuple<T, Long>> cGet(final String key, final Class<T> type) {
		return this.cGet(key, (Type) type);
	}

	private <T> Future<Tuple<T, Long>> cGet(final String key, final Type type) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Response<Tuple<T, Long>>>();
		final var msg = MessageBuilder.cGetMessage(tid, key);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingCGets.put(tid, new PendingCGet<>(msg, fut, type));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public <T, V> boolean update(final String key, final Function<Optional<T>, V> transform,
			final TypeReference<T> type) {
		return this.update(key, transform, TypeFactory.defaultInstance().constructType(type));
	}

	@Override
	public <T, V> boolean update(final String key, final Function<Optional<T>, V> transform, final Class<T> type) {
		return this.update(key, transform, (Type) type);
	}

	private <T, V> boolean update(final String key, final Function<Optional<T>, V> transform, final Type type) {

		final var attempts = 100;

		try {
			for (var i = 0; i < attempts; i++) {
				if (this.tryUpdate(key, transform, type)) {
					return true;
				}
			}
		} catch (InterruptedException | TimeoutException | WorterbuchException e) {
			this.onError.accept(new WorterbuchException("Error while trying to update compare-and-swap value", e));
			return false;
		}

		WorterbuchClientImpl.log.error("Unsuccessfully tried to update {} {} times, giving up.", key, attempts);
		return false;
	}

	@Override
	public <T> boolean update(final String key, final Consumer<T> update, final TypeReference<T> type) {
		return this.update(key, update, TypeFactory.defaultInstance().constructType(type));
	}

	@Override
	public <T> boolean update(final String key, final Consumer<T> update, final Class<T> type) {
		return this.update(key, update, (Type) type);
	}

	private <T> boolean update(final String key, final Consumer<T> update, final Type type) {

		return this.<T, T>update(key, mi -> {
			final var i = mi.orElseGet(() -> TypeUtil.instantiate(type));
			update.accept(i);
			return i;
		}, type);

	}

	@Override
	public Future<Void> lock(final String key) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Response<Void>>();
		final var msg = MessageBuilder.lockMessage(tid, key);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingAcks.put(tid, new PendingAck(msg, fut));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public Future<Void> acquireLock(final String key) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Response<Void>>();
		final var msg = MessageBuilder.acquireLockMessage(tid, key);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingAcks.put(tid, new PendingAck(msg, fut));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public Future<Void> releaseLock(final String key) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Response<Void>>();
		final var msg = MessageBuilder.releaseLockMessage(tid, key);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingAcks.put(tid, new PendingAck(msg, fut));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public Future<List<String>> getGraveGoods() {
		return this.get("$SYS/clients/" + this.getClientId() + "/graveGoods", TypeUtil.list(String.class));
	}

	@Override
	public Future<List<KeyValuePair>> getLastWill() {
		return this.get("$SYS/clients/" + this.getClientId() + "/lastWill", TypeUtil.list(KeyValuePair.class));
	}

	@Override
	public Future<Void> setGraveGoods(final List<String> graveGoods) {
		return this.cSet("$SYS/clients/" + this.getClientId() + "/graveGoods", graveGoods, 0);
	}

	@Override
	public Future<Void> setLastWill(final List<KeyValuePair> lastWill) {
		return this.cSet("$SYS/clients/" + this.getClientId() + "/lastWill", lastWill, 0);
	}

	@Override
	public void updateGraveGoods(final Consumer<List<String>> update) {
		this.update("$SYS/clients/" + this.getClientId() + "/graveGoods", update, TypeUtil.list(String.class));
	}

	@Override
	public void updateLastWill(final Consumer<List<KeyValuePair>> update) {
		this.update("$SYS/clients/" + this.getClientId() + "/lastWill", update, TypeUtil.list(KeyValuePair.class));
	}

	@Override
	public Future<Void> setClientName(final String name) {
		return this.set("$SYS/clients/" + this.getClientId() + "/clientName", name);
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

	private long acquireTid() {
		final var nextTid = this.transactionId.incrementAndGet();
		return nextTid;
	}

	private <T, V> boolean tryUpdate(final String key, final Function<Optional<T>, V> transform, final Type type)
			throws WorterbuchException, InterruptedException, TimeoutException {

		@SuppressWarnings("unchecked")
		final var cached = (Tuple<T, Long>) this.casCache.get(key);

		if (cached != null) {
			// value is cached

			try {
				@SuppressWarnings("unchecked")
				final var cloned = (T) this.objectMapper.treeToValue(this.objectMapper.valueToTree(cached.first()),
						cached.first().getClass());
				final var value = Optional.ofNullable(cloned);
				final var version = cached.second();
				final var newValue = transform.apply(value);
				final var set = this.cSet(key, newValue, version).await(Duration.ofSeconds(5));

				return this.evalUpdate(set, key, newValue, version + 1);
			} catch (JsonProcessingException | IllegalArgumentException e) {
				// cached object is corrupt, proceed to fetch it new
			}
		}

		final var get = this.<T>cGet(key, type).await(Duration.ofSeconds(5));

		if (get instanceof final Ok<Tuple<T, Long>> ok) {

			// value exists

			final var value = Optional.ofNullable(ok.value().first());
			final var version = ok.value().second();
			final var newValue = transform.apply(value);

			final var set = this.cSet(key, newValue, version).await(Duration.ofSeconds(5));

			return this.evalUpdate(set, key, newValue, version + 1);

		} else if (get instanceof final Error error) {

			if (error.err().getErrorCode() == ErrorCode.NoSuchValue) {

				// value does not exist

				final var value = Optional.<T>empty();
				final var version = 0L;
				final var newValue = transform.apply(value);

				final var set = this.cSet(key, newValue, version).await(Duration.ofSeconds(5));

				return this.evalUpdate(set, key, newValue, version + 1);
			}

			throw new WorterbuchException(new WorterbuchError(error.err()));

		} else {
			throw new IllegalStateException();
		}

	}

	private <T, V> boolean evalUpdate(final Response<Void> set, final String key, final Object newValue,
			final Long newVersion) throws InterruptedException, WorterbuchException {

		if (set instanceof Ok) {

			// set succeeded

			this.casCache.put(key, new Tuple<>(newValue, newVersion));

			return true;

		} else if (set instanceof final Error error) {

			if (error.err().getErrorCode() == ErrorCode.CasVersionMismatch) {

				// value was updated in the meantime

				this.casCache.remove(key);
				return false;

			}

			throw new WorterbuchException(new WorterbuchError(error.err()));

		} else {
			throw new IllegalStateException();
		}
	}

	public Future<Void> switchProtocol(final int protoVersion) {
		final var tid = 0L;
		final var fut = new CompletableFuture<Response<Void>>();
		final var msg = MessageBuilder.switchProtocolMessage(protoVersion);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingAcks.put(tid, new PendingAck(msg, fut));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	public Future<Void> authorize(final String authToken) {
		final var tid = 0L;
		final var fut = new CompletableFuture<Response<Void>>();
		final var msg = MessageBuilder.authorizationMessage(authToken);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingAuth.set(new PendingAuth(msg, fut));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	public void start(final Consumer<Welcome> onWelcome) {
		this.onWelcome.set(onWelcome);
	}

	public void messageReceived(final String message) {

		JsonNode parent;

		try {
			parent = this.objectMapper.readTree(message);

			final var authContainer = parent.get("authorized");
			if (authContainer != null) {

				final var pending = this.pendingAuth.getAndSet(null);
				if (pending != null) {
					pending.callback().complete(new Ok<>(null));
				}

				return;
			}

			final var ackContainer = parent.get("ack");
			if (ackContainer != null) {

				final var transactionId = ackContainer.get("transactionId").asLong();

				final var pending = this.pendingAcks.remove(transactionId);
				if (pending != null) {
					pending.callback().complete(new Ok<>(null));
				}

				return;
			}

			final var errContainer = parent.get("err");
			if (errContainer != null) {
				try {
					final var err = this.objectMapper.readValue(errContainer.toString(), Err.class);
					final var transactionId = err.getTransactionId();
					var handled = false;

					final var pendingAck = this.pendingAcks.remove(transactionId);
					if (pendingAck != null) {
						handled = true;
						pendingAck.callback().complete(new Error<>(err));
					}

					final var pendingGet = this.pendingGets.remove(transactionId);
					if (pendingGet != null) {
						handled = true;
						pendingGet.callback().complete(new Error<>(err));
					}

					final var pendingCGet = this.pendingCGets.remove(transactionId);
					if (pendingCGet != null) {
						handled = true;
						pendingCGet.callback().complete(new Error<>(err));
					}

					final var pendingPGet = this.pendingPGets.remove(transactionId);
					if (pendingPGet != null) {
						handled = true;
						pendingPGet.callback().complete(new Error<>(err));
					}

					final var pendingDelete = this.pendingDeletes.remove(transactionId);
					if (pendingDelete != null) {
						handled = true;
						pendingDelete.callback().complete(new Error<>(err));
					}

					final var pendingPDelete = this.pendingPDeletes.remove(transactionId);
					if (pendingPDelete != null) {
						handled = true;
						pendingPDelete.callback().complete(new Error<>(err));
					}

					final var pendingLs = this.pendingLss.remove(transactionId);
					if (pendingLs != null) {
						handled = true;
						pendingLs.callback().complete(new Error<>(err));
					}

					if (!handled) {
						WorterbuchClientImpl.log.error("Received error message from server: '{}'", err);
						this.onError.accept(new WorterbuchException(new WorterbuchError(err)));
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
					final var onWelcome = this.onWelcome.getAndSet(null);
					if (onWelcome != null) {
						onWelcome.accept(welcome);
					}
				} catch (final JsonProcessingException e) {
					this.onError.accept(new WorterbuchException("error deserializing welcome message", e));
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
				this.deliverPendingGet(pendingGet, valueContainer, typeFactory);

				final var pendingDelete = this.pendingDeletes.remove(transactionId);
				this.deliverPendingDelete(pendingDelete, deletedContainer, typeFactory);

				final var subscription = this.subscriptions.get(transactionId);
				this.deliverSubscription(subscription, valueContainer, deletedContainer, typeFactory);

				return;
			}

			final var cStateContainer = parent.get("cState");
			if (cStateContainer != null) {
				final var transactionId = cStateContainer.get("transactionId").asLong();

				final var valueContainer = cStateContainer.get("value");
				final var versionContainer = cStateContainer.get("version");

				final var typeFactory = this.objectMapper.getTypeFactory();

				final var pendingCGet = this.pendingCGets.remove(transactionId);
				this.deliverPendingCGet(pendingCGet, valueContainer, versionContainer, typeFactory);

				return;
			}

			final var pStateContainer = parent.get("pState");
			if (pStateContainer != null) {
				final var transactionId = pStateContainer.get("transactionId").asLong();

				final var kvpsContainer = pStateContainer.get("keyValuePairs");
				final var deletedContainer = pStateContainer.get("deleted");

				final var typeFactory = this.objectMapper.getTypeFactory();

				final var pendingPGet = this.pendingPGets.remove(transactionId);
				this.deliverPendingPGet(pendingPGet, kvpsContainer, typeFactory);

				final var pendingPDelete = this.pendingPDeletes.remove(transactionId);
				this.deliverPendingPDelete(pendingPDelete, deletedContainer, typeFactory);

				final var pSubscription = this.pSubscriptions.get(transactionId);
				this.deliverPSubscription(pSubscription, kvpsContainer, deletedContainer, typeFactory);

				return;
			}

			final var lsContainer = parent.get("lsState");
			if (lsContainer != null) {
				final var transactionId = lsContainer.get("transactionId").asLong();

				final var pendingLs = this.pendingLss.remove(transactionId);
				final var lsSubscription = this.lsSubscriptions.get(transactionId);

				final var childrenContainer = lsContainer.get("children");

				try {
					final var children = this.objectMapper.readValue(childrenContainer.toString(),
							new TypeReference<List<String>>() {
							});

					if (pendingLs != null) {
						pendingLs.callback().complete(new Ok<>(children));
					}

					if (lsSubscription != null) {
						lsSubscription.callback().accept(children);
					}
				} catch (final JsonProcessingException e) {

					WorterbuchClientImpl.log.error("Could not deserialize JSON '{}': {}", childrenContainer,
							e.getMessage());

					if (pendingLs != null) {
						pendingLs.callback()
								.completeExceptionally(new WorterbuchException("server sent incomplete response"));
					}

					if (lsSubscription != null) {
						lsSubscription.callback().accept(Collections.emptyList());
					}
				}

				return;
			}

		} catch (final JsonProcessingException e) {
			WorterbuchClientImpl.log.error("Received invalid JSON message: '{}'", message);
		}
	}

	private <T> void deliverPendingGet(final PendingGet<T> pendingGet, final JsonNode valueContainer,
			final TypeFactory typeFactory) {
		if (pendingGet != null) {
			if (valueContainer != null) {
				final var kvpType = typeFactory.constructType(pendingGet.type());
				try {
					final var value = this.objectMapper.<T>readValue(valueContainer.toString(), kvpType);
					pendingGet.callback().complete(new Ok<>(value));
				} catch (final JsonProcessingException e) {
					WorterbuchClientImpl.log.error("Could not deserialize JSON '{}': {}", valueContainer,
							e.getMessage());
					pendingGet.callback().completeExceptionally(e);
				}
			} else {
				pendingGet.callback().completeExceptionally(new WorterbuchException("server sent incomplete response"));
			}
		}
	}

	private <T> void deliverPendingCGet(final PendingCGet<T> pendingCGet, final JsonNode valueContainer,
			final JsonNode versionContainer, final TypeFactory typeFactory) {
		if ((pendingCGet != null) && (versionContainer != null)) {
			if (valueContainer != null) {
				final var kvpType = typeFactory.constructType(pendingCGet.type());
				try {
					final var value = this.objectMapper.<T>readValue(valueContainer.toString(), kvpType);
					final var version = this.objectMapper.readValue(versionContainer.toString(), Long.class);
					pendingCGet.callback().complete(new Ok<>(new Tuple<>(value, version)));
				} catch (final JsonProcessingException e) {
					WorterbuchClientImpl.log.error("Could not deserialize JSON '{}': {}", valueContainer,
							e.getMessage());
					pendingCGet.callback().completeExceptionally(e);
				}
			} else {
				pendingCGet.callback()
						.completeExceptionally(new WorterbuchException("server sent incomplete response"));
			}
		}
	}

	private <T> void deliverPendingDelete(final PendingDelete<T> pendingDelete, final JsonNode deletedContainer,
			final TypeFactory typeFactory) {
		if (pendingDelete != null) {
			if (deletedContainer != null) {
				final var kvpType = typeFactory.constructType(pendingDelete.type());
				try {
					final var value = this.objectMapper.<T>readValue(deletedContainer.toString(), kvpType);
					pendingDelete.callback().complete(new Ok<>(value));
				} catch (final JsonProcessingException e) {
					WorterbuchClientImpl.log.error("Could not deserialize JSON '{}': {}", deletedContainer,
							e.getMessage());
					pendingDelete.callback().completeExceptionally(e);
				}
			} else {
				pendingDelete.callback()
						.completeExceptionally(new WorterbuchException("server sent incomplete response"));
			}
		}
	}

	private <T> void deliverSubscription(final Subscription<T> subscription, final JsonNode valueContainer,
			final JsonNode deletedContainer, final TypeFactory typeFactory) {
		if (subscription != null) {
			final var kvpType = typeFactory.constructType(subscription.type());
			if (valueContainer != null) {
				try {
					final var value = this.objectMapper.<T>readValue(valueContainer.toString(), kvpType);
					subscription.callback().accept(Optional.ofNullable(value));
				} catch (final JsonProcessingException e) {
					WorterbuchClientImpl.log.error("Could not deserialize JSON '{}': {}", valueContainer,
							e.getMessage());
					subscription.callback().accept(Optional.empty());
				}
			} else {
				subscription.callback().accept(Optional.empty());
			}
		}
	}

	private <T> void deliverPendingPGet(final PendingPGet<T> pendingPGet, final JsonNode kvpsContainer,
			final TypeFactory typeFactory) {
		if (pendingPGet != null) {
			if (kvpsContainer != null) {
				final var valueType = typeFactory.constructParametricType(TypedKeyValuePair.class,
						typeFactory.constructType(pendingPGet.type()));
				final var kvpsType = typeFactory.constructParametricType(List.class, valueType);
				try {
					final List<TypedKeyValuePair<T>> kvps = this.objectMapper.readValue(kvpsContainer.toString(),
							kvpsType);
					pendingPGet.callback().complete(new Ok<>(kvps));
				} catch (final JsonProcessingException e) {
					WorterbuchClientImpl.log.error("Could not deserialize JSON '{}': {}", kvpsContainer,
							e.getMessage());
					pendingPGet.callback().completeExceptionally(e);
				}
			} else {
				pendingPGet.callback()
						.completeExceptionally(new WorterbuchException("server sent incomplete response"));
			}
		}
	}

	private <T> void deliverPendingPDelete(final PendingPDelete<T> pendingPDelete, final JsonNode deletedContainer,
			final TypeFactory typeFactory) {
		if (pendingPDelete != null) {
			if (deletedContainer != null) {
				final var valueType = typeFactory.constructParametricType(TypedKeyValuePair.class,
						typeFactory.constructType(pendingPDelete.type()));
				final var kvpsType = typeFactory.constructParametricType(List.class, valueType);
				try {
					final List<TypedKeyValuePair<T>> kvps = this.objectMapper.readValue(deletedContainer.toString(),
							kvpsType);
					pendingPDelete.callback().complete(new Ok<>(kvps));
				} catch (final JsonProcessingException e) {
					WorterbuchClientImpl.log.error("Could not deserialize JSON '{}': {}", deletedContainer,
							e.getMessage());
					pendingPDelete.callback().completeExceptionally(e);
				}
			} else {
				pendingPDelete.callback()
						.completeExceptionally(new WorterbuchException("server sent incomplete response"));
			}
		}
	}

	private <T> void deliverPSubscription(final PSubscription<T> pSubscription, final JsonNode kvpsContainer,
			final JsonNode deletedContainer, final TypeFactory typeFactory) {
		if (pSubscription != null) {
			final var valueType = typeFactory.constructParametricType(TypedKeyValuePair.class,
					typeFactory.constructType(pSubscription.type()));
			final var kvpsType = typeFactory.constructParametricType(List.class, valueType);
			if (kvpsContainer != null) {
				try {
					final List<TypedKeyValuePair<T>> kvps = this.objectMapper.readValue(kvpsContainer.toString(),
							kvpsType);
					final var event = new TypedPStateEvent<>(kvps, null);
					pSubscription.callback().accept(event);
				} catch (final JsonProcessingException e) {
					WorterbuchClientImpl.log.error("Could not deserialize JSON '{}': {}", kvpsContainer,
							e.getMessage());
				}
			} else if (deletedContainer != null) {
				try {
					final List<TypedKeyValuePair<T>> kvps = this.objectMapper.readValue(deletedContainer.toString(),
							kvpsType);
					final var event = new TypedPStateEvent<>(null, kvps);
					pSubscription.callback().accept(event);
				} catch (final JsonProcessingException e) {
					WorterbuchClientImpl.log.error("Could not deserialize JSON '{}': {}", deletedContainer,
							e.getMessage());
				}
			}
		}
	}
}
