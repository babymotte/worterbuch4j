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

package net.bbmsoft.worterbuch.client.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import net.bbmsoft.worterbuch.client.WorterbuchClient;

public class AsyncWorterbuchList<T> implements List<T> {

	private final WorterbuchClient wbClient;
	private final Consumer<? super Throwable> errorHandler;
	private final String key;
	private final List<T> localCache;

	public AsyncWorterbuchList(final WorterbuchClient wbClient, final String application, final String namespace,
			final String listName, final Class<T> valueType, final Consumer<? super Throwable> errorHandler)
			throws ExecutionException {
		this.wbClient = wbClient;
		this.errorHandler = errorHandler;
		this.key = application + "/state/" + namespace + "/" + listName;
		Optional<T[]> value = Optional.empty();
		try {
			value = wbClient.getArray(this.key, valueType).get();
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		this.localCache = value.map(a -> new ArrayList<>(Arrays.asList(a))).orElse(new ArrayList<>());
	}

	@Override
	public int size() {
		return this.localCache.size();
	}

	@Override
	public boolean isEmpty() {
		return this.localCache.isEmpty();
	}

	@Override
	public boolean contains(final Object o) {
		return this.localCache.contains(o);
	}

	@Override
	public Iterator<T> iterator() {
		return new SyncedIterator<>(this.localCache.iterator());
	}

	@Override
	public Object[] toArray() {
		return this.localCache.toArray();
	}

	@Override
	public <E> E[] toArray(final E[] a) {
		return this.localCache.toArray(a);
	}

	@Override
	public boolean add(final T e) {
		final var added = this.localCache.add(e);
		this.store();
		return added;
	}

	@Override
	public boolean remove(final Object o) {
		final var removed = this.localCache.remove(o);
		this.store();
		return removed;
	}

	@Override
	public boolean containsAll(final Collection<?> c) {
		return this.localCache.containsAll(c);
	}

	@Override
	public boolean addAll(final Collection<? extends T> c) {
		final var added = this.localCache.addAll(c);
		this.store();
		return added;
	}

	@Override
	public boolean addAll(final int index, final Collection<? extends T> c) {
		final var added = this.localCache.addAll(index, c);
		this.store();
		return added;
	}

	@Override
	public boolean removeAll(final Collection<?> c) {
		final var removed = this.localCache.removeAll(c);
		this.store();
		return removed;
	}

	@Override
	public boolean retainAll(final Collection<?> c) {
		final var retained = this.localCache.retainAll(c);
		this.store();
		return retained;
	}

	@Override
	public void clear() {
		this.localCache.clear();
		this.wbClient.delete(this.key, this.errorHandler);
	}

	@Override
	public T get(final int index) {
		return this.localCache.get(index);
	}

	@Override
	public T set(final int index, final T element) {
		final var set = this.localCache.set(index, element);
		this.store();
		return set;
	}

	@Override
	public void add(final int index, final T element) {
		this.localCache.add(element);
		this.store();
	}

	@Override
	public T remove(final int index) {
		final var removed = this.localCache.remove(index);
		this.store();
		return removed;
	}

	@Override
	public int indexOf(final Object o) {
		return this.localCache.indexOf(o);
	}

	@Override
	public int lastIndexOf(final Object o) {
		return this.localCache.indexOf(o);
	}

	@Override
	public ListIterator<T> listIterator() {
		return new SyncedListIterator<>(this.localCache.listIterator());
	}

	@Override
	public ListIterator<T> listIterator(final int index) {
		return new SyncedListIterator<>(this.localCache.listIterator(index));
	}

	@Override
	public List<T> subList(final int fromIndex, final int toIndex) {
		return this.localCache.subList(fromIndex, toIndex);
	}

	private void store() {
		if (this.localCache.isEmpty()) {
			this.wbClient.delete(this.key, this.errorHandler);
		} else {
			this.wbClient.set(this.key, this.localCache, this.errorHandler);
		}
	}

	class SyncedIterator<E> implements Iterator<E> {

		private final Iterator<E> delegate;

		public SyncedIterator(final Iterator<E> delegate) {
			this.delegate = delegate;
		}

		@Override
		public boolean hasNext() {
			return this.delegate.hasNext();
		}

		@Override
		public E next() {
			return this.delegate.next();
		}

		@Override
		public void remove() {
			this.delegate.remove();
			AsyncWorterbuchList.this.store();
		}

	}

	class SyncedListIterator<E> extends SyncedIterator<E> implements ListIterator<E> {

		private final ListIterator<E> delegate;

		public SyncedListIterator(final ListIterator<E> delegate) {
			super(delegate);
			this.delegate = delegate;
		}

		@Override
		public boolean hasPrevious() {
			return this.delegate.hasPrevious();
		}

		@Override
		public E previous() {
			return this.delegate.previous();
		}

		@Override
		public int nextIndex() {
			return this.delegate.nextIndex();
		}

		@Override
		public int previousIndex() {
			return 0;
		}

		@Override
		public void set(final E e) {
			this.delegate.set(e);
			AsyncWorterbuchList.this.store();
		}

		@Override
		public void add(final E e) {
			this.delegate.add(e);
			AsyncWorterbuchList.this.store();
		}

	}
}
