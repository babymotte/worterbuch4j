package net.bbmsoft.worterbuch.client.collections;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import net.bbmsoft.worterbuch.client.WorterbuchClient;
import net.bbmsoft.worterbuch.client.WorterbuchException;

public class AsyncWorterbuchMap<T> implements Map<String, T> {

	private final WorterbuchClient wbClient;
	private final Map<String, T> localCache;
	private final String rootKey;
	private final Consumer<WorterbuchException> errorHandler;
	private final Class<T> valueType;

	public AsyncWorterbuchMap(final WorterbuchClient wbClient, final String application, final String namespace,
			final String mapName, final Class<T> valueType, final Consumer<WorterbuchException> errorHandler)
			throws ExecutionException {
		this.wbClient = wbClient;
		this.valueType = valueType;
		this.errorHandler = errorHandler;
		this.rootKey = application + "/state/" + namespace + "/" + mapName;
		this.localCache = new HashMap<>();
		try {
			final var kvps = wbClient.pGet(this.rootKey + "/?", valueType).get();
			kvps.forEach(kvp -> this.localCache.put(this.unescape(kvp.getKey().substring(this.rootKey.length() + 1)),
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
		this.wbClient.set(this.fullKey(key), value, this.errorHandler);
	}

	private void removeRemote(final Object key) {
		this.wbClient.delete(this.fullKey(key), this.valueType);
	}

	private void putAllRemote(final Map<? extends String, ? extends T> m) {
		m.forEach((k, v) -> this.wbClient.set(this.fullKey(k), v, this.errorHandler));
	}

	private void clearRemote() {
		this.wbClient.pDelete(this.rootKey + "/?", this.valueType);
	}

	private String fullKey(final Object k) {
		return this.rootKey + "/" + this.escape(k.toString());
	}

	private String escape(final String string) {
		return string.replace("/", "%2F");
	}

	private String unescape(final String string) {
		return string.replace("%2F", "/");
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
			return this.delegate.contains(AsyncWorterbuchMap.this.escape(o.toString()));
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
