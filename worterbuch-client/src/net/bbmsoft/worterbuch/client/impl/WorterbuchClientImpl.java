package net.bbmsoft.worterbuch.client.impl;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
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
import net.bbmsoft.worterbuch.client.api.WorterbuchException;
import net.bbmsoft.worterbuch.client.api.util.Tuple;
import net.bbmsoft.worterbuch.client.error.Error;
import net.bbmsoft.worterbuch.client.error.Future;
import net.bbmsoft.worterbuch.client.error.Ok;
import net.bbmsoft.worterbuch.client.error.Result;
import net.bbmsoft.worterbuch.client.error.WorterbuchError;
import net.bbmsoft.worterbuch.client.model.Err;
import net.bbmsoft.worterbuch.client.model.KeyValuePair;
import net.bbmsoft.worterbuch.client.model.Welcome;
import net.bbmsoft.worterbuch.client.pending.LsSubscription;
import net.bbmsoft.worterbuch.client.pending.PSubscription;
import net.bbmsoft.worterbuch.client.pending.PendingAck;
import net.bbmsoft.worterbuch.client.pending.PendingCGet;
import net.bbmsoft.worterbuch.client.pending.PendingDelete;
import net.bbmsoft.worterbuch.client.pending.PendingGet;
import net.bbmsoft.worterbuch.client.pending.PendingLs;
import net.bbmsoft.worterbuch.client.pending.PendingPDelete;
import net.bbmsoft.worterbuch.client.pending.PendingPGet;
import net.bbmsoft.worterbuch.client.pending.Subscription;

public class WorterbuchClientImpl implements WorterbuchClient {

	private static final Logger log = LoggerFactory.getLogger(WorterbuchClientImpl.class);

	private final ClientSocket client;
	private final Consumer<WorterbuchException> onError;
	private final AtomicLong transactionId;
	private final ObjectMapper objectMapper;
	private final MessageSerDe messageSerde;
	private final MessageSender messageSender;

