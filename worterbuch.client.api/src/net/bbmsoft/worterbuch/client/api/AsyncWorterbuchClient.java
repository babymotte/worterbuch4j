package net.bbmsoft.worterbuch.client.api;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.osgi.annotation.versioning.ProviderType;

/**
 * A client for a Wörterbuch server. Once connected it can be used to get, set
 * and subscribe to values.
 * 
 * @since 1.0
 */
@ProviderType
public interface AsyncWorterbuchClient extends AutoCloseable {

	/**
	 * Connect to a Wörterbuch server.
	 * <p>
	 * The returned future will throw an {@link ExecutionException} if an
	 * {@link IOException} occurs while connecting.
	 * <p>
	 * Once the client is connected it will start processing requests to the server.
	 * Requests cannot be made before calling the connect function, however it is
	 * not necessary to wait for the connect future to complete before making
	 * requests or calling {@link #disconnect()}. Requests made before the
	 * connection is actually established will be buffered and executed once a
	 * connection has been established.
	 * <p>
	 * If this is called after calling {@link #disconnect()}, the future returned by
	 * {@link #disconnect()} must have completed at the time this function is
	 * claled, otherwise an {@link IllegalStateException} will be thrown.
	 * 
	 * @param url the server's URL
	 * @return a future that completes as soon as a connection has been established
	 * @throws IllegalStateException if called while {@link #disconnect()
	 *                               disconnecting}
	 */
	public Future<Void> connect(URL url);

	/**
	 * Closes the connection to the server. This function must not be called without
	 * calling {@link #connect(URL)} first. It is however not necessary to wait for
	 * the future returned by {@link #connect(URL)} to complete before calling this
	 * function.
	 * <p>
	 * The client will attempt to complete any previously issued requests before
	 * disconnecting. Any requests that can not be completed within the specified
	 * timeout will be dropped.
	 * <p>
	 * The returned future will complete once all pending requests have completed or
	 * the specified timeout has passed and the client has disconnected.
	 * 
	 * @throws IllegalStateException if called while not connected or connecting
	 */
	public Future<Void> disconnect(Duration timeout);

	/**
	 * Synonymous to {@link #disconnect()
	 * this.disconnect(Duration.ofSeconds(3)).get()}.
	 * 
	 * @throws ExecutionException   if an {@link IOException} occurs while
	 *                              disconnecting
	 * @throws InterruptedException if the current thread is interrupted while
	 *                              waiting for the disconnect to complete
	 */
	@Override
	default void close() throws InterruptedException, ExecutionException {
		this.disconnect(Duration.ofSeconds(3)).get();
	}

	/**
	 * Perform a GET request to the server. This function must not be called before
	 * {@link #connect(URL)}, however it may be called before the future returned by
	 * {@link #connect(URL)} completes.
	 * <p>
	 * The returned future will throw an {@link ExecutionException} if an
	 * {@link IOException} occurs while making the request.
	 * 
	 * @param pattern a request pattern conforming to the Wörterbuch specification
	 * @return a future that resolves to a map of all key/value pairs matching the
	 *         provided pattern
	 */
	public Future<HashMap<String, String>> get(String pattern);

	/**
	 * Perform a SET request to the server. This function must not be called before
	 * {@link #connect(URL)}, however it may be called before the future returned by
	 * {@link #connect(URL)} completes.
	 * <p>
	 * The returned future will throw an {@link ExecutionException} if an
	 * {@link IOException} occurs while making the request.
	 * 
	 * @param key   the key for which to SET a value
	 * @param value the value to set
	 * @return a future that completes as soon as the server has ACKed the SET
	 *         operation
	 */
	public Future<Void> set(String key, String value);

	/**
	 * Perform a SUBSCRIBE request to the server. This function must not be called
	 * before {@link #connect(URL)}, however it may be called before the future
	 * returned by {@link #connect(URL)} completes.
	 * <p>
	 * The returned future will throw an {@link ExecutionException} if an
	 * {@link IOException} occurs while making the request.
	 * <p>
	 * If the subscription is successful, it will return a {@link BlockingQueue}
	 * into which the client will push events from the server. Events are wrapped
	 * into an {@link Optional} to indicate the connection is still open. If the
	 * connection closes, a single {@link Optional#empty()} will be pushed to the
	 * queue indicating that there will be no more events coming in from this
	 * subscription.
	 * 
	 * @param pattern a request pattern conforming to the Wörterbuch specification
	 * @return a future that resolves to a {@link BlockingQueue} into which the
	 *         client will push any {@link Event Events} it receives from the server
	 *         as a result of this subscription
	 */
	public Future<BlockingQueue<Optional<Event>>> subscribe(String pattern);

	/**
	 * Data object used to transport key/value change events in a subscription
	 * @author mbachmann
	 *
	 */
	public record Event(String key, String event) {
	}

}
