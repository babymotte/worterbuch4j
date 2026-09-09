package proxy.locks.demo;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bbmsoft.worterbuch.client.Worterbuch;
import net.bbmsoft.worterbuch.client.api.WorterbuchClient;
import net.bbmsoft.worterbuch.client.error.ConnectionError;
import net.bbmsoft.worterbuch.client.error.ConnectionFailed;

@Component
public class ProxyLocksDemo {

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private final Executor exec = Executors.newCachedThreadPool();

	private volatile BundleContext ctx;
	private volatile boolean deactivating;
	private volatile boolean running;

	private volatile WorterbuchClient client1;
	private volatile WorterbuchClient client2;

	@Activate
	public void start() throws ConnectionFailed, TimeoutException, URISyntaxException, InterruptedException {

		this.running = true;

		this.client1 = Worterbuch.connect(List.of(new URI("tcp://localhost:9094")), null, this::exit, this::error);
		this.client2 = Worterbuch.connect(List.of(new URI("tcp://localhost:9095")), null, this::exit, this::error);

		this.lock(this.client1, "lock/1");
		this.lock(this.client2, "lock/2");
		Thread.sleep(100);
		this.lock(this.client1, "lock/2");
		this.lock(this.client2, "lock/1");

	}

	@Deactivate
	public void stop() {
		this.deactivating = true;
		this.running = false;

		final var client1 = this.client1;
		final var client2 = this.client2;

		if (client1 != null) {
			try {
				client1.close();
			} catch (final Exception e) {
				this.log.error("Failed to close client '{}':", client1.getClientId(), e);
			}
		}
		if (client2 != null) {
			try {
				client2.close();
			} catch (final Exception e) {
				this.log.error("Failed to close client '{}':", client2.getClientId(), e);
			}
		}
	}

	private void lock(final WorterbuchClient client, final String key) {

		if (!this.running) {
			return;
		}

		this.log.info("Requesting lock on key '{}' for client '{}' …", key, client.getClientId());
		final Runnable onLost = () -> {
			this.log.warn("Client '{}' lost the lock on key '{}'. Trying to re-acquire it …", client.getClientId(),
					key);
			this.lock(client, key);
		};
		final var fut = client.acquireLock(key, onLost);
		this.exec.execute(() -> {
			try {
				final var res = fut.await();
				if (res.isOk()) {
					this.log.info("Client '{}' acquired lock on key '{}'.", client.getClientId(), key);
				} else {
					this.log.error("Client '{}' failed to acquire lock on key '{}': {}", client.getClientId(), key,
							res.err());
					this.lock(client, key);
				}
			} catch (final ConnectionError e) {
				this.log.error("Client '{}' failed to acquire lock on key '{}':", client.getClientId(), key, e);
				this.lock(client, key);
			} catch (final InterruptedException e) {
				this.log.error("Client '{}' failed to acquire lock on key '{}':", client.getClientId(), key, e);
				this.lock(client, key);
			}
		});
	}

	private void exit(final Integer errorCode, final String message) {

		if (Thread.currentThread().isInterrupted()) {
			throw new IllegalStateException("Some sneaky little bastard interrupted this thread. Don't do that!");
		}

		this.running = false;
		if (this.deactivating) {
			return;
		}
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
