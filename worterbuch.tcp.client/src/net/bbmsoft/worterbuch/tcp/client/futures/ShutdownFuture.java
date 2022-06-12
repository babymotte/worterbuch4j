package net.bbmsoft.worterbuch.tcp.client.futures;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ShutdownFuture implements Future<Boolean> {

	private final ExecutorService executor;

	private volatile boolean completed = false;

	public ShutdownFuture(final ExecutorService executor) {
		this.executor = executor;
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
		return this.completed;
	}

	@Override
	public Boolean get() throws InterruptedException, ExecutionException {
		final var clean = this.executor.awaitTermination(10, TimeUnit.SECONDS);
		if (!clean) {
			this.executor.shutdownNow();
		}
		this.completed = true;
		return clean;
	}

	@Override
	public Boolean get(final long timeout, final TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		final var clean = this.executor.awaitTermination(timeout, unit);
		if (!clean) {
			this.executor.shutdownNow();
		}
		this.completed = true;
		return clean;
	}

}
