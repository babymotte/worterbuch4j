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

package net.bbmsoft.worterbuch.client.impl;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class WrappingExecutor implements ScheduledExecutorService {

	private final ScheduledExecutorService delegate;
	private final Consumer<Throwable> onError;

	public WrappingExecutor(final ScheduledExecutorService delegate, final Consumer<Throwable> onError) {
		this.delegate = delegate;
		this.onError = onError;
	}

	@Override
	public ScheduledFuture<?> schedule(final Runnable command, final long delay, final TimeUnit unit) {
		return this.delegate.schedule(this.wrap(command), delay, unit);
	}

	@Override
	public void execute(final Runnable command) {
		this.delegate.execute(this.wrap(command));
	}

	@Override
	public <V> ScheduledFuture<V> schedule(final Callable<V> callable, final long delay, final TimeUnit unit) {
		return this.delegate.schedule(this.wrap(callable), delay, unit);
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay, final long period,
			final TimeUnit unit) {
		return this.delegate.scheduleAtFixedRate(this.wrap(command), initialDelay, period, unit);
	}

	@Override
	public void shutdown() {
		this.delegate.shutdown();
	}

	@Override
	public List<Runnable> shutdownNow() {
		return this.delegate.shutdownNow();
	}

	@Override
	public boolean isShutdown() {
		return this.delegate.isShutdown();
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, final long initialDelay, final long delay,
			final TimeUnit unit) {
		return this.delegate.scheduleWithFixedDelay(this.wrap(command), initialDelay, delay, unit);
	}

	@Override
	public boolean isTerminated() {
		return this.delegate.isTerminated();
	}

	@Override
	public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
		return this.delegate.awaitTermination(timeout, unit);
	}

	@Override
	public <T> Future<T> submit(final Callable<T> task) {
		return this.delegate.submit(this.wrap(task));
	}

	@Override
	public <T> Future<T> submit(final Runnable task, final T result) {
		return this.delegate.submit(this.wrap(task), result);
	}

	@Override
	public Future<?> submit(final Runnable task) {
		return this.delegate.submit(this.wrap(task));
	}

	@Override
	public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws InterruptedException {
		return this.delegate.invokeAll(tasks.stream().map(this::wrap).toList());
	}

	@Override
	public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks, final long timeout,
			final TimeUnit unit) throws InterruptedException {
		return this.delegate.invokeAll(tasks.stream().map(this::wrap).toList(), timeout, unit);
	}

	@Override
	public <T> T invokeAny(final Collection<? extends Callable<T>> tasks)
			throws InterruptedException, ExecutionException {
		return this.delegate.invokeAny(tasks.stream().map(this::wrap).toList());
	}

	@Override
	public <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		return this.delegate.invokeAny(tasks.stream().map(this::wrap).toList(), timeout, unit);
	}

	private Runnable wrap(final Runnable command) {
		return () -> {
			try {
				command.run();
			} catch (final Throwable th) {
				this.onError.accept(th);
			}
		};
	}

	private <T> Callable<T> wrap(final Callable<T> command) {
		return () -> {
			try {
				return command.call();
			} catch (final Throwable th) {
				this.onError.accept(th);
				throw th;
			}
		};
	}
}
