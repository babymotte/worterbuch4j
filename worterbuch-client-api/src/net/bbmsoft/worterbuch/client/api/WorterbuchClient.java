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
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

import org.osgi.annotation.versioning.ProviderType;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.bbmsoft.worterbuch.client.api.util.Tuple;
import net.bbmsoft.worterbuch.client.model.KeyValuePair;
import net.bbmsoft.worterbuch.client.response.Future;

/**
 * A worterbuch client.
 */
@ProviderType
public interface WorterbuchClient extends AutoCloseable {

	/**
	 * Set a retained value in the server's state store.
	 *
	 * @param key   the key for which to set the value
	 * @param value the value to be set
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	Future<Void> set(String key, Object value);

	/**
	 * Publish an ephemeral value. The value will be forwarded to all current
	 * subscribers but will not be stored on the server and thus not be received by
	 * any future subscribers.
	 *
	 * @param key
	 * @param value
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	Future<Void> publish(String key, Object value);

	/**
	 * Initiate a publish stream. This binds the provided key to this transaction's
	 * ID so that subsequently values can be published just by using the transaction
	 * ID, without the need to also provide the key every time.
	 * <p/>
	 * This is an optimization over regular publish operation to reduce network
	 * traffic when repeatedly publishing new values to the same key with high
	 * frequency.
	 *
	 * @param key
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	Future<Void> initPubStream(String key);

	/**
	 * Publish to a previously initiated pblish stream.
	 *
	 * @param transactionId
	 * @param value
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	Future<Void> streamPub(long transactionId, Object value);

	/**
	 * Retrieve the current value of the specified key.
	 *
	 * @param <T>
	 * @param key
	 * @param type
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	<T> Future<T> get(final String key, final Class<T> type);

	/**
	 * Retrieve the current value of the specified key.
	 *
	 * @param <T>
	 * @param key
	 * @param type
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	<T> Future<T> get(final String key, final TypeReference<T> type);

	/**
	 * Retrieve all key/value pairs where the key matches the specified pattern.
	 *
	 * @param <T>
	 * @param pattern
	 * @param type
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	<T> Future<List<TypedKeyValuePair<T>>> pGet(final String pattern, final Class<T> type);

	/**
	 * Retrieve all key/value pairs where the key matches the specified pattern.
	 *
	 * @param <T>
	 * @param pattern
	 * @param type
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	<T> Future<List<TypedKeyValuePair<T>>> pGet(String pattern, TypeReference<T> type);

	/**
	 * Delete a value from the server's state store.
	 *
	 * @param <T>
	 * @param key
	 * @param type
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	<T> Future<T> delete(final String key, final Class<T> type);

	/**
	 * Delete a value from the server's state store.
	 *
	 * @param <T>
	 * @param key
	 * @param type
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	<T> Future<T> delete(String key, TypeReference<T> type);

	/**
	 * Delete a value from the server's state store.
	 *
	 * @param key
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	Future<Void> delete(String key);

	/**
	 * Delete all key/value pairs from the server's state store where the key
	 * matches the provided pattern.
	 *
	 * @param <T>
	 * @param pattern
	 * @param type
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	<T> Future<List<TypedKeyValuePair<T>>> pDelete(final String pattern, final Class<T> type);

	/**
	 * Delete all key/value pairs from the server's state store where the key
	 * matches the provided pattern.
	 *
	 * @param <T>
	 * @param pattern
	 * @param type
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	<T> Future<List<TypedKeyValuePair<T>>> pDelete(String pattern, TypeReference<T> type);

	/**
	 * Delete all key/value pairs from the server's state store where the key
	 * matches the provided pattern.
	 *
	 * @param pattern
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	Future<Void> pDelete(String pattern);

	/**
	 * List all immediate child key segments of the specified partial key.
	 *
	 * @param parent
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	Future<List<String>> ls(String parent);

	/**
	 * List all immediate child key segments of all partial keys matching the
	 * specified pattern.
	 *
	 * @param parentPattern
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	Future<List<String>> pLs(String parentPattern);

	/**
	 * Subscribe to changes of the value of the specified key.
	 *
	 * @param <T>
	 * @param key
	 * @param unique
	 * @param liveOnly
	 * @param type
	 * @param callback
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	<T> Future<Void> subscribe(final String key, final boolean unique, final boolean liveOnly, final Class<T> type,
			final Consumer<Optional<T>> callback);

	/**
	 * Subscribe to changes of the value of the specified key.
	 *
	 * @param <T>
	 * @param key
	 * @param unique
	 * @param liveOnly
	 * @param type
	 * @param callback
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	<T> Future<Void> subscribe(String key, boolean unique, boolean liveOnly, TypeReference<T> type,
			Consumer<Optional<T>> callback);

	/**
	 * Subscribe to changes of the value of the specified key.
	 *
	 * @param <T>
	 * @param key
	 * @param unique
	 * @param liveOnly
	 * @param type
	 * @param callback
	 * @param executor
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	default <T> Future<Void> subscribe(final String key, final boolean unique, final boolean liveOnly,
			final Class<T> type, final Consumer<Optional<T>> callback, final Executor executor) {
		return this.subscribe(key, unique, liveOnly, type, v -> executor.execute(() -> callback.accept(v)));
	}

	/**
	 * Subscribe to changes of the value of the specified key.
	 *
	 * @param <T>
	 * @param key
	 * @param unique
	 * @param liveOnly
	 * @param type
	 * @param callback
	 * @param executor
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	default <T> Future<Void> subscribe(final String key, final boolean unique, final boolean liveOnly,
			final TypeReference<T> type, final Consumer<Optional<T>> callback, final Executor executor) {
		return this.<T>subscribe(key, unique, liveOnly, type, v -> executor.execute(() -> callback.accept(v)));
	}

	/**
	 * Subscribe to changes of the values of all keys matching the specified
	 * pattern.
	 *
	 * @param <T>
	 * @param pattern
	 * @param unique
	 * @param liveOnly
	 * @param aggregateEvents
	 * @param type
	 * @param callback
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	<T> Future<Void> pSubscribe(final String pattern, final boolean unique, final boolean liveOnly,
			final Optional<Long> aggregateEvents, final Class<T> type, final Consumer<TypedPStateEvent<T>> callback);

	/**
	 * Subscribe to changes of the values of all keys matching the specified
	 * pattern.
	 *
	 * @param <T>
	 * @param pattern
	 * @param unique
	 * @param liveOnly
	 * @param aggregateEvents
	 * @param type
	 * @param callback
	 * @param executor
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	default <T> Future<Void> pSubscribe(final String pattern, final boolean unique, final boolean liveOnly,
			final Optional<Long> aggregateEvents, final Class<T> type, final Consumer<TypedPStateEvent<T>> callback,
			final Executor executor) {
		return this.pSubscribe(pattern, unique, liveOnly, aggregateEvents, type,
				v -> executor.execute(() -> callback.accept(v)));
	}

	/**
	 * Subscribe to changes of the values of all keys matching the specified
	 * pattern.
	 *
	 * @param <T>
	 * @param pattern
	 * @param unique
	 * @param liveOnly
	 * @param aggregateEvents
	 * @param type
	 * @param callback
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	<T> Future<Void> pSubscribe(String pattern, boolean unique, boolean liveOnly, Optional<Long> aggregateEvents,
			TypeReference<T> type, Consumer<TypedPStateEvent<T>> callback);

	/**
	 * Subscribe to changes of the values of all keys matching the specified
	 * pattern.
	 *
	 * @param <T>
	 * @param pattern
	 * @param unique
	 * @param liveOnly
	 * @param aggregateEvents
	 * @param type
	 * @param callback
	 * @param executor
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	default <T> Future<Void> pSubscribe(final String pattern, final boolean unique, final boolean liveOnly,
			final Optional<Long> aggregateEvents, final TypeReference<T> type,
			final Consumer<TypedPStateEvent<T>> callback, final Executor executor) {
		return this.<T>pSubscribe(pattern, unique, liveOnly, aggregateEvents, type,
				v -> executor.execute(() -> callback.accept(v)));
	}

	/**
	 * Unsubscribe from changes of the value of the specified key.
	 *
	 * @param transactionId
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	Future<Void> unsubscribe(long transactionId);

	/**
	 * Subscribe to changes of the immediate child keys of the specified partial
	 * key.
	 *
	 * @param parent
	 * @param callback
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	Future<Void> subscribeLs(String parent, Consumer<List<String>> callback);

	/**
	 * Subscribe to changes of the immediate child keys of the specified partial
	 * key.
	 *
	 * @param parent
	 * @param callback
	 * @param executor
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	default Future<Void> subscribeLs(final String parent, final Consumer<List<String>> callback,
			final Executor executor) {
		return this.subscribeLs(parent, v -> executor.execute(() -> callback.accept(v)));
	}

	/**
	 * Unsubscribe from changes of the immediate child keys of the specified partial
	 * key.
	 *
	 * @param transactionId
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	Future<Void> unsubscribeLs(long transactionId);

	/**
	 * Set a value for the provided key using compare-and-swap, where the specified
	 * version is compared to the value's current version on the server.<br/>
	 * If the versions match, the new value is set, otherwise the new value is
	 * rejected with an error.
	 *
	 * @param <T>
	 * @param key
	 * @param value
	 * @param version
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	<T> Future<Void> cSet(String key, T value, long version);

	/**
	 * Retrieve the current value of the specified key along with the value's
	 * current version.
	 *
	 * @param <T>
	 * @param key
	 * @param type
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	<T> Future<Tuple<T, Long>> cGet(final String key, final Class<T> type);

	/**
	 * Retrieve the current value of the specified key along with the value's
	 * current version.
	 *
	 * @param <T>
	 * @param key
	 * @param type
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	<T> Future<Tuple<T, Long>> cGet(String key, TypeReference<T> type);

	/**
	 * Update the value of the specified key using compare-and-swap.
	 *
	 * @param <T>
	 * @param <V>
	 * @param key
	 * @param transform
	 * @param type
	 * @return
	 */
	<T, V> boolean update(final String key, final Function<Optional<T>, V> transform, final Class<T> type);

