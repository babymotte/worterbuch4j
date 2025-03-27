package net.bbmsoft.worterbuch.client.collections;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.type.MapLikeType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import net.bbmsoft.worterbuch.client.api.WorterbuchClient;

public class WbMap<T> implements Map<String, T> {

	private final WorterbuchClient wbClient;
	private final String mapKey;
	private final MapLikeType type;

	public WbMap(final WorterbuchClient wbClient, final String application, final String namespace,
			final String mapName, final Class<T> valueType) {
		this.wbClient = wbClient;
		this.mapKey = application + "/state/" + namespace + "/" + mapName;
		this.type = TypeFactory.defaultInstance().constructMapLikeType(HashMap.class, String.class, valueType);
	}

	@Override
	public int size() {
		try {
			return this.wbClient.<Map<String, T>>get(this.mapKey, this.type).result().get().get().size();
		} catch (InterruptedException | ExecutionException e) {
			return 0;
		}
	}

	@Override
	public boolean isEmpty() {
		try {
			return this.wbClient.<Map<String, T>>get(this.mapKey, this.type).result().get().get().isEmpty();
		} catch (InterruptedException | ExecutionException e) {
			return true;
		}
	}

	@Override
	public boolean containsKey(final Object key) {
		try {
			return this.wbClient.<Map<String, T>>get(this.mapKey, this.type).result().get().get().containsKey(key);
		} catch (InterruptedException | ExecutionException e) {
			return false;
		}
	}

	@Override
	public boolean containsValue(final Object value) {
		try {
			return this.wbClient.<Map<String, T>>get(this.mapKey, this.type).result().get().get().containsValue(value);
		} catch (InterruptedException | ExecutionException e) {
			return false;
		}
	}

	@Override
	public T get(final Object key) {
		try {
			return this.wbClient.<Map<String, T>>get(this.mapKey, this.type).result().get().get().get(key);
		} catch (InterruptedException | ExecutionException e) {
			return null;
		}
	}

	@Override
	public T put(final String key, final T value) {
		final var prev = new AtomicReference<T>();
		this.wbClient.<Map<String, T>>update(this.mapKey, HashMap::new, m -> prev.set(m.put(key, value)), this.type);
		return prev.get();
	}

	@Override
	public T remove(final Object key) {
		final var item = new AtomicReference<T>();
		this.wbClient.<Map<String, T>>update(this.mapKey, HashMap::new, m -> item.set(m.remove(key)), this.type);
		return item.get();
	}

	@Override
	public void putAll(final Map<? extends String, ? extends T> other) {
		this.wbClient.<Map<String, T>>update(this.mapKey, HashMap::new, m -> m.putAll(other), this.type);
	}

	@Override
	public void clear() {
		this.wbClient.<Map<String, T>>update(this.mapKey, HashMap::new, (Consumer<Map<String, T>>) Map::clear,
				this.type);
	}

	@Override
	public Set<String> keySet() {
		try {
			return this.wbClient.<Map<String, T>>get(this.mapKey, this.type).result().get().get().keySet();
		} catch (InterruptedException | ExecutionException e) {
			return Collections.emptySet();
		}
	}

	@Override
	public Collection<T> values() {
		try {
			return this.wbClient.<Map<String, T>>get(this.mapKey, this.type).result().get().get().values();
		} catch (InterruptedException | ExecutionException e) {
			return Collections.emptyList();
		}
	}

	@Override
	public Set<Entry<String, T>> entrySet() {
		try {
			return this.wbClient.<Map<String, T>>get(this.mapKey, this.type).result().get().get().entrySet();
		} catch (InterruptedException | ExecutionException e) {
			return Collections.emptySet();
		}
	}

}
