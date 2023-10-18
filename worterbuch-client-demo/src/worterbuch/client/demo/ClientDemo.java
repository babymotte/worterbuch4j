package worterbuch.client.demo;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bbmsoft.worterbuch.client.KeyValuePair;
import net.bbmsoft.worterbuch.client.WorterbuchClient;
import net.bbmsoft.worterbuch.client.WorterbuchException;
import net.bbmsoft.worterbuch.client.collections.AsyncWorterbuchList;

@Component
public class ClientDemo {

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private volatile BundleContext ctx;

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

		new Thread(() -> {
			try {
				this.run();
			} catch (ExecutionException | InterruptedException | URISyntaxException | TimeoutException e) {
				this.error(e);
			}
		}).start();

	}

	private void run() throws ExecutionException, InterruptedException, URISyntaxException, TimeoutException {

		final var uri = new URI("tcp://localhost:8081");

		final var wb = WorterbuchClient.connect(uri, Arrays.asList("clientDemo/#"),
				Arrays.asList(KeyValuePair.of("clientDemo/lastWill", "nein")), this::exit, this::error);

		wb.subscribe("hello", true, true, String.class, System.err::println, System.err::println);

		final var list = new AsyncWorterbuchList<>(wb, "testapp", "collections", "asyncList", HelloWorld.class,
				this::error);

		var counter = list.size() - 1;
		var inverted = counter >= 2;
		while (true) {

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
				case 2 -> list.add(new HelloWorld("General", "Kenobi"));
				}
			}

			counter = list.size() - 1;

			if (counter >= 2) {
				inverted = true;
			}

			Thread.sleep(1000);
		}
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
		th.printStackTrace();
		this.exit(-1, th.getMessage());
	}

}
