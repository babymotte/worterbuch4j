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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bbmsoft.worterbuch.client.api.WorterbuchClient;

/**
 * A {@link Map} implementation that automatically synchronizes its contents to
 * worterbuch. A {@link HashMap} is used to store the data of this Map locally
 * and it will asynchronously be synchronized to worterbuch in the background.
 * <p>
 * Simultaneous writes from both multiple threads or multiple JVMs will lead to
 * data corruption and/or lost updates, however updating the map from one JVM
 * while reading it from another is safe.
 *
 * @param <T> the type of the Map's values.
 */
public class AsyncWorterbuchMap<T> implements Map<String, T> {

	private final WorterbuchClient wbClient;
	private final Map<String, T> localCache;
	private final String rootKey;
	private final Class<T> valueType;

	@SuppressFBWarnings(value = "EI_EXPOSE_REP2")
	public AsyncWorterbuchMap(final WorterbuchClient wbClient, final String application, final String namespace,
			final String mapName, final Class<T> valueType) {
		this.wbClient = wbClient;
		this.valueType = valueType;
		this.rootKey = application + "/state/" + namespace + "/" + mapName;
		this.localCache = new HashMap<>();
	}

	public void init() throws ExecutionException {
		try {
			final var kvps = this.wbClient.pGet(this.rootKey + "/?", this.valueType).get();
			kvps.forEach(kvp -> this.localCache.put(Utils.unescape(kvp.getKey().substring(this.rootKey.length() + 1)),
					kvp.getValue()));
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		}
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
	public boolean containsKey(final Object key) {
		return this.localCache.containsKey(key);
	}

	@Override
	public boolean containsValue(final Object value) {
		return this.localCache.containsValue(value);
	}

	@Override
	public T get(final Object key) {
		return this.localCache.get(key);
	}

	@Override
	public T put(final String key, final T value) {
		final var previous = this.localCache.put(key, value);
		this.putRemote(key, value);
		return previous;
	}

	@Override
	public T remove(final Object key) {
		Objects.requireNonNull(key);
		final var removed = this.localCache.remove(key);
		this.removeRemote(key);
		return removed;
	}

	@Override
	public void putAll(final Map<? extends String, ? extends T> m) {
		this.localCache.putAll(m);
		this.putAllRemote(m);
	}

	@Override
	public void clear() {
		this.localCache.clear();
		this.clearRemote();
	}

	@Override
	public Set<String> keySet() {
		return new SyncedKeySet(this.localCache.keySet());
	}

	@Override
	public Collection<T> values() {
		return this.localCache.values();
	}

	@Override
	public Set<Entry<String, T>> entrySet() {
		return new SyncedEntrySet(this.localCache.entrySet());
	}

	@Override
	public String toString() {
		return this.localCache.toString();
	}

	private void putRemote(final String key, final T value) {
		this.wbClient.set(this.fullKey(key), value);
	}

	private void removeRemote(final Object key) {
		this.wbClient.delete(this.fullKey(key), this.valueType);
	}

	private void putAllRemote(final Map<? extends String, ? extends T> m) {
		m.forEach((k, v) -> this.wbClient.set(this.fullKey(k), v));
	}

	private void clearRemote() {
		this.wbClient.pDelete(this.rootKey + "/?", this.valueType);
	}

	private String fullKey(final Object k) {
		return this.rootKey + "/" + Utils.escape(k.toString());
	}

	class SyncedKeySet implements Set<String> {

		private final Set<String> delegate;

		public SyncedKeySet(final Set<String> keySet) {
			this.delegate = keySet;
		}

		@Override
		public int size() {
			return this.delegate.size();
		}

		@Override
		public boolean isEmpty() {
			return this.delegate.isEmpty();
		}

		@Override
		public boolean contains(final Object o) {
			return this.delegate.contains(Utils.escape(o.toString()));
		}

		@Override
		public Iterator<String> iterator() {
			return new SyncedKeyIterator(this.delegate.iterator());
		}

		@Override
		public Object[] toArray() {
			return this.delegate.toArray();
		}

		@Override
		public <V> V[] toArray(final V[] a) {
			return this.delegate.toArray(a);
		}

		@Override
		public boolean add(final String e) {
			throw new UnsupportedOperationException("not implemented");
		}

		@Override
		public boolean remove(final Object o) {
			final var removed = this.delegate.remove(o);
			AsyncWorterbuchMap.this.removeRemote(o);
			return removed;
		}

		@Override
		public boolean containsAll(final Collection<?> c) {
			return this.delegate.containsAll(c);
		}

		@Override
		public boolean addAll(final Collection<? extends String> c) {
			throw new UnsupportedOperationException("not implemented");
		}

		@Override
		public boolean retainAll(final Collection<?> c) {
			final Set<String> removed = new HashSet<>(this.delegate);
			removed.removeAll(c);
			final var changed = this.delegate.retainAll(c);
			removed.forEach(AsyncWorterbuchMap.this::removeRemote);
			return changed;
		}

		@Override
		public boolean removeAll(final Collection<?> c) {
			final Set<Object> removed = new HashSet<>(c);
			removed.retainAll(this.delegate);
			final var changed = this.delegate.removeAll(c);
			removed.forEach(AsyncWorterbuchMap.this::removeRemote);
			return changed;
		}

		@Override
		public void clear() {
			this.delegate.clear();
			AsyncWorterbuchMap.this.clearRemote();
		}
	}

	class SyncedEntrySet implements Set<Map.Entry<String, T>> {

		private final Set<Entry<String, T>> delegate;

		public SyncedEntrySet(final Set<Entry<String, T>> entrySet) {
			this.delegate = entrySet;
		}

		@Override
		public int size() {
			return this.delegate.size();
		}

		@Override
		public boolean isEmpty() {
			return this.delegate.isEmpty();
		}

		@Override
		public boolean contains(final Object o) {
			return this.delegate.contains(o);
		}

		@Override
		public Iterator<Entry<String, T>> iterator() {
			return new SyncedEntryIterator<>(this.delegate.iterator());
		}

		@Override
		public Object[] toArray() {
			return this.delegate.toArray();
		}

		@Override
		public <V> V[] toArray(final V[] a) {
			return this.delegate.toArray(a);
		}

		@Override
		public boolean add(final Entry<String, T> e) {
			final var added = this.delegate.add(e);
			AsyncWorterbuchMap.this.putRemote(e.getKey(), e.getValue());
			return added;
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean remove(final Object o) {
			final var removed = this.delegate.remove(o);
			AsyncWorterbuchMap.this.removeRemote(((Map.Entry<String, T>) o).getKey());
			return removed;
		}

		@Override
		public boolean containsAll(final Collection<?> c) {
			return this.delegate.containsAll(c);
		}

		@Override
		public boolean addAll(final Collection<? extends Entry<String, T>> c) {
			final var added = this.delegate.addAll(c);
			c.forEach(e -> AsyncWorterbuchMap.this.putRemote(e.getKey(), e.getValue()));
			return added;
		}

		@Override
		public boolean retainAll(final Collection<?> c) {
			final Set<Map.Entry<String, T>> removed = new HashSet<>(this.delegate);
			removed.removeAll(c);
			final var changed = this.delegate.retainAll(c);
			removed.forEach(e -> AsyncWorterbuchMap.this.removeRemote(e.getKey()));
			return changed;
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean removeAll(final Collection<?> c) {
			final Set<Object> removed = new HashSet<>(c);
			removed.retainAll(this.delegate);
			final var changed = this.delegate.removeAll(c);
			removed.forEach(e -> AsyncWorterbuchMap.this.removeRemote(((Map.Entry<String, T>) e).getKey()));
			return changed;
		}

		@Override
		public void clear() {
			this.delegate.clear();
			AsyncWorterbuchMap.this.clearRemote();
		}

	}

	class SyncedKeyIterator implements Iterator<String> {

		private final Iterator<String> delegate;

		private String current;

		public SyncedKeyIterator(final Iterator<String> iterator) {
			this.delegate = iterator;
		}

		@Override
		public boolean hasNext() {
			return this.delegate.hasNext();
		}

		@Override
		public String next() {
			return this.current = this.delegate.next();
		}

		@Override
		public void remove() {
			this.delegate.remove();
			AsyncWorterbuchMap.this.removeRemote(this.current);
		}

	}

	class SyncedEntryIterator<V> implements Iterator<Map.Entry<String, V>> {

		private final Iterator<Map.Entry<String, V>> delegate;

		private Map.Entry<String, V> current;

		public SyncedEntryIterator(final Iterator<Map.Entry<String, V>> iterator) {
			this.delegate = iterator;
		}

		@Override
		public boolean hasNext() {
			return this.delegate.hasNext();
		}

		@Override
		public Map.Entry<String, V> next() {
			return this.current = this.delegate.next();
		}

		@Override
		public void remove() {
			this.delegate.remove();
			AsyncWorterbuchMap.this.removeRemote(this.current.getKey());
		}

	}

}
