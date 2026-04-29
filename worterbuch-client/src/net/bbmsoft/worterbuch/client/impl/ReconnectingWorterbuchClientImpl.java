package net.bbmsoft.worterbuch.client.impl;

import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bbmsoft.worterbuch.client.api.TypedKeyValuePair;
import net.bbmsoft.worterbuch.client.api.TypedPStateEvent;
import net.bbmsoft.worterbuch.client.api.WorterbuchClient;
import net.bbmsoft.worterbuch.client.api.util.Tuple;
import net.bbmsoft.worterbuch.client.error.ConnectionFailed;
import net.bbmsoft.worterbuch.client.error.WorterbuchException;
import net.bbmsoft.worterbuch.client.model.Err;
import net.bbmsoft.worterbuch.client.model.KeyValuePair;
import net.bbmsoft.worterbuch.client.response.Future;

public class ReconnectingWorterbuchClientImpl implements WorterbuchClient {

	private static final int MAX_BACKOFF = 5_000;
	private static final long ALREADY_CLOSED = Integer.MAX_VALUE;

	private final Logger log;
	private final Iterable<URI> uris;
	private final ScheduledExecutorService executor;
	private final Optional<String> authToken;
	private final Consumer<WorterbuchException> onError;

	private State state;
	private Iterator<URI> urisIterator;
	private int backoff;

