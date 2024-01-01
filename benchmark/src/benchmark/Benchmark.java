package benchmark;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.bbmsoft.worterbuch.client.KeyValuePair;
import net.bbmsoft.worterbuch.client.WorterbuchClient;

@Component
public class Benchmark {

	final static int CLIENTS = 100;
	final static int LOOPS = 10;

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private BundleContext ctx;
	private WorterbuchClient wb;

	@Activate
	public void activate(final BundleContext ctx)
			throws IOException, URISyntaxException, InterruptedException, TimeoutException, ExecutionException {

		this.ctx = ctx;

//		final var uri = new URI("ws://localhost:8080/ws");
		final var uri = new URI("tcp://localhost:8081");

		for (var i = 0; i < Benchmark.CLIENTS; i++) {
			new Subscriber(uri).start();
		}

		this.wb = WorterbuchClient.connect(uri, this::exit, this::error);

		final var start = System.currentTimeMillis();

		System.err.println("Starting set loop …");
		for (var i = 0; i < Benchmark.LOOPS; i++) {

			System.err.println("Setting values …");
			this.processDump(kvp -> this.wb.set(kvp.getKey(), kvp.getValue(), this::error));
			System.err.println("Done.");

		}
		System.err.println("Done. Waiting for messages to be processed …");

		this.wb.set("clientDemo/roundtrip", "hello", this::error);
		this.wb.subscribe("clientDemo/roundtrip", false, false, Object.class, e -> {
			if (e.isPresent()) {
				System.err.println("Messages processed.");

				final var stop = System.currentTimeMillis();
				final var duration = stop - start;
				final var average = duration / Benchmark.LOOPS;

				System.err.println("Run time: " + duration + "ms; average duration per loop: " + average + "ms.");

				this.exit(0, "Done");
			}
		}, this::error);

	}

	@Deactivate
	public void deactivate() {
		this.wb.close();
	}

	private void processDump(final Consumer<KeyValuePair<?>> action) throws IOException {
		final var mapper = new ObjectMapper();
		final var dumpFile = this.getClass().getResourceAsStream("/dump.json");
		try (var read = new BufferedReader(new InputStreamReader(dumpFile, StandardCharsets.UTF_8))) {
			for (var line = read.readLine(); line != null; line = read.readLine()) {
				final var kvp = mapper.readValue(line, KeyValuePair.class);
				action.accept(kvp);
			}
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
