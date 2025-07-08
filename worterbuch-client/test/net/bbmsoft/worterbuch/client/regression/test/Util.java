package net.bbmsoft.worterbuch.client.regression.test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.testcontainers.containers.GenericContainer;

import net.bbmsoft.worterbuch.client.Worterbuch;
import net.bbmsoft.worterbuch.client.api.WorterbuchClient;
import net.bbmsoft.worterbuch.client.error.WorterbuchException;

public class Util {

	public static class ContainerizedWB implements AutoCloseable {
		private GenericContainer<?> wb;

		public WorterbuchClient client;

		public void start() throws URISyntaxException, TimeoutException, WorterbuchException {
			this.wb = new GenericContainer<>("babymotte/worterbuch:1.3.7");
			this.wb.withExposedPorts(80, 9090).start();
			final var tcpPort = this.wb.getMappedPort(9090);
			final var wsPort = this.wb.getMappedPort(80);

			final var uris = this.uris(tcpPort, wsPort);

			this.client = Worterbuch.connect(uris.subList(0, 1), (i, m) -> {
			}, e -> {
				e.printStackTrace();
			});
		}

		private List<URI> uris(final Integer tcpPort, final Integer wsPort) throws URISyntaxException {
			// uncomment to test with locally running instance. Make sure you start a new
			// instance without persistence
//			final var uris = Arrays.asList(new URI("tcp://127.0.0.1:8081"), new URI("ws://127.0.0.1:8080"));
			final var uris = Arrays.asList(new URI("tcp://127.0.0.1:" + tcpPort),
					new URI("ws://127.0.0.1:" + wsPort + "/ws"));
			return uris;
		}

		@Override
		public void close() throws Exception {
			this.client.close();
			this.wb.close();
		}

		public WorterbuchClient startSecondClient() throws TimeoutException, WorterbuchException, URISyntaxException {

			final var tcpPort = this.wb.getMappedPort(9090);
			final var wsPort = this.wb.getMappedPort(80);

			final var uris = this.uris(tcpPort, wsPort);

			return Worterbuch.connect(uris.subList(0, 1), (i, m) -> {
			}, e -> {
				e.printStackTrace();
			});

		}

		public WorterbuchClient startWsClient() throws TimeoutException, WorterbuchException, URISyntaxException {
			final var tcpPort = this.wb.getMappedPort(9090);
			final var wsPort = this.wb.getMappedPort(80);

			final var uris = this.uris(tcpPort, wsPort);

			return Worterbuch.connect(uris.subList(1, 2), (i, m) -> {
			}, e -> {
				e.printStackTrace();
			});

		}
	}
}