	private final AtomicReference<Consumer<Welcome>> onWelcome;

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
	public <T> Future<Void> set(final String key, final T value) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Result<Void>>();
		final var msg = MessageBuilder.setMessage(tid, key, value);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingAcks.put(tid, new PendingAck(msg, fut));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public <T> Future<Void> publish(final String key, final T value) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Result<Void>>();
		final var msg = MessageBuilder.publishMessage(tid, key, value);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingAcks.put(tid, new PendingAck(msg, fut));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public <T> Future<Void> initPubStream(final String key) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Result<Void>>();
		final var msg = MessageBuilder.sPubInitMessage(tid, key);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingAcks.put(tid, new PendingAck(msg, fut));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public <T> Future<Void> streamPub(final long transactionId, final T value) {
		final var fut = new CompletableFuture<Result<Void>>();
		final var msg = MessageBuilder.sPubMessage(transactionId, value);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingAcks.put(transactionId, new PendingAck(msg, fut));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, transactionId);
	}

	@Override
	public <T> Future<T> get(final String key, final Type type) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Result<T>>();
		final var msg = MessageBuilder.getMessage(tid, key);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingGets.put(tid, new PendingGet<>(msg, fut, type));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public <T> Future<List<TypedKeyValuePair<T>>> pGet(final String pattern, final Type type) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Result<List<TypedKeyValuePair<T>>>>();
		final var msg = MessageBuilder.pGetMessage(tid, pattern);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingPGets.put(tid, new PendingPGet<>(msg, fut, type));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public <T> Future<T> delete(final String key, final Type type) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Result<T>>();
		final var msg = MessageBuilder.deleteMessage(tid, key);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingDeletes.put(tid, new PendingDelete<>(msg, fut, type));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public Future<Void> delete(final String key) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Result<Void>>();
		final var msg = MessageBuilder.deleteMessage(tid, key);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingDeletes.put(tid, new PendingDelete<>(msg, fut, null));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public <T> Future<List<TypedKeyValuePair<T>>> pDelete(final String pattern, final Type type) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Result<List<TypedKeyValuePair<T>>>>();
		final var msg = MessageBuilder.pDeleteMessage(tid, pattern, false);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingPDeletes.put(tid, new PendingPDelete<>(msg, fut, type));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public Future<Void> pDelete(final String pattern) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Result<Void>>();
		final var msg = MessageBuilder.pDeleteMessage(tid, pattern, true);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingAcks.put(tid, new PendingAck(msg, fut));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public Future<List<String>> ls(final String parent) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Result<List<String>>>();
		final var msg = MessageBuilder.lsMessage(tid, parent);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingLss.put(tid, new PendingLs(msg, fut));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public Future<List<String>> pLs(final String parentPattern) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Result<List<String>>>();
		final var msg = MessageBuilder.pLsMessage(tid, parentPattern);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingLss.put(tid, new PendingLs(msg, fut));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public <T> Future<Void> subscribe(final String key, final boolean unique, final boolean liveOnly, final Type type,
			final Consumer<Optional<T>> callback) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Result<Void>>();
		final var msg = MessageBuilder.subscribeMessage(tid, key, unique, liveOnly);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingAcks.put(tid, new PendingAck(msg, fut));
		this.subscriptions.put(tid, new Subscription<>(msg, callback, type));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public <T> Future<Void> pSubscribe(final String pattern, final boolean unique, final boolean liveOnly,
			final Optional<Long> aggregateEvents, final Type type, final Consumer<TypedPStateEvent<T>> callback) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Result<Void>>();
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
		final var fut = new CompletableFuture<Result<Void>>();
		final var msg = MessageBuilder.unsubscribeMessage(tid);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingAcks.put(tid, new PendingAck(msg, fut));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public Future<Void> subscribeLs(final String parent, final Consumer<List<String>> callback) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Result<Void>>();
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
		final var fut = new CompletableFuture<Result<Void>>();
		final var msg = MessageBuilder.unsubscribeLsMessage(tid);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingAcks.put(tid, new PendingAck(msg, fut));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public <T> Future<Void> cSet(final String key, final T value, final long version) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Result<Void>>();
		final var msg = MessageBuilder.cSetMessage(tid, key, value, version);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingAcks.put(tid, new PendingAck(msg, fut));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public <T> Future<Tuple<T, Long>> cGet(final String key, final Type type) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Result<Tuple<T, Long>>>();
		final var msg = MessageBuilder.cGetMessage(tid, key);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingCGets.put(tid, new PendingCGet<>(msg, fut, type));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public <T, V> boolean update(final String key, final Function<Optional<T>, V> transform, final Type type) {
		try {
			return this.tryUpdate(key, transform, type, 0);
		} catch (WorterbuchError | InterruptedException | ExecutionException | TimeoutException e) {
			this.onError.accept(new WorterbuchException("Error while trying to update compare-and-swap value", e));
			return false;
		}
	}

	@Override
	public Future<Void> lock(final String key) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Result<Void>>();
		final var msg = MessageBuilder.lockMessage(tid, key);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingAcks.put(tid, new PendingAck(msg, fut));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public Future<Void> acquireLock(final String key) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Result<Void>>();
		final var msg = MessageBuilder.acquireLockMessage(tid, key);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingAcks.put(tid, new PendingAck(msg, fut));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public Future<Void> releaseLock(final String key) {
		final var tid = this.acquireTid();
		final var fut = new CompletableFuture<Result<Void>>();
		final var msg = MessageBuilder.releaseLockMessage(tid, key);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingAcks.put(tid, new PendingAck(msg, fut));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	@Override
	public Future<List<String>> getGraveGoods() {
		return this.getList("$SYS/clients/" + this.getClientId() + "/graveGoods", String.class);
	}

	@Override
	public Future<List<KeyValuePair>> getLastWill() {
		return this.getList("$SYS/clients/" + this.getClientId() + "/lastWill", KeyValuePair.class);
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
		this.updateList("$SYS/clients/" + this.getClientId() + "/graveGoods", update, String.class);
	}

	@Override
	public void updateLastWill(final Consumer<List<KeyValuePair>> update) {
		this.updateList("$SYS/clients/" + this.getClientId() + "/lastWill", update, KeyValuePair.class);
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

	private <T, V> boolean tryUpdate(final String key, final Function<Optional<T>, V> transform, final Type type,
			final int counter) throws WorterbuchError, InterruptedException, ExecutionException, TimeoutException {

		if (counter > 100) {
			WorterbuchClientImpl.log.error("Unsuccessfully tried to update {} 100 times, giving up.", key);
			return false;
		}

		// TODO cache value so we don't have to get it from the server every time

		final var get = this.<T>cGet(key, type).result().get(5, TimeUnit.SECONDS);

		if (get instanceof final Ok<Tuple<T, Long>> ok) {

			// value exists

			final var value = Optional.ofNullable(ok.get().first());
			final var version = ok.get().second();
			final var newValue = transform.apply(value);

			final var set = this.cSet(key, newValue, version).result().get(5, TimeUnit.SECONDS);

			return this.evalUpdate(set, key, transform, type, counter);

		} else if (get instanceof final Error error) {

			if (error.err().getErrorCode() == ErrorCode.NoSuchValue) {

				// value does not exist

				final var value = Optional.<T>empty();
				final var version = 0;
				final var newValue = transform.apply(value);

				final var set = this.cSet(key, newValue, version).result().get(5, TimeUnit.SECONDS);

				return this.evalUpdate(set, key, transform, type, counter);
			}

			throw new WorterbuchError(error.err());

		} else {
			throw new IllegalStateException();
		}

	}

	private <T, V> boolean evalUpdate(final Result<Void> set, final String key,
			final Function<Optional<T>, V> transform, final Type type, final int counter)
			throws WorterbuchError, InterruptedException, ExecutionException, TimeoutException {

		if (set instanceof Ok) {

			// set succeeded

			return true;

		} else if (set instanceof final Error error) {

			if (error.err().getErrorCode() == ErrorCode.CasVersionMismatch) {

				// value was updated in the meantime

				return this.tryUpdate(key, transform, type, counter + 1);

			}

			throw new WorterbuchError(error.err());

		} else {
			throw new IllegalStateException();
		}
	}

	public Future<Void> switchProtocol(final int protoVersion) {
		final var tid = 0L;
		final var fut = new CompletableFuture<Result<Void>>();
		final var msg = MessageBuilder.switchProtocolMessage(protoVersion);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingAcks.put(tid, new PendingAck(msg, fut));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	public Future<Void> authorize(final String authToken) {
		final var tid = 0L;
		final var fut = new CompletableFuture<Result<Void>>();
		final var msg = MessageBuilder.authorizationMessage(authToken);
		final var json = this.messageSerde.serializeMessage(msg);
		this.pendingAcks.put(tid, new PendingAck(msg, fut));
		this.messageSender.sendMessage(json);
		return new Future<>(fut, tid);
	}

	public void start(final Consumer<Welcome> onWelcome) {
		this.onWelcome.set(onWelcome);
	}

	public void messageReceived(final String message, final Executor callbackExecutor) {

		JsonNode parent;

		try {
			parent = this.objectMapper.readTree(message);

			final var ackContainer = parent.get("ack");
			if (ackContainer != null) {

				final var transactionId = ackContainer.get("transactionId").asLong();

				final var pending = this.pendingAcks.remove(transactionId);
				if (pending != null) {
					callbackExecutor.execute(() -> pending.callback().complete(new Ok<>(null)));
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
						callbackExecutor.execute(() -> pendingAck.callback().complete(new Error<>(err)));
					}

					final var pendingGet = this.pendingGets.remove(transactionId);
					if (pendingGet != null) {
						handled = true;
						callbackExecutor.execute(() -> pendingGet.callback().complete(new Error<>(err)));
					}

					final var pendingCGet = this.pendingCGets.remove(transactionId);
					if (pendingCGet != null) {
						handled = true;
						callbackExecutor.execute(() -> pendingCGet.callback().complete(new Error<>(err)));
					}

					final var pendingPGet = this.pendingPGets.remove(transactionId);
					if (pendingPGet != null) {
						handled = true;
						callbackExecutor.execute(() -> pendingPGet.callback().complete(new Error<>(err)));
					}

					final var pendingDelete = this.pendingDeletes.remove(transactionId);
					if (pendingDelete != null) {
						handled = true;
						callbackExecutor.execute(() -> pendingDelete.callback().complete(new Error<>(err)));
					}

					final var pendingPDelete = this.pendingPDeletes.remove(transactionId);
					if (pendingPDelete != null) {
						handled = true;
						callbackExecutor.execute(() -> pendingPDelete.callback().complete(new Error<>(err)));
					}

					final var pendingLs = this.pendingLss.remove(transactionId);
					if (pendingLs != null) {
						handled = true;
						callbackExecutor.execute(() -> pendingLs.callback().complete(new Error<>(err)));
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
						callbackExecutor.execute(() -> onWelcome.accept(welcome));
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

				final var pendingLs = this.pendingLss.remove(transactionId);
				final var lsSubscription = this.lsSubscriptions.get(transactionId);

				final var childrenContainer = lsContainer.get("children");

				try {
					final var children = this.objectMapper.readValue(childrenContainer.toString(),
							new TypeReference<List<String>>() {
							});

					if (pendingLs != null) {
						callbackExecutor.execute(() -> pendingLs.callback().complete(new Ok<>(children)));
					}

					if (lsSubscription != null) {
						callbackExecutor.execute(() -> lsSubscription.callback().accept(children));
					}
				} catch (final JsonProcessingException e) {

					WorterbuchClientImpl.log.error("Could not deserialize JSON '{}': {}", childrenContainer,
							e.getMessage());

					if (pendingLs != null) {
						callbackExecutor.execute(() -> pendingLs.callback()
								.completeExceptionally(new WorterbuchException("server sent incomplete response")));
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

	private <T> void deliverPendingGet(final PendingGet<T> pendingGet, final JsonNode valueContainer,
			final TypeFactory typeFactory, final Executor callbackExecutor) {
		if (pendingGet != null) {
			if (valueContainer != null) {
				final var kvpType = typeFactory.constructType(pendingGet.type());
				try {
					final var value = this.objectMapper.<T>readValue(valueContainer.toString(), kvpType);
					callbackExecutor.execute(() -> pendingGet.callback().complete(new Ok<>(value)));
				} catch (final JsonProcessingException e) {
					WorterbuchClientImpl.log.error("Could not deserialize JSON '{}': {}", valueContainer,
							e.getMessage());
					callbackExecutor.execute(() -> pendingGet.callback().completeExceptionally(e));
				}
			} else {
				callbackExecutor.execute(() -> pendingGet.callback()
						.completeExceptionally(new WorterbuchException("server sent incomplete response")));
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
					callbackExecutor
							.execute(() -> pendingCGet.callback().complete(new Ok<>(new Tuple<>(value, version))));
				} catch (final JsonProcessingException e) {
					WorterbuchClientImpl.log.error("Could not deserialize JSON '{}': {}", valueContainer,
							e.getMessage());
					callbackExecutor.execute(() -> pendingCGet.callback().completeExceptionally(e));
				}
			} else {
				callbackExecutor.execute(() -> pendingCGet.callback()
						.completeExceptionally(new WorterbuchException("server sent incomplete response")));
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
					callbackExecutor.execute(() -> pendingDelete.callback().complete(new Ok<>(value)));
				} catch (final JsonProcessingException e) {
					WorterbuchClientImpl.log.error("Could not deserialize JSON '{}': {}", deletedContainer,
							e.getMessage());
					callbackExecutor.execute(() -> pendingDelete.callback().completeExceptionally(e));
				}
			} else {
				callbackExecutor.execute(() -> pendingDelete.callback()
						.completeExceptionally(new WorterbuchException("server sent incomplete response")));
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
					callbackExecutor.execute(() -> pendingPGet.callback().complete(new Ok<>(kvps)));
				} catch (final JsonProcessingException e) {
					WorterbuchClientImpl.log.error("Could not deserialize JSON '{}': {}", kvpsContainer,
							e.getMessage());
					callbackExecutor.execute(() -> pendingPGet.callback().completeExceptionally(e));
				}
			} else {
				callbackExecutor.execute(() -> pendingPGet.callback()
						.completeExceptionally(new WorterbuchException("server sent incomplete response")));
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
					callbackExecutor.execute(() -> pendingPDelete.callback().complete(new Ok<>(kvps)));
				} catch (final JsonProcessingException e) {
					WorterbuchClientImpl.log.error("Could not deserialize JSON '{}': {}", deletedContainer,
							e.getMessage());
					callbackExecutor.execute(() -> pendingPDelete.callback().completeExceptionally(e));
				}
			} else {
				callbackExecutor.execute(() -> pendingPDelete.callback()
						.completeExceptionally(new WorterbuchException("server sent incomplete response")));
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
				}
			}
		}
	}
}
