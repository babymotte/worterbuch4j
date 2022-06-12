package net.bbmsoft.worterbuch.client.api;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
	 * A list of protocols the client supports
	 *
	 * @return a list of protocols the client supports
	 */
	public List<String> supportedProtocols();

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
	 * @throws IllegalStateException    if called while {@link #disconnect()
	 *                                  disconnecting}
	 * @throws IllegalArgumentException if the provided {@link URL} uses a protocol
	 *                                  the client does not support
	 */
	public Future<Void> connect(URI uri);

	/**
	 * Closes the connection to the server. This function must not be called without
	 * calling {@link #connect(URL)} first. It is however not necessary to wait for
	 * the future returned by {@link #connect(URL)} to complete before calling this
	 * function.
	 * <p>
	 * The client will attempt to send any previously issued requests before
	 * disconnecting, however it will not wait for any replies from the server. Any
	 * requests that can not be sent within the specified timeout will be dropped.
	 * <p>
	 * The returned future will complete once all pending requests have completed or
	 * the specified timeout has passed and the client has disconnected. The
	 * returned boolean indicates whether the client was able to disconnect in time.
	 *
	 * @throws IllegalStateException if called while not connected or connecting
	 */
	public Future<Boolean> disconnect();

	/**
	 * Synonymous to {@link #disconnect() this.disconnect().get(3,
	 * TimeUnit.SECONDS)}.
	 *
	 * @throws ExecutionException   if an {@link IOException} occurs while
	 *                              disconnecting
	 * @throws InterruptedException if the current thread is interrupted while
	 *                              waiting for the disconnect to complete
	 * @throws TimeoutException     if the client could not disconnect within three
	 *                              seconds
	 */
	@Override
	default void close() throws TimeoutException, InterruptedException, ExecutionException {
		this.disconnect().get(3, TimeUnit.SECONDS);
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
	public Future<Map<String, String>> get(String pattern);

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
	public Future<Object> set(String key, String value);

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
	 * Runs the provided action when the connection is closed by the server. Call
	 * {@link #disconnect()} will NOT trigger this action.
	 *
	 * @param action run when the connection is closed by the server
	 */
	public void onConnectionLost(Runnable action);

	/**
	 * Data object used to transport key/value change events in a subscription
	 *
	 * @author mbachmann
	 *
	 */
	public record Event(String key, String value) {
	}

}
