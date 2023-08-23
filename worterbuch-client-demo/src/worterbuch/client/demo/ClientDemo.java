package worterbuch.client.demo;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bbmsoft.worterbuch.client.KeyValuePair;
import net.bbmsoft.worterbuch.client.WorterbuchClient;
import net.bbmsoft.worterbuch.client.WorterbuchException;

@Component
public class ClientDemo {

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private BundleContext ctx;

	@Activate
	public void activate(final BundleContext ctx)
			throws URISyntaxException, WorterbuchException, InterruptedException, ExecutionException {
		this.ctx = ctx;

		final var uri = new URI("ws://worterbuch.local/ws");

		final var wb = WorterbuchClient.connect(uri, Arrays.asList("clientDemo/#"),
				Arrays.asList(KeyValuePair.of("clientDemo/lastWill", "nein")), this::exit, this::error);

		wb.pSubscribe("#", true, Object.class, System.err::println, this::error);

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
