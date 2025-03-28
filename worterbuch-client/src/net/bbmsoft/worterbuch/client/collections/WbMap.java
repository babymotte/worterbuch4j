package net.bbmsoft.worterbuch.client.collections;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.core.type.TypeReference;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bbmsoft.worterbuch.client.api.WorterbuchClient;
import net.bbmsoft.worterbuch.client.api.util.type.TypeUtil;

public class WbMap<T> implements Map<String, T> {

	private final WorterbuchClient wbClient;
	private final String mapKey;
	private final TypeReference<Map<String, T>> type;

	@SuppressFBWarnings("EI_EXPOSE_REP2")
	public WbMap(final WorterbuchClient wbClient, final String application, final String namespace,
			final String mapName, final Class<T> valueType) {
		this.wbClient = wbClient;
		this.mapKey = application + "/state/" + namespace + "/" + mapName;
		this.type = TypeUtil.map(valueType);
	}

	@Override
	public int size() {
		try {
			return this.wbClient.get(this.mapKey, this.type).responseFuture().get().value().size();
		} catch (InterruptedException | ExecutionException e) {
			return 0;
		}
	}

	@Override
	public boolean isEmpty() {
		try {
			return this.wbClient.get(this.mapKey, this.type).responseFuture().get().value().isEmpty();
		} catch (InterruptedException | ExecutionException e) {
			return true;
		}
	}

	@Override
	public boolean containsKey(final Object key) {
		try {
			return this.wbClient.get(this.mapKey, this.type).responseFuture().get().value().containsKey(key);
		} catch (InterruptedException | ExecutionException e) {
			return false;
		}
	}

	@Override
	public boolean containsValue(final Object value) {
		try {
			return this.wbClient.get(this.mapKey, this.type).responseFuture().get().value().containsValue(value);
		} catch (InterruptedException | ExecutionException e) {
			return false;
		}
	}

	@Override
	public T get(final Object key) {
		try {
			return this.wbClient.get(this.mapKey, this.type).responseFuture().get().value().get(key);
		} catch (InterruptedException | ExecutionException e) {
			return null;
		}
	}

	@Override
	public T put(final String key, final T value) {
		final var prev = new AtomicReference<T>();
		this.wbClient.update(this.mapKey, m -> {
			prev.set(m.put(key, value));
		}, this.type);
		return prev.get();
	}

	@Override
	public T remove(final Object key) {
		final var item = new AtomicReference<T>();
		this.wbClient.update(this.mapKey, m -> {
			item.set(m.remove(key));
		}, this.type);
		return item.get();
	}

	@Override
	public void putAll(final Map<? extends String, ? extends T> other) {
		this.wbClient.update(this.mapKey, m -> {
			m.putAll(other);
		}, this.type);
	}

	@Override
	public void clear() {
		this.wbClient.update(this.mapKey, m -> {
			m.clear();
		}, this.type);
	}

	@Override
	public Set<String> keySet() {
		try {
			return this.wbClient.get(this.mapKey, this.type).responseFuture().get().value().keySet();
		} catch (InterruptedException | ExecutionException e) {
			return Collections.emptySet();
		}
	}

	@Override
	public Collection<T> values() {
		try {
			return this.wbClient.get(this.mapKey, this.type).responseFuture().get().value().values();
		} catch (InterruptedException | ExecutionException e) {
			return Collections.emptyList();
		}
	}

	@Override
	public Set<Entry<String, T>> entrySet() {
		try {
			return this.wbClient.get(this.mapKey, this.type).responseFuture().get().value().entrySet();
		} catch (InterruptedException | ExecutionException e) {
			return Collections.emptySet();
		}
	}

}
