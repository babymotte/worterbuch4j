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

package net.bbmsoft.worterbuch.client.api;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.osgi.annotation.versioning.ProviderType;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.bbmsoft.worterbuch.client.api.util.Tuple;
import net.bbmsoft.worterbuch.client.api.util.type.TypeUtil;
import net.bbmsoft.worterbuch.client.error.Future;
import net.bbmsoft.worterbuch.client.model.KeyValuePair;

@ProviderType
public interface WorterbuchClient extends AutoCloseable {

	<T> Future<Void> set(String key, T value);

	<T> Future<Void> publish(String key, T value);

	<T> Future<Void> initPubStream(String key);

	<T> Future<Void> streamPub(long transactionId, T value);

	default <T> Future<T> get(final String key, final Class<T> type) {
		return this.get(key, (Type) type);
	}

	<T> Future<T> get(String key, Type type);

	default <T> Future<List<T>> getList(final String key, final Class<T> elementType) {
		return this.get(key, TypeUtil.listType(elementType));
	}

	default <T> Future<List<TypedKeyValuePair<T>>> pGet(final String pattern, final Class<T> type) {
		return this.pGet(pattern, (Type) type);
	}

	<T> Future<List<TypedKeyValuePair<T>>> pGet(String pattern, Type type);

	default <T> Future<T> delete(final String key, final Class<T> type) {
		return this.delete(key, (Type) type);
	}

	<T> Future<T> delete(String key, Type type);

	Future<Void> delete(String key);

	default <T> Future<List<TypedKeyValuePair<T>>> pDelete(final String pattern, final Class<T> type) {
		return this.pDelete(pattern, (Type) type);
	}

	<T> Future<List<TypedKeyValuePair<T>>> pDelete(String pattern, Type type);

	Future<Void> pDelete(String pattern);

	Future<List<String>> ls(String parent);

	Future<List<String>> pLs(String parentPattern);

	default <T> Future<Void> subscribe(final String key, final boolean unique, final boolean liveOnly,
			final Class<T> type, final Consumer<Optional<T>> callback) {
		return this.subscribe(key, unique, liveOnly, (Type) type, callback);
	}

	<T> Future<Void> subscribe(String key, boolean unique, boolean liveOnly, Type type, Consumer<Optional<T>> callback);

	default <T> Future<Void> subscribe(final String key, final boolean unique, final boolean liveOnly,
			final Class<T> type, final Consumer<Optional<T>> callback, final Consumer<? super Throwable> onError,
			final Executor executor) {
		return this.subscribe(key, unique, liveOnly, type, v -> executor.execute(() -> callback.accept(v)));
	}

	default <T> Future<Void> subscribe(final String key, final boolean unique, final boolean liveOnly, final Type type,
			final Consumer<Optional<T>> callback, final Consumer<? super Throwable> onError, final Executor executor) {
		return this.<T>subscribe(key, unique, liveOnly, type, v -> executor.execute(() -> callback.accept(v)));
	}

	default <T> Future<Void> subscribeList(final String key, final boolean unique, final boolean liveOnly,
			final Class<T> elementType, final Consumer<Optional<List<T>>> callback) {
		return this.subscribe(key, unique, liveOnly, TypeUtil.listType(elementType), callback);
	}

	default <T> Future<Void> subscribeList(final String key, final boolean unique, final boolean liveOnly,
			final Class<T> elementType, final Consumer<Optional<List<T>>> callback,
			final Consumer<? super Throwable> onError, final Executor executor) {
		return this.subscribeList(key, unique, liveOnly, elementType, v -> executor.execute(() -> callback.accept(v)));
	}

	default <T> Future<Void> pSubscribe(final String pattern, final boolean unique, final boolean liveOnly,
			final Optional<Long> aggregateEvents, final Class<T> type, final Consumer<TypedPStateEvent<T>> callback) {
		return this.pSubscribe(pattern, unique, liveOnly, aggregateEvents, (Type) type, callback);
	}

	default <T> Future<Void> pSubscribe(final String pattern, final boolean unique, final boolean liveOnly,
			final Optional<Long> aggregateEvents, final Class<T> type, final Consumer<TypedPStateEvent<T>> callback,
			final Executor executor) {
		return this.pSubscribe(pattern, unique, liveOnly, aggregateEvents, type,
				v -> executor.execute(() -> callback.accept(v)));
	}

	<T> Future<Void> pSubscribe(String pattern, boolean unique, boolean liveOnly, Optional<Long> aggregateEvents,
			Type type, Consumer<TypedPStateEvent<T>> callback);

	default <T> Future<Void> pSubscribe(final String pattern, final boolean unique, final boolean liveOnly,
			final Optional<Long> aggregateEvents, final Type type, final Consumer<TypedPStateEvent<T>> callback,
			final Executor executor) {
		return this.<T>pSubscribe(pattern, unique, liveOnly, aggregateEvents, type,
				v -> executor.execute(() -> callback.accept(v)));
	}

	Future<Void> unsubscribe(long transactionId);

	Future<Void> subscribeLs(String parent, Consumer<List<String>> callback);

	default Future<Void> subscribeLs(final String parent, final Consumer<List<String>> callback,
			final Consumer<? super Throwable> onError, final Executor executor) {
		return this.subscribeLs(parent, v -> executor.execute(() -> callback.accept(v)));
	}

	Future<Void> unsubscribeLs(long transactionId);

	<T> Future<Void> cSet(String key, T value, long version);

	default <T> Future<Tuple<T, Long>> cGet(final String key, final Class<T> type) {
		return this.cGet(key, (Type) type);
	}

	<T> Future<Tuple<T, Long>> cGet(String key, Type type);

	default <T, V> boolean update(final String key, final Function<Optional<T>, V> transform, final Class<T> type) {
		return this.update(key, transform, (Type) type);
	}

	<T, V> boolean update(String key, Function<Optional<T>, V> transform, Type type);

	default <T> boolean update(final String key, final Supplier<T> seed, final Consumer<T> update,
			final Class<T> type) {
		return this.update(key, seed, update, (Type) type);
	}

	default <T> boolean update(final String key, final Supplier<T> seed, final Consumer<T> update, final Type type) {
		return this.<T, T>update(key, mi -> {
			final var i = mi.orElseGet(seed);
			update.accept(i);
			return i;
		}, type);
	}

	default <T> boolean updateList(final String key, final Consumer<List<T>> update, final Class<T> elementType) {
		return this.update(key, ArrayList::new, update, TypeUtil.listType(elementType));
	}

	Future<Void> lock(String key);

	Future<Void> acquireLock(String key);

	Future<Void> releaseLock(String key);

	Future<List<String>> getGraveGoods();

	Future<List<KeyValuePair>> getLastWill();

	Future<Void> setGraveGoods(List<String> graveGoods);

	Future<Void> setLastWill(List<KeyValuePair> lastWill);

	void updateGraveGoods(Consumer<List<String>> update);

	void updateLastWill(Consumer<List<KeyValuePair>> update);

	Future<Void> setClientName(String name);

	ObjectMapper getObjectMapper();

	String getClientId();

}