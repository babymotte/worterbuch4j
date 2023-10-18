package benchmark;

import java.net.URI;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import net.bbmsoft.worterbuch.client.KeyValuePair;
import net.bbmsoft.worterbuch.client.WorterbuchClient;
import net.bbmsoft.worterbuch.client.WorterbuchException;

public class Subscriber extends Thread {

	private final URI uri;

	public Subscriber(final URI uri) {
		this.uri = uri;
	}

	@Override
	public void run() {
		WorterbuchClient wb;
		try {
			wb = WorterbuchClient.connect(this.uri, Arrays.asList("clientDemo/#"),
					Arrays.asList(KeyValuePair.of("clientDemo/lastWill", "nein")),
					(e, msg) -> System.err.println("Connection lost: " + msg + "(" + e + ")"),
					Throwable::printStackTrace);

			final var topic = "#";

			System.err.println("Subscribing to '" + topic + "'");

			wb.pSubscribe(topic, false, false, Optional.of(1L), Object.class, e -> {
			}, WorterbuchException::printStackTrace);

		} catch (InterruptedException | TimeoutException e) {
			throw new IllegalStateException(e);
		}

	}

}
