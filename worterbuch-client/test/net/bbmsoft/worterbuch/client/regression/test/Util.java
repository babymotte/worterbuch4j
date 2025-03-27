package net.bbmsoft.worterbuch.client.regression.test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.testcontainers.containers.GenericContainer;

import net.bbmsoft.worterbuch.client.Worterbuch;
import net.bbmsoft.worterbuch.client.api.WorterbuchClient;
import net.bbmsoft.worterbuch.client.api.WorterbuchException;

public class Util {

	public static class ContainerizedWB implements AutoCloseable {
		private GenericContainer<?> wb;

		public WorterbuchClient client;

		public void start() throws URISyntaxException, TimeoutException, WorterbuchException {
			this.wb = new GenericContainer<>("babymotte/worterbuch:1.3.2");
			this.wb.withExposedPorts(9090).start();
			final var port = this.wb.getMappedPort(9090);

			final var uris = this.uris(port);

			this.client = Worterbuch.connect(uris, (i, m) -> {
			}, e -> {
			});
		}

		private List<URI> uris(final Integer port) throws URISyntaxException {
			// uncomment to test with locally running instance. Make sure you start a new
			// instance without persistence
//			final var uris = Arrays.asList(new URI("tcp://127.0.0.1:8081"));
			final var uris = Arrays.asList(new URI("tcp://127.0.0.1:" + port));
			return uris;
		}

		@Override
		public void close() throws Exception {
			this.client.close();
			this.wb.close();
		}

		public WorterbuchClient startSecondClient() throws TimeoutException, WorterbuchException, URISyntaxException {
			final var port = this.wb.getMappedPort(9090);

			final var uris = this.uris(port);

			return Worterbuch.connect(uris, (i, m) -> {
			}, e -> {
			});

		}
	}

}
