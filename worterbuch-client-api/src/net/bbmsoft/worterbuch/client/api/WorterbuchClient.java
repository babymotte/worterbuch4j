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
import java.util.concurrent.Future;
import java.util.function.Consumer;

import org.osgi.annotation.versioning.ProviderType;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.bbmsoft.worterbuch.client.model.KeyValuePair;

@ProviderType
public interface WorterbuchClient {

	void close();

	<T> long set(String key, T value, Consumer<? super Throwable> onError);

	<T> long publish(String key, T value, Consumer<? super Throwable> onError);

	<T> Future<Long> initPubStream(String key, Consumer<? super Throwable> onError);

	<T> long initPubStream(String key, Consumer<Long> callback, Consumer<? super Throwable> onError);

	<T> void streamPub(long transactionId, T value, Consumer<? super Throwable> onError);

	<T> Future<Optional<T>> get(String key, Class<T> type);

	<T> long get(String key, Class<T> type, Consumer<Optional<T>> callback, Consumer<? super Throwable> onError);

	<T> Future<Optional<T[]>> getArray(String key, Class<T> elementType);

	<T> long getArray(String key, Class<T> elementType, Consumer<Optional<T[]>> callback,
			Consumer<? super Throwable> onError);

	<T> Future<List<TypedKeyValuePair<T>>> pGet(String pattern, Class<T> type);

	<T> long pGet(String pattern, Class<T> type, Consumer<List<TypedKeyValuePair<T>>> callback,
			Consumer<? super Throwable> onError);

	<T> Future<Optional<T>> delete(String key, Class<T> type);

	<T> long delete(String key, Class<T> type, Consumer<Optional<T>> callback, Consumer<? super Throwable> onError);

	long delete(String key, Consumer<? super Throwable> onError);

	<T> Future<List<TypedKeyValuePair<T>>> pDelete(String pattern, Class<T> type);

	Future<Void> pDelete(String pattern);

	<T> long pDelete(String pattern, Class<T> type, Consumer<List<TypedKeyValuePair<T>>> callback,
			Consumer<? super Throwable> onError);

	long pDelete(String pattern, Runnable callback, Consumer<? super Throwable> onError);

	Future<List<String>> ls(String parent);

	long ls(String parent, Consumer<List<String>> callback, Consumer<? super Throwable> onError);

	Future<List<String>> pLs(String parentPattern);

	long pLs(String parentPattern, Consumer<List<String>> callback, Consumer<? super Throwable> onError);

	<T> long subscribe(String key, boolean unique, boolean liveOnly, Class<T> type, Consumer<Optional<T>> callback,
			Consumer<? super Throwable> onError);

	<T> long subscribeArray(String key, boolean unique, boolean liveOnly, Class<T> elementType,
			Consumer<Optional<T[]>> callback, Consumer<? super Throwable> onError);

	<T> long pSubscribe(String pattern, boolean unique, boolean liveOnly, Optional<Long> aggregateEvents, Class<T> type,
			Consumer<TypedPStateEvent<T>> callback, Consumer<? super Throwable> onError);

	void unsubscribe(long transactionId, Consumer<? super Throwable> onError);

	long subscribeLs(String parent, Consumer<List<String>> callback, Consumer<? super Throwable> onError);

	void unsubscribeLs(long transactionId, Consumer<? super Throwable> onError);

	ObjectMapper getObjectMapper();

	String getClientId();

	Future<Optional<String[]>> getGraveGoods();

	Future<Optional<KeyValuePair[]>> getLastWill();

	long setGraveGoods(String[] graveGoods, Consumer<? super Throwable> onError);

	long setLastWill(KeyValuePair[] lastWill, Consumer<? super Throwable> onError);

	long setClientName(String name);

}