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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import net.bbmsoft.worterbuch.client.KeyValuePair;
import net.bbmsoft.worterbuch.client.WorterbuchClient;

/**
 * A {@link Map} implementation that uses worterbuch as its data store. This
 * implementation does not store any data locally, all operations are done
 * directly on the underlying worterbuch instance.
 * <p>
 * Concurrent access to this map (both from multiple threads within the same JVM
 * and from separate JVMs) will not cause any data races that will leave the Map
 * in a corrupted state, however care needs to be taken when using
 * check-then-act patterns because there is no option to lock the contents of
 * the map across instances.<br>
 * In other words calling {@link #put(String, Object)} simultaneously from
 * different threads or JVMs at the same time, even with the same key, is fine
 * (last one wins), however something like <code>
 * <pre>
 * if (!map.containsKey("key")) {
 * 	map.put("key", "value");
 * }
 * </pre>
 * </code> may produce unexpected results because another client may put data to
 * "key" after {@code map.containsKey("key")} but before
 * {@code map.put("key", "value");}
 *
 * @param <T> the type of the Map's values.
 */
public class WorterbuchMap<T> implements Map<String, T> {

	private final String rootKey;
	private final WorterbuchClient wbClient;
	private final Class<T> valueType;
	private final Consumer<? super Throwable> errorHandler;

	public WorterbuchMap(final WorterbuchClient wbClient, final String application, final String namespace,
			final String mapName, final Class<T> valueType, final Consumer<? super Throwable> errorHandler) {
		this.wbClient = wbClient;
		this.errorHandler = errorHandler;
		this.rootKey = application + "/state/" + namespace + "/" + mapName;
		this.valueType = valueType;
	}

	@Override
	public int size() {
		try {
			return this.wbClient.ls(this.rootKey).get().size();
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			return 0;
		} catch (final ExecutionException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public boolean isEmpty() {
		return this.size() == 0;
	}

	@Override
	public boolean containsKey(final Object key) {
		final var fullKey = this.fullKey(key);
		try {
			final var keys = this.wbClient.ls(this.rootKey).get();
			return keys.contains(fullKey);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		} catch (final ExecutionException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public boolean containsValue(final Object value) {
		try {
			final var kvps = this.wbClient.pGet(this.rootKey + "/?", this.valueType).get();
			return kvps.stream().anyMatch(e -> Objects.equals(e.getValue(), value));
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		} catch (final ExecutionException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public T get(final Object key) {
		final var fullKey = this.fullKey(key);
		try {
			final var state = this.wbClient.get(fullKey, this.valueType).get();
			return state.orElse(null);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		} catch (final ExecutionException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public T put(final String key, final T value) {
		final var fullKey = this.fullKey(key);
		try {
			final var state = this.wbClient.get(fullKey, this.valueType).get();
			final var currentValue = state.orElse(null);
			this.wbClient.set(fullKey, value, this.errorHandler);
			return currentValue;
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		} catch (final ExecutionException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public T remove(final Object key) {
		final var fullKey = this.fullKey(key);
		try {
			final var state = this.wbClient.delete(fullKey, this.valueType).get();
			return state.orElse(null);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		} catch (final ExecutionException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void putAll(final Map<? extends String, ? extends T> m) {
		m.forEach(this::put);
	}

	@Override
	public void clear() {
		this.wbClient.pDelete(this.rootKey + "/?", this.valueType);
	}

	@Override
	public Set<String> keySet() {
		try {
			final var keys = this.wbClient.ls(this.rootKey).get();
			return keys.stream().map(this::trimKey).collect(Collectors.toSet());
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			return Collections.emptySet();
		} catch (final ExecutionException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public Collection<T> values() {
		final List<T> values = new ArrayList<>();
		try {
			final var kvps = this.wbClient.pGet(this.rootKey + "/?", this.valueType).get();
			kvps.forEach(kvp -> values.add(kvp.getValue()));
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (final ExecutionException e) {
			throw new IllegalStateException(e);
		}
		return values;
	}

	@Override
	public Set<Entry<String, T>> entrySet() {
		try {
			final var kvps = this.wbClient.pGet(this.rootKey + "/?", this.valueType).get();
			return kvps.stream().map(this::toEntry).collect(Collectors.toSet());
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			return Collections.emptySet();
		} catch (final ExecutionException e) {
			throw new IllegalStateException(e);
		}
	}

	private Map.Entry<String, T> toEntry(final KeyValuePair<T> kvp) {
		return new Map.Entry<>() {

			@Override
			public String getKey() {
				return WorterbuchMap.this.trimKey(kvp.getKey());
			}

			@Override
			public T getValue() {
				return kvp.getValue();
			}

			@Override
			public T setValue(final T value) {
				return WorterbuchMap.this.put(kvp.getKey(), value);
			}
		};
	}

	String fullKey(final Object key) {
		return Utils.fullKey(key, this.rootKey);
	}

	String trimKey(final String fullKey) {
		return Utils.trimKey(fullKey, this.rootKey);
	}

	public long addListener(final BiConsumer<String, T> listener, final boolean unique, final boolean liveOnly) {
		return this.addListener(listener, unique, liveOnly, null);
	}

	public long addListener(final BiConsumer<String, T> listener, final boolean unique, final boolean liveOnly,
			final Executor executor) {

		final var theExecutor = executor != null ? executor : (Executor) Runnable::run;

		final var key = this.rootKey + "/?";
		final var tid = this.wbClient.pSubscribe(key, unique, liveOnly, Optional.of(1L), this.valueType, e -> {
			if (e.keyValuePairs != null) {
				e.keyValuePairs.forEach(kvp -> {
					final var fullKey = kvp.getKey();
					final var value = kvp.getValue();
					final var trimmedKey = this.trimKey(fullKey);
					theExecutor.execute(() -> listener.accept(trimmedKey, value));
				});
			}
			if (e.deleted != null) {
				e.deleted.forEach(kvp -> {
					final var fullKey = kvp.getKey();
					final var trimmedKey = this.trimKey(fullKey);
					theExecutor.execute(() -> listener.accept(trimmedKey, null));
				});
			}
		}, this.errorHandler);
		return tid;
	}

	public void removeListener(final long transactionId) {
		this.wbClient.unsubscribeLs(transactionId, this.errorHandler);
	}
}
