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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

import org.osgi.annotation.versioning.ProviderType;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.bbmsoft.worterbuch.client.api.util.Tuple;
import net.bbmsoft.worterbuch.client.model.KeyValuePair;

@ProviderType
public interface WorterbuchClient extends AutoCloseable {

	<T> void set(String key, T value);

	<T> void publish(String key, T value);

	<T> CompletableFuture<Long> initPubStream(String key);

	<T> void streamPub(long transactionId, T value);

	<T> CompletableFuture<Optional<T>> get(String key, Class<T> type);

	<T> CompletableFuture<Optional<T[]>> getArray(String key, Class<T> elementType);

	<T> CompletableFuture<List<TypedKeyValuePair<T>>> pGet(String pattern, Class<T> type);

	<T> CompletableFuture<Optional<T>> delete(String key, Class<T> type);

	void delete(String key);

	<T> CompletableFuture<List<TypedKeyValuePair<T>>> pDelete(String pattern, Class<T> type);

	void pDelete(String pattern);

	CompletableFuture<List<String>> ls(String parent);

	CompletableFuture<List<String>> pLs(String parentPattern);

	<T> long subscribe(String key, boolean unique, boolean liveOnly, Class<T> type, Consumer<Optional<T>> callback);

	default <T> long subscribe(final String key, final boolean unique, final boolean liveOnly, final Class<T> type,
			final Consumer<Optional<T>> callback, final Consumer<? super Throwable> onError, final Executor executor) {
		return this.subscribe(key, unique, liveOnly, type, v -> executor.execute(() -> callback.accept(v)));
	}

	<T> long subscribeArray(String key, boolean unique, boolean liveOnly, Class<T> elementType,
			Consumer<Optional<T[]>> callback);

	default <T> long subscribeArray(final String key, final boolean unique, final boolean liveOnly,
			final Class<T> elementType, final Consumer<Optional<T[]>> callback,
			final Consumer<? super Throwable> onError, final Executor executor) {
		return this.subscribeArray(key, unique, liveOnly, elementType, v -> executor.execute(() -> callback.accept(v)));
	}

	<T> long pSubscribe(String pattern, boolean unique, boolean liveOnly, Optional<Long> aggregateEvents, Class<T> type,
			Consumer<TypedPStateEvent<T>> callback);

	default <T> long pSubscribe(final String pattern, final boolean unique, final boolean liveOnly,
			final Optional<Long> aggregateEvents, final Class<T> type, final Consumer<TypedPStateEvent<T>> callback,
			final Executor executor) {
		return this.pSubscribe(pattern, unique, liveOnly, aggregateEvents, type,
				v -> executor.execute(() -> callback.accept(v)));
	}

	void unsubscribe(long transactionId);

	long subscribeLs(String parent, Consumer<List<String>> callback);

	default long subscribeLs(final String parent, final Consumer<List<String>> callback,
			final Consumer<? super Throwable> onError, final Executor executor) {
		return this.subscribeLs(parent, v -> executor.execute(() -> callback.accept(v)));
	}

	void unsubscribeLs(long transactionId);

	ObjectMapper getObjectMapper();

	String getClientId();

	CompletableFuture<Optional<String[]>> getGraveGoods();

	CompletableFuture<Optional<KeyValuePair[]>> getLastWill();

	void setGraveGoods(String[] graveGoods);

	void setLastWill(KeyValuePair[] lastWill);

	void setClientName(String name);

	<T> CompletableFuture<Boolean> cSet(String key, T value, long version);

	<T> CompletableFuture<Tuple<T, Long>> cGet(String key, T value, Class<T> type);

	<T> void update(String key, Function<T, T> transform, Class<T> type);

}