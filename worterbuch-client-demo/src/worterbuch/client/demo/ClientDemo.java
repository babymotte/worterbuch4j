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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.type.TypeFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bbmsoft.worterbuch.client.Worterbuch;
import net.bbmsoft.worterbuch.client.api.WorterbuchException;
import net.bbmsoft.worterbuch.client.collections.WbMap;
import net.bbmsoft.worterbuch.client.model.KeyValuePair;

@Component
public class ClientDemo {

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private volatile BundleContext ctx;
	private volatile boolean running;
	private volatile Thread thread;

	static record HelloWorld(String greeting, String gretee) {
	}

	@Activate
	@SuppressFBWarnings("EI_EXPOSE_REP2")
	public void activate(final BundleContext ctx)
			throws URISyntaxException, WorterbuchException, InterruptedException, ExecutionException, TimeoutException {
		this.ctx = ctx;
		this.running = true;

		this.thread = new Thread(() -> {
			try {
				this.run();
			} catch (final Exception e) {
				this.error(e);
			}
		});
		this.thread.start();

	}

	@Deactivate
	public void deactivate() {
		this.running = false;
	}

	private void run() throws Exception {

		final var uris = Arrays.asList(new URI("ws://localhost:8081/ws"), new URI("ws://localhost:8080"),
				new URI("ws://localhost:8080/ws"));

		final var authToken = System.getenv("WORTERBUCH_AUTH_TOKEN");

		final var wb = authToken != null ? Worterbuch.connect(uris, authToken, null, this::exit, this::error)
				: Worterbuch.connect(uris, this::exit, this::error);

		final var locked = wb.lock("testapp/state/leader").result().get().isOk();
		System.out.println("Key locked: " + locked);

		System.out.println("Acquiring lock on hello/world ...");
		wb.acquireLock("hello/world").result().get();
		System.out.println("Lock on hello/world acquired.");

		wb.set("testapp/state/running", true);

		wb.pLs("$SYS/?/?").result().thenAccept(System.err::println);
		System.err.println(wb.pLs("$SYS/?/?").result().get().get());

		wb.subscribeList("testapp/state/collections/asyncList", true, true, HelloWorld.class, this::printOptional);

		wb.setLastWill(Collections.emptyList());
		wb.setGraveGoods(Collections.emptyList());

		wb.updateLastWill(will -> will.add(new KeyValuePair("testapp/state/running", false)));
		wb.updateGraveGoods(gg -> gg.add("testapp/state/#"));

		final var map = new WbMap<>(wb, "testapp", "mapTest", "map", HelloWorld.class);

		final var type = TypeFactory.defaultInstance().constructCollectionType(TreeSet.class, Integer.class);
		for (var i = 0; i < 10; i++) {
			final var it = i;
			new Thread(() -> {
				wb.update("testapp/state/cas-list", TreeSet::new, l -> l.add(it), type);
			}).start();
		}

		final var inverted = new AtomicBoolean();

		while (this.running) {

			wb.updateList("testapp/state/collections/asyncList", list -> {

				var counter = list.size() - 1;

				if (counter < 0) {
					counter = 0;
					inverted.set(false);
				}

				if (inverted.get()) {
					list.remove(counter);
					map.remove(String.valueOf(counter));
				} else {
					switch (list.size()) {
					case 0 -> {
						list.add(new HelloWorld("Hello", "World"));
						map.put("0", new HelloWorld("Hello", "World"));
					}
					case 1 -> {
						list.add(new HelloWorld("Hello", "There"));
						map.put("1", new HelloWorld("Hello", "World"));
					}
					default -> {
						list.add(new HelloWorld("General", "Kenobi"));
						map.put("2", new HelloWorld("Hello", "World"));
					}
					}
				}

				counter = list.size() - 1;

				if (counter >= 2) {
					inverted.set(true);
				}

			}, HelloWorld.class);

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

	private <T> void printOptional(final Optional<List<T>> optional) {
		optional.ifPresentOrElse(
				it -> System.err.println(String.join(", ", Arrays.asList(it).stream().map(Object::toString).toList())),
				() -> System.err.println("empty"));
	}

}
