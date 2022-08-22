package net.bbmsoft.worterbuch.client.api;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.osgi.annotation.versioning.ProviderType;

import net.bbmsoft.worterbuch.client.api.AsyncWorterbuchClient.Handshake;

/**
 * A client for a Wörterbuch server. Once connected it can be used to get, set
 * and subscribe to values. Values will automatically be cached and the cache
 * will be kept in sync with the server, so any subsequent get requests to the
 * same value will not cause any network traffic.
 *
 * @since 1.0
 */
@ProviderType
public interface CachingWorterbuchClient extends AutoCloseable {

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
	public Future<Handshake> connect(URI uri);

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

	public String get(String key) throws InterruptedException;

	public void set(String key, String value);

	public void subscribe(String key, Consumer<String> onValue);

	/**
	 * Runs the provided action when the connection is closed by the server. Call
	 * {@link #disconnect()} will NOT trigger this action.
	 *
	 * @param action run when the connection is closed by the server
	 */
	public void onConnectionLost(Runnable action);
}
