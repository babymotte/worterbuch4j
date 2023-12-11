package worterbuch.client.demo;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bbmsoft.worterbuch.client.KeyValuePair;
import net.bbmsoft.worterbuch.client.WorterbuchClient;
import net.bbmsoft.worterbuch.client.WorterbuchException;
import net.bbmsoft.worterbuch.client.collections.AsyncWorterbuchList;
import net.bbmsoft.worterbuch.client.collections.WorterbuchMap;

@Component(service = ClientDemo.class, immediate = true, property = { "osgi.command.scope=wbdemo",
		"osgi.command.function=put","osgi.command.function=rm", "osgi.command.function=print" })
public class ClientDemo {

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private volatile BundleContext ctx;
	private volatile boolean running;
	private volatile Thread thread;
	private WorterbuchMap<String> map;

	static class HelloWorld {
		public String greeting;
		public String gretee;

		public HelloWorld() {
		}

		public HelloWorld(final String greeting, final String gretee) {
			this.greeting = greeting;
			this.gretee = gretee;
		}

		@Override
		public String toString() {
			return this.greeting + ", " + this.gretee + "!";
		}
	}

	@Activate
	public void activate(final BundleContext ctx)
			throws URISyntaxException, WorterbuchException, InterruptedException, ExecutionException, TimeoutException {
		this.ctx = ctx;
		this.running = true;

		this.thread = new Thread(() -> {
			try {
				this.run();
			} catch (ExecutionException | InterruptedException | URISyntaxException | TimeoutException e) {
				this.error(e);
			}
		});
		this.thread.start();

	}

	@Deactivate
	public void deactivate() {
		this.running = false;
		this.thread.interrupt();
	}

	private void run() throws ExecutionException, URISyntaxException, TimeoutException, InterruptedException {

		final var uri = new URI("tcp://localhost:8081");

		final var wb = WorterbuchClient.connect(uri, Arrays.asList("clientDemo/#"),
				Arrays.asList(KeyValuePair.of("clientDemo/lastWill", "nein")), this::exit, this::error);

		this.map = new WorterbuchMap<>(wb, "demo", "test", "myMap", String.class, this::error);

		this.map.addListener((k, v) -> {
			log.info("{} -> {}", k, v);
		}, Executors.newSingleThreadExecutor(r -> new Thread(r, "Listener thread")));

		final var list = new AsyncWorterbuchList<>(wb, "testapp", "collections", "asyncList", HelloWorld.class,
				this::error);

		var counter = list.size() - 1;
		var inverted = counter >= 2;
		while (this.running) {

			if (counter < 0) {
				counter = 0;
				inverted = false;
			}

			if (inverted) {
				list.remove(counter);
			} else {
				switch (list.size()) {
				case 0 -> list.add(new HelloWorld("Hello", "World"));
				case 1 -> list.add(new HelloWorld("Hello", "There"));
				default -> list.add(new HelloWorld("General", "Kenobi"));
				}
			}

			counter = list.size() - 1;

			if (counter >= 2) {
				inverted = true;
			}

			try {
				Thread.sleep(1000);
			} catch (final InterruptedException e) {
				break;
			}
		}

		wb.close();
	}

	private void exit(final Integer errorCode, final String message) {
		this.log.error("Disconnected: {} ({})", message, errorCode);
		if (this.ctx != null) {
			final var sys = this.ctx.getBundle(0);
			if (sys != null) {
				try {
					sys.stop();
				} catch (final BundleException e) {
					this.log.error("Error stopping system bundle:", e);
				}
			}
		}
	}

	private void error(final Throwable th) {
		log.error("Error:", th);
		this.exit(-1, th.getMessage());
	}

	public void put(final String key, final String value) {
		this.map.put(key, value);
	}

	public void rm(final String key) {
		this.map.remove(key);
	}

	public void print() {
		this.map.forEach((k, v) -> log.info(k + " -> " + v));
	}

}
