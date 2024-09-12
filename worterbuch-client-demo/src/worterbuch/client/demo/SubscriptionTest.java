package worterbuch.client.demo;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bbmsoft.worterbuch.client.KeyValuePair;
import net.bbmsoft.worterbuch.client.PStateEvent;
import net.bbmsoft.worterbuch.client.WorterbuchClient;

@Component
public class SubscriptionTest {

	static record Property(String value) {
	}

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private final Set<String> subscribedProperties;
	private WorterbuchClient wb;

	public SubscriptionTest() {
		this.subscribedProperties = ConcurrentHashMap.newKeySet();
	}

	@Activate
	public void activate() throws URISyntaxException, InterruptedException, TimeoutException {

		final var authToken = System.getenv("WORTERBUCH_AUTH_TOKEN");

		final var uri = new URI("tcp://stagenet.int:31001");
		this.log.info("Connecting to worterbuch server at {}", uri);

		this.wb = WorterbuchClient.connect(uri, authToken, (e, m) -> {
		}, System.err::println);

		this.wb.pSubscribe("stagenetSolution/blocks/?/parameter/?/value", true, false, Optional.empty(), String.class,
				this::parameterValueKeyChanged, System.out::println);

		Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::printMissing, 3, 1, TimeUnit.SECONDS);
	}

	private void parameterValueKeyChanged(final PStateEvent<String> e) {

		if (!this.subscribedProperties.isEmpty()) {
			throw new IllegalStateException("Parameter value key changed after start!");
		}

		if (e.keyValuePairs == null) {
			throw new IllegalStateException("Inconceivable!");
		}

		System.err.printf("Subscribing to %d properties ...\n", e.keyValuePairs.size());

		for (final KeyValuePair<String> kvp : e.keyValuePairs) {
			final var propertyKey = kvp.getValue().substring(1);
			if (this.subscribedProperties.add(propertyKey)) {
				this.wb.subscribe(propertyKey, true, false, Property.class, v -> this.propertyValueChanged(propertyKey),
						System.err::println);
			} else {
				System.err.println("Duplicate property: " + propertyKey);
			}
		}

	}

	private void propertyValueChanged(final String propertyKey) {
		this.subscribedProperties.remove(propertyKey);
	}

	private void printMissing() {
		System.err.println(this.subscribedProperties.size() + " missing props:");
		for (final String prop : this.subscribedProperties) {
			System.err.println(prop);
		}
	}
}