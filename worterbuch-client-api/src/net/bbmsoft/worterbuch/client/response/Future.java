package net.bbmsoft.worterbuch.client.response;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bbmsoft.worterbuch.client.error.ConnectionError;
import net.bbmsoft.worterbuch.client.model.Err;

@SuppressFBWarnings({ "EI_EXPOSE_REP", "EI_EXPOSE_REP2" })
public record Future<T>(CompletableFuture<Response<T>> responseFuture, long transactionId) {

	/**
	 * Waits for the request to either succeed, in which case the server's
	 * {@link Response response} is returned, or to fail (e.g. due to a network
	 * error), in which case the resulting exception is thrown wrapped in a
	 * {@link ConnectionError}
	 *
	 * @return the server's response
	 * @throws ConnectionError      if the request failed, e.g. due to a network
	 *                              error
	 * @throws InterruptedException if the current thread is interrupted while
	 *                              waiting for the server's response
	 */
	public Response<T> await() throws ConnectionError, InterruptedException {
		try {
			return this.responseFuture.get();
		} catch (final ExecutionException e) {
			throw new ConnectionError("Could not get server response",e);
		}
	}

	/**
	 * Waits at most the specified amount of time for the request to either succeed,
	 * in which case the server's {@link Response response} is returned, or to fail
	 * (e.g. due to a network error), in which case the resulting exception is
	 * thrown wrapped in a {@link ConnectionError}
	 *
	 * @param timeout the maximum time to wait
	 * @param unit    time unit of the timeout argument
	 * @return the server's {@link Response response}
	 * @throws ConnectionError      if the request failed, e.g. due to a network
	 *                              error
	 * @throws InterruptedException if the current thread is interrupted while
	 *                              waiting for the server's response
	 * @throws TimeoutException     if the wait timed out
	 */
	public Response<T> await(final long timeout, final TimeUnit unit)
			throws ConnectionError, InterruptedException, TimeoutException {
		try {
			return this.responseFuture.get(timeout, unit);
		} catch (final ExecutionException e) {
			throw new ConnectionError("Could not get server response",e);
		}
	}

	/**
	 * Waits at most the specified amount of time for the request to either succeed,
	 * in which case the server's {@link Response response} is returned, or to fail
	 * (e.g. due to a network error), in which case the resulting exception is
	 * thrown wrapped in a {@link ConnectionError}
	 *
	 * @param timeout the maximum time to wait
	 * @return the server's {@link Response response}
	 * @throws ConnectionError      if the request failed, e.g. due to a network
	 *                              error
	 * @throws InterruptedException if the current thread is interrupted while
	 *                              waiting for the server's response
	 * @throws TimeoutException     if the wait timed out
	 */
	public Response<T> await(final Duration timeout) throws ConnectionError, InterruptedException, TimeoutException {
		return this.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
	}

	/**
	 * Register an asynchronous callback that accept's the server's success response
	 * value, if any.
	 *
	 * @param callback accepts either {@code Optional.of(responseValue)} if a
	 *                 success response was received or {@code Optional.empty()} if
	 *                 the server sent an error or if there was an Exception.
	 */
	public void andThen(final Consumer<Optional<T>> callback) {
		this.responseFuture().whenCompleteAsync((res, e) -> {
			if (e != null) {
				callback.accept(Optional.empty());
			} else if (res != null) {
				res.ifOkOrElse(v -> callback.accept(Optional.of(v)), err -> callback.accept(Optional.empty()));
			}
		});
	}

	public void andThen(final Consumer<Optional<T>> callback, final Executor exec) {
		this.responseFuture().whenCompleteAsync((res, e) -> {
			if (e != null) {
				exec.execute(() -> callback.accept(Optional.empty()));
			} else if (res != null) {
				res.ifOkOrElse(v -> exec.execute(() -> callback.accept(Optional.of(v))),
						err -> exec.execute(() -> callback.accept(Optional.empty())));
			}
		});
	}

	public void andThen(final Consumer<T> onSuccess, final Consumer<Err> onErrorResponse,
			final Consumer<Throwable> onExecutionError) {
		this.responseFuture().whenCompleteAsync((res, e) -> {
			if (e != null) {
				onExecutionError.accept(e);
			} else if (res != null) {
				res.ifOkOrElse(onSuccess, onErrorResponse);
			}
		});
	}

	public void andThen(final Consumer<T> onSuccess, final Consumer<Err> onErrorResponse,
			final Consumer<Throwable> onExecutionError, final Executor exec) {
		this.responseFuture().whenCompleteAsync((res, e) -> {
			if (e != null) {
				exec.execute(() -> onExecutionError.accept(e));
			} else if (res != null) {
				res.ifOkOrElse(v -> exec.execute(() -> onSuccess.accept(v)),
						err -> exec.execute(() -> onErrorResponse.accept(err)));
			}
		});
	}
}
