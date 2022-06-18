package net.bbmsoft.worterbuch.caching.client.demo;

import java.net.URI;
import java.net.URISyntaxException;
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

import net.bbmsoft.worterbuch.client.api.CachingWorterbuchClient;

@Component(property = { "osgi.command.scope=wb", "osgi.command.function=get", "osgi.command.function=set",
		"osgi.command.function=sub" }, service = WorterbuchCachingClientDemo.class, immediate = true)
public class WorterbuchCachingClientDemo {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Reference
	private CachingWorterbuchClient client;

	@Activate
	public void activate() throws URISyntaxException {
		final var uri = new URI("tcp://localhost:4242");

		try {
			this.client.onConnectionLost(this::shutdown);
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

	public String get(final String key) throws InterruptedException {
		final var result = this.client.get(key);
		return result;
	}

	public String set(final String key, final String value) {
		this.client.set(key, value);
		return "Ok";
	}

	public void sub(final String key) {
		this.client.subscribe(key, v -> System.out.println(key + " = " + v));
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

}
