/*
 *  Worterbuch Java client library demo
 *
 *  Copyright (C) 2024 Michael Bachmann
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package worterbuch.client.demo;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
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

@Component
public class ClientDemo {

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private volatile BundleContext ctx;
	private volatile boolean running;
	private volatile Thread thread;

	static record HelloWorld(String greeting, String gretee) {

		@Override
		public int hashCode() {
			return Objects.hash(this.greeting, this.gretee);
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (this.getClass() != obj.getClass()) {
				return false;
			}
			final var other = (HelloWorld) obj;
			return Objects.equals(this.greeting, other.greeting) && Objects.equals(this.gretee, other.gretee);
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

		final var authToken = System.getenv("WORTERBUCH_AUTH_TOKEN");

		final var wb = authToken != null ? WorterbuchClient.connect(uri, authToken, this::exit, this::error)
				: WorterbuchClient.connect(uri, this::exit, this::error);

		wb.subscribeArray("testapp/state/collections/asyncList", true, true, HelloWorld.class, this::printOptional,
				System.err::println);

		final var list = new AsyncWorterbuchList<>(wb, "testapp", "collections", "asyncList", HelloWorld.class,
				this::error);

		wb.setLastWill(new KeyValuePair<?>[] { KeyValuePair.of("testapp/state/running", false) }, System.err::println);
		wb.setGraveGoods(new String[] { "testapp/state/collections/asyncList" }, System.err::println);

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
		th.printStackTrace();
		this.exit(-1, th.getMessage());
	}

	private <T> void printOptional(final Optional<T[]> optional) {
		optional.ifPresentOrElse(
				it -> System.err.println(String.join(", ", Arrays.asList(it).stream().map(Object::toString).toList())),
				() -> System.err.println("empty"));
	}

}