	/**
	 * Update the value of the specified key using compare-and-swap.
	 *
	 * @param <T>
	 * @param <V>
	 * @param key
	 * @param transform
	 * @param type
	 * @return
	 */
	<T, V> boolean update(String key, Function<Optional<T>, V> transform, TypeReference<T> type);

	/**
	 * Update the value of the specified key using compare-and-swap.
	 *
	 * @param <T>
	 * @param key
	 * @param seed
	 * @param update
	 * @param type
	 * @return
	 */
	<T> boolean update(final String key, final Consumer<T> update, final Class<T> type);

	/**
	 * Update the value of the specified key using compare-and-swap.
	 *
	 * @param <T>
	 * @param key
	 * @param seed
	 * @param update
	 * @param type
	 * @return
	 */
	<T> boolean update(final String key, final Consumer<T> update, final TypeReference<T> type);

	/**
	 * Try to get the lock on the specified key. If no one currently holds the lock
	 * or the lock is held by this client, the server will immediately send a
	 * success message, if the lock is currently held by another client, the server
	 * will immediately send an error message.
	 *
	 * @param key
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	Future<Void> lock(String key);

	/**
	 * Acquire the lock on the specified key, waiting if necessary for the lock to
	 * become available.
	 *
	 * @param key
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	Future<Void> acquireLock(String key);

	/**
	 * Release the lock on the specified key.
	 *
	 * @param key
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	Future<Void> releaseLock(String key);

	/**
	 * Retrieve this client's grave goods from the server.
	 *
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	Future<List<String>> getGraveGoods();

	/**
	 * Retrieve this client's last will from the server.
	 *
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	Future<List<KeyValuePair>> getLastWill();

	/**
	 * Set the grave goods for this client. This will overwrite any previously set
	 * grave goods.
	 *
	 * @param graveGoods
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	Future<Void> setGraveGoods(List<String> graveGoods);

	/**
	 * Set the last will for this client. This will overwrite any previously set
	 * last will.
	 *
	 * @param lastWill
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	Future<Void> setLastWill(List<KeyValuePair> lastWill);

	/**
	 * Change this client's grave goods.
	 *
	 * @param update
	 */
	void updateGraveGoods(Consumer<List<String>> update);

	/**
	 * Change this client's last will.
	 *
	 * @param update
	 */
	void updateLastWill(Consumer<List<KeyValuePair>> update);

	/**
	 * Set a user readable name for this client. This is for debugging purposes
	 * only.
	 *
	 * @param name
	 * @return a future that allows retrieving the transaction ID as well as
	 *         awaiting the server response
	 */
	Future<Void> setClientName(String name);

	/**
	 * Get the Jackson object mapper used by this client. This is intended to allow
	 * registering custom serializers/deserializers if necessary.
	 *
	 * @return
	 */
	ObjectMapper getObjectMapper();

	/**
	 * Get the ID that was assigned to this client by the server.
	 *
	 * @return
	 */
	String getClientId();

}