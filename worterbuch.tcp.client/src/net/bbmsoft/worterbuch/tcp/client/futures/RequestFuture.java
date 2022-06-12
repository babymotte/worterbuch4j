package net.bbmsoft.worterbuch.tcp.client.futures;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RequestFuture<T> implements Future<T> {

	private final Future<BlockingQueue<T>> request;

	private volatile boolean done;

	public RequestFuture(final Future<BlockingQueue<T>> request) {
		this.request = request;
	}

	@Override
	public boolean cancel(final boolean mayInterruptIfRunning) {
		return false;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return this.done;
	}

	@Override
	public T get() throws InterruptedException, ExecutionException {
		final var queue = this.request.get();
		return queue.take();
	}

	@Override
	public T get(final long timeout, final TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {

		final var start = System.currentTimeMillis();
		final var queue = this.request.get(timeout, unit);

		final var elapsed = System.currentTimeMillis() - start;
		final var remaining = unit.toMillis(timeout) - elapsed;

		if (remaining > 0) {
			final var value = queue.poll(remaining, TimeUnit.MILLISECONDS);
			if (value == null) {
				throw new TimeoutException("did not receive server response in time");
			} else {
				return value;
			}
		} else {
			throw new TimeoutException("request could not be sent in time");
		}
	}

}