	public ReconnectingWorterbuchClientImpl(final Iterable<URI> uris, final Optional<String> authToken,
			final Consumer<WorterbuchException> onError) {
		this.authToken = authToken;
		this.onError = onError;
		this.log = LoggerFactory.getLogger(this.getClass());
		this.uris = uris;
		this.executor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "worterbuch-client-executor"));
		this.state = new State.Init();
	}

	// ###### Reconnect state machine implementation ######

	static sealed interface State permits State.Init, State.Disconnected, State.ReconnectScheduled,
			State.AttemptingReconnect, State.Connecting, State.ConnectionFailed, State.Connected, State.Closed {

		static record Init() implements State {
		}

		static record Disconnected() implements State {
		}

		static record ReconnectScheduled() implements State {
		}

		static record AttemptingReconnect() implements State {
		}

		static record Connecting(URI uri) implements State {
		}

		static record ConnectionFailed(URI uri) implements State {
		}

		@SuppressFBWarnings({ "EI_EXPOSE_REP", "EI_EXPOSE_REP2" })
		static record Connected(URI uri, WorterbuchClient client) implements State {
		}

		static record Closed() implements State {
		}
	}

	public void start() {
		this.executor.execute(this::initialConnect);
	}

	private void initialConnect() {
		if (this.isClosed()) {
			return;
		}

		if (!(this.state instanceof State.Init)) {
			throw new IllegalStateException("Expected state " + new State.Init() + " but got " + this.state);
		}

		this.state = new State.ReconnectScheduled();
		this.reconnect();
	}

	private void reconnect() {
		if (this.isClosed()) {
			return;
		}

		if (!(this.state instanceof State.ReconnectScheduled)) {
			throw new IllegalStateException(
					"Expected state " + new State.ReconnectScheduled() + " but got " + this.state);
		}

		this.log.info("Attempting to (re-)connect.");

		this.state = new State.AttemptingReconnect();
		this.urisIterator = this.uris.iterator();

		this.tryNextUri(null);
	}

	private void tryNextUri(final URI previousUri) {
		if (this.isClosed()) {
			return;
		}

		if (!((this.state instanceof State.AttemptingReconnect) && (previousUri == null))
				&& !(this.state instanceof final State.ConnectionFailed failed && failed.uri.equals(previousUri))) {
			throw new IllegalStateException("Expected state " + new State.AttemptingReconnect() + " or "
					+ new State.ConnectionFailed(previousUri) + " but got " + this.state);
		}

		if (this.urisIterator.hasNext()) {
			final var next = this.urisIterator.next();
			this.state = new State.AttemptingReconnect();
			this.tryConnect(next);
		} else {
			this.reconnectFailed();
		}
	}

	private void tryConnect(final URI uri) {
		if (this.isClosed()) {
			return;
		}

		if (!(this.state instanceof State.AttemptingReconnect)) {
			throw new IllegalStateException(
					"Expected state " + new State.AttemptingReconnect() + " but got " + this.state);
		}

		this.log.info("Trying to connect to {} …", uri);

		this.state = new State.Connecting(uri);

		final BiConsumer<Integer, String> onDisconnect = (i, s) -> this.executor
				.execute(() -> this.disconnected(i, s, uri));

		try {
			final var client = new Connector(List.of(uri), this.authToken, onDisconnect, this.onError).connect();
			this.connected(uri, client);
		} catch (final Throwable th) {
			this.connectionFailed(uri);
		}
	}

	private void disconnected(final Integer i, final String s, final URI uri) {
		if (this.isClosed()) {
			return;
		}

		if (!(this.state instanceof final State.Connected connected && connected.uri.equals(uri))) {
			throw new IllegalStateException(
					"Expected state " + new State.Connected(uri, null) + " but got " + this.state);
		}

		this.noConnection(String.format("Connection to %s lost", uri.toString()));
	}

	private void connected(final URI uri, final WorterbuchClient client) {
		if (this.isClosed()) {
			return;
		}

		if (!(this.state instanceof final State.Connecting connecting) || !connecting.uri.equals(uri)) {
			throw new IllegalStateException("Expected state " + new State.Connecting(uri) + " but got " + this.state);
		}

		this.log.info("Connected to {}", uri);

		this.state = new State.Connected(uri, client);

		// TODO re-insert failed commands into queue
		// TODO apply any pending commands
		// TODO re-establish subscriptions
		// TODO try to re-acquire held locks
		// TODO re-register last will and grave goods

		this.resetBackoff();
	}

	private void connectionFailed(final URI uri) {
		if (this.isClosed()) {
			return;
		}

		if (!(this.state instanceof final State.Connecting connecting) || !connecting.uri.equals(uri)) {
			throw new IllegalStateException("Expected state " + new State.Connecting(uri) + " but got " + this.state);
		}

		this.log.info("Could not connect to {}", uri);

		this.state = new State.ConnectionFailed(uri);

		this.tryNextUri(uri);
	}

	private void reconnectFailed() {
		if (this.isClosed()) {
			return;
		}

		if (!(this.state instanceof State.ConnectionFailed)) {
			throw new IllegalStateException(
					"Expected state " + ConnectionFailed.class.getSimpleName() + " but got " + this.state);
		}

		this.noConnection("Could not connect to any server");
	}

	private void noConnection(final String message) {
		if (this.isClosed()) {
			return;
		}

		this.log.info("{}. Trying again in {} seconds …", message, String.format("%.1f", this.backoff / 1000.0));

		this.state = new State.ReconnectScheduled();
		this.executor.schedule(this::reconnect, this.backoff, TimeUnit.MILLISECONDS);
		this.increaseBackoff();
	}

	private void increaseBackoff() {
		if (this.backoff == 0) {
			this.backoff = 100;
		} else {
			this.backoff = Math.min(this.backoff * 2, ReconnectingWorterbuchClientImpl.MAX_BACKOFF);
		}
	}

	private void resetBackoff() {
		this.backoff = 0;
	}

	private boolean isClosed() {
		return this.state instanceof State.Closed;
	}

	// ###### Worterbuch client implementation ######

	@Override
	public void close() {
		this.executor.execute(this::doClose);
		this.executor.shutdown();
	}

	@Override
	public Future<Void> set(final String key, final Object value) {
		if (this.isClosed()) {
			return this.alreadyClosed();
		}

		// TODO Auto-generated method stub
		return this.alreadyClosed();
	}

	@Override
	public Future<Void> publish(final String key, final Object value) {
		if (this.isClosed()) {
			return this.alreadyClosed();
		}

		// TODO Auto-generated method stub
		return this.alreadyClosed();
	}

	@Override
	public Future<Void> initPubStream(final String key) {
		if (this.isClosed()) {
			return this.alreadyClosed();
		}

		// TODO Auto-generated method stub
		return this.alreadyClosed();
	}

	@Override
	public Future<Void> streamPub(final long transactionId, final Object value) {
		if (this.isClosed()) {
			return this.alreadyClosed();
		}

		// TODO Auto-generated method stub
		return this.alreadyClosed();
	}

	@Override
	public <T> Future<T> get(final String key, final Class<T> type) {
		if (this.isClosed()) {
			return this.alreadyClosed();
		}

		// TODO Auto-generated method stub
		return this.alreadyClosed();
	}

	@Override
	public <T> Future<T> get(final String key, final TypeReference<T> type) {
		if (this.isClosed()) {
			return this.alreadyClosed();
		}

		// TODO Auto-generated method stub
		return this.alreadyClosed();
	}

	@Override
	public <T> Future<List<TypedKeyValuePair<T>>> pGet(final String pattern, final Class<T> type) {
		if (this.isClosed()) {
			return this.alreadyClosed();
		}

		// TODO Auto-generated method stub
		return this.alreadyClosed();
	}

	@Override
	public <T> Future<List<TypedKeyValuePair<T>>> pGet(final String pattern, final TypeReference<T> type) {
		if (this.isClosed()) {
			return this.alreadyClosed();
		}

		// TODO Auto-generated method stub
		return this.alreadyClosed();
	}

	@Override
	public <T> Future<T> delete(final String key, final Class<T> type) {
		if (this.isClosed()) {
			return this.alreadyClosed();
		}

		// TODO Auto-generated method stub
		return this.alreadyClosed();
	}

	@Override
	public <T> Future<T> delete(final String key, final TypeReference<T> type) {
		if (this.isClosed()) {
			return this.alreadyClosed();
		}

		// TODO Auto-generated method stub
		return this.alreadyClosed();
	}

	@Override
	public Future<Void> delete(final String key) {
		if (this.isClosed()) {
			return this.alreadyClosed();
		}

		// TODO Auto-generated method stub
		return this.alreadyClosed();
	}

	@Override
	public <T> Future<List<TypedKeyValuePair<T>>> pDelete(final String pattern, final Class<T> type) {
		if (this.isClosed()) {
			return this.alreadyClosed();
		}

		// TODO Auto-generated method stub
		return this.alreadyClosed();
	}

	@Override
	public <T> Future<List<TypedKeyValuePair<T>>> pDelete(final String pattern, final TypeReference<T> type) {
		if (this.isClosed()) {
			return this.alreadyClosed();
		}

		// TODO Auto-generated method stub
		return this.alreadyClosed();
	}

	@Override
	public Future<Void> pDelete(final String pattern) {
		if (this.isClosed()) {
			return this.alreadyClosed();
		}

		// TODO Auto-generated method stub
		return this.alreadyClosed();
	}

	@Override
	public Future<List<String>> ls(final String parent) {
		if (this.isClosed()) {
			return this.alreadyClosed();
		}

		// TODO Auto-generated method stub
		return this.alreadyClosed();
	}

	@Override
	public Future<List<String>> pLs(final String parentPattern) {
		if (this.isClosed()) {
			return this.alreadyClosed();
		}

		// TODO Auto-generated method stub
		return this.alreadyClosed();
	}

	@Override
	public <T> Future<Void> subscribe(final String key, final boolean unique, final boolean liveOnly,
			final Class<T> type, final Consumer<Optional<T>> callback) {
		if (this.isClosed()) {
			return this.alreadyClosed();
		}

		// TODO Auto-generated method stub
		return this.alreadyClosed();
	}

	@Override
	public <T> Future<Void> subscribe(final String key, final boolean unique, final boolean liveOnly,
			final TypeReference<T> type, final Consumer<Optional<T>> callback) {
		if (this.isClosed()) {
			return this.alreadyClosed();
		}

		// TODO Auto-generated method stub
		return this.alreadyClosed();
	}

	@Override
	public <T> Future<Void> pSubscribe(final String pattern, final boolean unique, final boolean liveOnly,
			final Optional<Long> aggregateEvents, final Class<T> type, final Consumer<TypedPStateEvent<T>> callback) {
		if (this.isClosed()) {
			return this.alreadyClosed();
		}

		// TODO Auto-generated method stub
		return this.alreadyClosed();
	}

	@Override
	public <T> Future<Void> pSubscribe(final String pattern, final boolean unique, final boolean liveOnly,
			final Optional<Long> aggregateEvents, final TypeReference<T> type,
			final Consumer<TypedPStateEvent<T>> callback) {
		if (this.isClosed()) {
			return this.alreadyClosed();
		}

		// TODO Auto-generated method stub
		return this.alreadyClosed();
	}

	@Override
	public Future<Void> unsubscribe(final long transactionId) {
		if (this.isClosed()) {
			return this.alreadyClosed();
		}

		// TODO Auto-generated method stub
		return this.alreadyClosed();
	}

	@Override
	public Future<Void> subscribeLs(final String parent, final Consumer<List<String>> callback) {
		if (this.isClosed()) {
			return this.alreadyClosed();
		}

		// TODO Auto-generated method stub
		return this.alreadyClosed();
	}

	@Override
	public Future<Void> unsubscribeLs(final long transactionId) {
		if (this.isClosed()) {
			return this.alreadyClosed();
		}

		// TODO Auto-generated method stub
		return this.alreadyClosed();
	}

	@Override
	public <T> Future<Void> cSet(final String key, final T value, final long version) {
		if (this.isClosed()) {
			return this.alreadyClosed();
		}

		// TODO Auto-generated method stub
		return this.alreadyClosed();
	}

	@Override
	public <T> Future<Tuple<T, Long>> cGet(final String key, final Class<T> type) {
		if (this.isClosed()) {
			return this.alreadyClosed();
		}

		// TODO Auto-generated method stub
		return this.alreadyClosed();
	}

	@Override
	public <T> Future<Tuple<T, Long>> cGet(final String key, final TypeReference<T> type) {
		if (this.isClosed()) {
			return this.alreadyClosed();
		}

		// TODO Auto-generated method stub
		return this.alreadyClosed();
	}

	@Override
	public <T, V> boolean update(final String key, final Function<Optional<T>, V> transform, final Class<T> type) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public <T, V> boolean update(final String key, final Function<Optional<T>, V> transform,
			final TypeReference<T> type) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public <T> boolean update(final String key, final Consumer<T> update, final Class<T> type) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public <T> boolean update(final String key, final Consumer<T> update, final TypeReference<T> type) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Future<Void> lock(final String key) {
		if (this.isClosed()) {
			return this.alreadyClosed();
		}

		// TODO Auto-generated method stub
		return this.alreadyClosed();
	}

	@Override
	public Future<Void> acquireLock(final String key) {
		if (this.isClosed()) {
			return this.alreadyClosed();
		}

		// TODO Auto-generated method stub
		return this.alreadyClosed();
	}

	@Override
	public Future<Void> releaseLock(final String key) {
		if (this.isClosed()) {
			return this.alreadyClosed();
		}

		// TODO Auto-generated method stub
		return this.alreadyClosed();
	}

	@Override
	public Future<List<String>> getGraveGoods() {
		if (this.isClosed()) {
			return this.alreadyClosed();
		}

		// TODO Auto-generated method stub
		return this.alreadyClosed();
	}

	@Override
	public Future<List<KeyValuePair>> getLastWill() {
		if (this.isClosed()) {
			return this.alreadyClosed();
		}

		// TODO Auto-generated method stub
		return this.alreadyClosed();
	}

	@Override
	public Future<Void> setGraveGoods(final List<String> graveGoods) {
		if (this.isClosed()) {
			return this.alreadyClosed();
		}

		// TODO Auto-generated method stub
		return this.alreadyClosed();
	}

	@Override
	public Future<Void> setLastWill(final List<KeyValuePair> lastWill) {
		if (this.isClosed()) {
			return this.alreadyClosed();
		}

		// TODO Auto-generated method stub
		return this.alreadyClosed();
	}

	@Override
	public void updateGraveGoods(final Consumer<List<String>> update) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateLastWill(final Consumer<List<KeyValuePair>> update) {
		// TODO Auto-generated method stub

	}

	@Override
	public Future<Void> setClientName(final String name) {
		if (this.isClosed()) {
			return this.alreadyClosed();
		}

		// TODO Auto-generated method stub
		return this.alreadyClosed();
	}

	@Override
	public ObjectMapper getObjectMapper() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getClientId() {
		// TODO Auto-generated method stub
		return null;
	}

	private void doClose() {
		if (this.isClosed()) {
			return;
		}

		if (this.state instanceof final State.Connected connected) {
			try {
				connected.client().close();
			} catch (final Exception e) {
				this.log.error("Error closing worterbuch client:", e);
				if (e instanceof InterruptedException) {
					Thread.currentThread().interrupt();
				}
			}
		}
		this.state = new State.Closed();
	}

	private <T> Future<T> alreadyClosed() {
		return new Future<>(CompletableFuture.completedFuture(new net.bbmsoft.worterbuch.client.response.Error<T>(
				new Err(-1, ReconnectingWorterbuchClientImpl.ALREADY_CLOSED, "already closed"))), -1);
	}
}
