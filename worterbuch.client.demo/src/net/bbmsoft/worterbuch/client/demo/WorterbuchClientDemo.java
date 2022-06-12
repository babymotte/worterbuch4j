package net.bbmsoft.worterbuch.client.demo;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bbmsoft.worterbuch.client.api.AsyncWorterbuchClient;

@Component(property = { "osgi.command.scope=wb", "osgi.command.function=get", "osgi.command.function=set",
		"osgi.command.function=sub" }, service = WorterbuchClientDemo.class, immediate = true)
public class WorterbuchClientDemo {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Reference
	private AsyncWorterbuchClient client;

	@Activate
	public void activate() throws URISyntaxException {
		final var uri = new URI("tcp://localhost:4242");

		try {
			this.client.connect(uri).get();
		} catch (InterruptedException | ExecutionException e) {
			this.log.error("Could not establish connection:", e);
			return;
		}
	}

	@Deactivate
	public void deactivate() throws InterruptedException, ExecutionException, TimeoutException {
		this.client.close();
	}

	public String get(final String pattern) throws InterruptedException, ExecutionException {
		final var result = this.client.get(pattern).get();
		final var sb = new StringBuilder();
		result.forEach((k, v) -> sb.append(k).append("=").append(v).append("\n"));
		return sb.toString();
	}

	public void set(final String key, final String value) throws InterruptedException, ExecutionException {
		this.client.set(key, value).get();
	}

	public void sub(final String pattern) throws InterruptedException, ExecutionException {
		final var events = this.client.subscribe(pattern).get();
		new Thread(() -> {
			while (!Thread.currentThread().isInterrupted()) {
				try {
					final var event = events.take();
					if (event.isEmpty()) {
						break;
					} else {
						final var theEvent = event.get();
						this.log.info("Received event: {} = {}", theEvent.key(), theEvent.value());
					}
				} catch (final InterruptedException e) {
					break;
				}
			}
		}, "subscription - " + pattern).start();
	}
}
