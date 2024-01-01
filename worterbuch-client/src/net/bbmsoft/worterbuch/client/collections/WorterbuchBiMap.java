package net.bbmsoft.worterbuch.client.collections;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.collect.BiMap;

import net.bbmsoft.worterbuch.client.KeyValuePair;
import net.bbmsoft.worterbuch.client.WorterbuchClient;

public class WorterbuchBiMap implements BiMap<String, String> {

	private final String rootKey;
	private final WorterbuchClient wbClient;
	private final Consumer<? super Throwable> errorHandler;

	public WorterbuchBiMap(final WorterbuchClient wbClient, final String application, final String namespace,
			final String mapName, final Consumer<? super Throwable> errorHandler) {
		this.wbClient = wbClient;
		this.errorHandler = errorHandler;
		this.rootKey = application + "/state/" + namespace + "/" + mapName;
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
		final var escapedKey = Utils.escape(key.toString());
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
		final var escapedValue = Utils.escape(value.toString());
		try {
			final var kvps = this.wbClient.pGet(this.rootKey + "/#", String.class).get();
			return kvps.stream().anyMatch(e -> Objects.equals(e.getValue(), escapedValue));
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		} catch (final ExecutionException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public String get(final Object key) {
		final var escapedKey = Utils.escape(key.toString());
		final var fullKey = this.rootKey + "/" + escapedKey;
		try {
			final var state = this.wbClient.get(fullKey, String.class).get();
			return state.map(Utils::unescape).orElse(null);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		} catch (final ExecutionException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public String put(final String key, final String value) {
		final var escapedKey = Utils.escape(key.toString());
		final var fullKey = this.rootKey + "/" + escapedKey;
		final var escapedValue = Utils.escape(value.toString());
		try {
			final var state = this.wbClient.get(fullKey, String.class).get();
			final var currentValue = state.orElse(null);
			this.wbClient.set(fullKey, escapedValue, this.errorHandler);
			return currentValue;
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		} catch (final ExecutionException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public String remove(final Object key) {
		final var escapedKey = Utils.escape(key.toString());
		final var fullKey = this.rootKey + "/" + escapedKey;
		try {
			final var state = this.wbClient.delete(fullKey, String.class).get();
			return state.map(Utils::unescape).orElse(null);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		} catch (final ExecutionException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void putAll(final Map<? extends String, ? extends String> m) {
		m.forEach(this::put);
	}

	@Override
	public void clear() {
		this.wbClient.pDelete(this.rootKey + "/#", String.class);
	}

	@Override
	public Set<String> keySet() {
		try {
			final var keys = this.wbClient.ls(this.rootKey).get();
			return keys.stream().map(Utils::unescape).collect(Collectors.toSet());
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			return Collections.emptySet();
		} catch (final ExecutionException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public Set<String> values() {
		final Set<String> values = new HashSet<>();
		try {
			final var kvps = this.wbClient.pGet(this.rootKey + "/#", String.class).get();
			kvps.forEach(kvp -> values.add(Utils.unescape(kvp.getValue())));
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (final ExecutionException e) {
			throw new IllegalStateException(e);
		}
		return values;
	}

	@Override
	public BiMap<String, String> inverse() {
		return new InvertedWorterbuchBiMap();
	}

	@Override
	public String forcePut(final String key, final String value) {
		return this.put(key, value);
	}

	@Override
	public Set<Entry<String, String>> entrySet() {
		try {
			final var kvps = this.wbClient.pGet(this.rootKey + "/?", String.class).get();
			return kvps.stream().map(this::toEntry).collect(Collectors.toSet());
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			return Collections.emptySet();
		} catch (final ExecutionException e) {
			throw new IllegalStateException(e);
		}
	}

	private Map.Entry<String, String> toEntry(final KeyValuePair<String> kvp) {
		return new Map.Entry<>() {

			@Override
			public String getKey() {
				return Utils.unescape(kvp.getKey());
			}

			@Override
			public String getValue() {
				return Utils.unescape(kvp.getValue());
			}

			@Override
			public String setValue(final String value) {
				return WorterbuchBiMap.this.put(kvp.getKey(), value);
			}
		};
	}

	class InvertedWorterbuchBiMap implements BiMap<String, String> {

		@Override
		public int size() {
			return WorterbuchBiMap.this.size();
		}

		@Override
		public boolean isEmpty() {
			return WorterbuchBiMap.this.isEmpty();
		}

		@Override
		public boolean containsKey(final Object key) {
			return WorterbuchBiMap.this.containsValue(key);
		}

		@Override
		public boolean containsValue(final Object value) {
			return WorterbuchBiMap.this.containsKey(value);
		}

		@Override
		public String get(final Object key) {
			try {
				final var kvps = WorterbuchBiMap.this.wbClient.pGet(WorterbuchBiMap.this.rootKey + "/#", String.class)
						.get();
				return kvps.stream().filter(e -> Objects.equals(e.getValue(), key)).map(KeyValuePair::getKey).findAny()
						.orElse(null);
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
				return null;
			} catch (final ExecutionException e) {
				throw new IllegalStateException(e);
			}
		}

		@Override
		public String put(final String key, final String value) {
			return WorterbuchBiMap.this.put(value, key);
		}

		@Override
		public String forcePut(final String key, final String value) {
			return WorterbuchBiMap.this.put(value, key);
		}

		@Override
		public String remove(final Object key) {
			try {
				final var kvps = WorterbuchBiMap.this.wbClient.pGet(WorterbuchBiMap.this.rootKey + "/#", String.class)
						.get();
				final var kvp = kvps.stream().filter(e -> Objects.equals(e.getValue(), key)).findAny();
				if (kvp.isPresent()) {
					final var invKey = kvp.get().getKey();
					WorterbuchBiMap.this.wbClient.delete(invKey, String.class);
					return invKey;
				} else {
					return null;
				}
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
				return null;
			} catch (final ExecutionException e) {
				throw new IllegalStateException(e);
			}
		}

		@Override
		public void putAll(final Map<? extends String, ? extends String> m) {
			m.forEach((k, v) -> WorterbuchBiMap.this.put(v, k));
		}

		@Override
		public void clear() {
			WorterbuchBiMap.this.clear();
		}

		@Override
		public Set<String> keySet() {
			return new HashSet<>(WorterbuchBiMap.this.values());
		}

		@Override
		public Set<String> values() {
			return WorterbuchBiMap.this.keySet();
		}

		@Override
		public BiMap<String, String> inverse() {
			return WorterbuchBiMap.this;
		}

		@Override
		public Set<Entry<String, String>> entrySet() {
			try {
				final var kvps = WorterbuchBiMap.this.wbClient.pGet(WorterbuchBiMap.this.rootKey + "/?", String.class)
						.get();
				return kvps.stream().map(this::toEntry).collect(Collectors.toSet());
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
				return Collections.emptySet();
			} catch (final ExecutionException e) {
				throw new IllegalStateException(e);
			}
		}

		private Map.Entry<String, String> toEntry(final KeyValuePair<String> kvp) {
			return new Map.Entry<>() {

				@Override
				public String getKey() {
					return Utils.unescape(kvp.getValue());
				}

				@Override
				public String getValue() {
					return Utils.unescape(kvp.getKey());
				}

				@Override
				public String setValue(final String value) {
					return WorterbuchBiMap.this.put(value, kvp.getKey());
				}
			};
		}
	}
}
