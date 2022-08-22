package net.bbmsoft.worterbuch.client.demo;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bbmsoft.worterbuch.client.api.AsyncWorterbuchClient;
import net.bbmsoft.worterbuch.client.api.AsyncWorterbuchClient.Event;

@Component(property = { "osgi.command.scope=wb", "osgi.command.function=get", "osgi.command.function=pget",
		"osgi.command.function=set", "osgi.command.function=sub", "osgi.command.function=psub",
		"osgi.command.function=subu",
		"osgi.command.function=psubu" }, service = WorterbuchClientDemo.class, immediate = true)
public class WorterbuchClientDemo {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Reference
	private AsyncWorterbuchClient client;

	@Activate
	public void activate() throws URISyntaxException {

		final var host = Optional.ofNullable(System.getenv("WORTERBUCH_HOST_ADDRESS")).orElse("localhost");
		final var port = Optional.ofNullable(System.getenv("WORTERBUCH_TCP_PORT")).orElse("4242");

		final var uri = new URI("tcp://" + host + ":" + port);

		try {
			this.client.onConnectionLost(this::shutdown);
			var handshake = this.client.connect(uri).get();
			this.log.info("Successfully connected: {}", handshake);
		} catch (InterruptedException | ExecutionException e) {
			this.log.error("Could not establish connection:", e);
			return;
		}
	}

	@Deactivate
	public void deactivate() throws InterruptedException, ExecutionException, TimeoutException {
		this.client.close();
	}

	public String get(final String key) throws InterruptedException, ExecutionException {
		final var result = this.client.get(key).get();
		return result.isEmpty() ? "No value" : key + "=" + result.get();
	}

	public String pget(final String pattern) throws InterruptedException, ExecutionException {
		final var result = this.client.pget(pattern).get();
		final var sb = new StringBuilder();
		result.forEach((k, v) -> sb.append(k).append("=").append(v).append("\n"));
		return sb.toString();
	}

	public String set(final String key, final String value) throws InterruptedException, ExecutionException {
		this.client.set(key, value).get();
		return "Ok";
	}

	public String sub(final String key) throws InterruptedException, ExecutionException {
		this.client.subscribe(key, e -> e.ifPresent(this::logEvent), false).get();
		return "Ok";
	}

	public String psub(final String pattern) throws InterruptedException, ExecutionException {
		this.client.psubscribe(pattern, e -> e.ifPresent(this::logEvent), false).get();
		return "Ok";
	}

	public String subu(final String key) throws InterruptedException, ExecutionException {
		this.client.subscribe(key, e -> e.ifPresent(this::logEvent), true).get();
		return "Ok";
	}

	public String psubu(final String pattern) throws InterruptedException, ExecutionException {
		this.client.psubscribe(pattern, e -> e.ifPresent(this::logEvent), true).get();
		return "Ok";
	}

	private void shutdown() {

		final var bundle = FrameworkUtil.getBundle(this.getClass());
		if (bundle != null) {
			final var ctx = bundle.getBundleContext();
			final var systemBundle = ctx.getBundle(0);
			try {
				systemBundle.stop();
			} catch (final BundleException e) {
				this.log.error("Failed to stop system bundle:", e);
			}
		}
	}

	private void logEvent(final Event event) {
		System.out.println("Event: " + event.key() + " = " + event.value());
	}

}
