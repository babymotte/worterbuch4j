package net.bbmsoft.worterbuch.caching.client;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import net.bbmsoft.worterbuch.client.api.AsyncWorterbuchClient;
import net.bbmsoft.worterbuch.client.api.AsyncWorterbuchClient.Event;
import net.bbmsoft.worterbuch.client.api.AsyncWorterbuchClient.Void;
import net.bbmsoft.worterbuch.client.api.CachingWorterbuchClient;

@Component
public class WorterbuchCachingClient implements CachingWorterbuchClient {

	private final Set<String> subscribedKeys;
	private final Map<String, String> cache;
	private final Map<String, Queue<Consumer<String>>> subscribers;
	private final Object subscribersLock;

	public WorterbuchCachingClient() {
		this.subscribedKeys = ConcurrentHashMap.newKeySet();
		this.cache = new ConcurrentHashMap<>();
		this.subscribers = new ConcurrentHashMap<>();
		this.subscribersLock = new Object();
	}

	@Reference
	private AsyncWorterbuchClient delegateClient;

	@Override
	public String get(final String key) throws InterruptedException {

		final var value = this.cache.get(key);

		if (value != null) {
			return value;
		}

		final var latch = new CountDownLatch(1);
		final Consumer<String> callback = v -> latch.countDown();
		this.subscribe(key, callback);
		latch.await();
		this.removeCallback(key, callback);
		return this.cache.get(key);
	}

	@Override
	public void set(final String key, final String value) {
		this.delegateClient.set(key, value);
	}

	@Override
	public void subscribe(final String key, final Consumer<String> onValue) {

		var subscribers = this.subscribers.get(key);
		if (subscribers == null) {
			// check-then-act, so we need a synschronized block
			synchronized (this.subscribersLock) {
				// make sure no queue was inserted before we entered the synchronized block
				subscribers = this.subscribers.get(key);
				if (subscribers == null) {
					this.subscribers.put(key, subscribers = new LinkedBlockingQueue<>());
				}
			}
		}
		subscribers.add(onValue);
		this.addSubscription(key);
	}

	@Override
	public Future<Void> connect(final URI uri) {
		return this.delegateClient.connect(uri);
	}

	@Override
	public Future<Boolean> disconnect() {
		return this.delegateClient.disconnect();
	}

	@Override
	public List<String> supportedProtocols() {
		return this.delegateClient.supportedProtocols();
	}

	@Override
	public void onConnectionLost(final Runnable action) {
		this.delegateClient.onConnectionLost(action);
	}

	private void addSubscription(final String key) {
		final var needsSubscription = this.subscribedKeys.add(key);
		if (needsSubscription) {
			this.delegateClient.subscribe(key, this::insertValue);
		}
	}

	private void insertValue(final Optional<Event> event) {
		event.ifPresent(e -> {
			this.cache.put(e.key(), e.value());
			final var subscribers = this.subscribers.get(e.key());
			if (subscribers != null) {
				for (final var subscriber : subscribers) {
					subscriber.accept(e.value());
				}
			}
		});
	}

	private void removeCallback(final String key, final Consumer<String> callback) {
		final var subscribers = this.subscribers.get(key);
		// subscribers cannot be null at this point, since we just made a subscription
		subscribers.remove(callback);
	}

}
