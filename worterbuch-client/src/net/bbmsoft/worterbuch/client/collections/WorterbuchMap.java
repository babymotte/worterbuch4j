package net.bbmsoft.worterbuch.client.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import net.bbmsoft.worterbuch.client.KeyValuePair;
import net.bbmsoft.worterbuch.client.WorterbuchClient;
import net.bbmsoft.worterbuch.client.WorterbuchException;

public class WorterbuchMap<T> implements Map<String, T> {

	private final String rootKey;
	private final WorterbuchClient wbClient;
	private final Class<T> valueType;
	private final Consumer<WorterbuchException> errorHandler;

	public WorterbuchMap(final WorterbuchClient wbClient, final String application, final String namespace,
			final String mapName, final Class<T> valueType, final Consumer<WorterbuchException> errorHandler) {
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
		final var escapedKey = this.escape(key.toString());
		try {
			final var keys = this.wbClient.ls(this.rootKey).get();
			return keys.contains(escapedKey);
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
		final var escapedKey = this.escape(key.toString());
		final var fullKey = this.rootKey + "/" + escapedKey;
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
		final var escapedKey = this.escape(key.toString());
		final var fullKey = this.rootKey + "/" + escapedKey;
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
		final var escapedKey = this.escape(key.toString());
		final var fullKey = this.rootKey + "/" + escapedKey;
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
			return keys.stream().map(this::unescape).collect(Collectors.toSet());
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
				return WorterbuchMap.this.unescape(kvp.getKey());
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

	private String escape(final String string) {
		return string.replace("/", "%2F");
	}

	private String unescape(final String string) {
		return string.replace("%2F", "/");
	}

}
