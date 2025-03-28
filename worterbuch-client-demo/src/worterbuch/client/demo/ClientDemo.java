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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bbmsoft.worterbuch.client.Worterbuch;
import net.bbmsoft.worterbuch.client.api.WorterbuchClient;
import net.bbmsoft.worterbuch.client.api.util.type.TypeUtil;
import net.bbmsoft.worterbuch.client.collections.WbMap;
import net.bbmsoft.worterbuch.client.error.WorterbuchException;
import net.bbmsoft.worterbuch.client.model.KeyValuePair;
import net.bbmsoft.worterbuch.client.response.Response;

@Component
public class ClientDemo {

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private volatile BundleContext ctx;
	private volatile boolean deactivating;
	private volatile boolean running;

	final ScheduledExecutorService testExecutor = Executors
			.newSingleThreadScheduledExecutor(r -> new Thread(r, "WB Demo Executor"));

	static record HelloWorld(String greeting, String gretee) {
	}

	@Activate
	@SuppressFBWarnings("EI_EXPOSE_REP2")
	public void activate(final BundleContext ctx)
			throws URISyntaxException, WorterbuchException, InterruptedException, ExecutionException, TimeoutException {
		this.ctx = ctx;
		this.running = true;

		this.testExecutor.execute(() -> {
			try {
				this.run();
			} catch (final Exception e) {
				this.error(e);
			}
		});

	}

	@Deactivate
	public void deactivate() {
		this.deactivating = true;
		this.running = false;
	}

	private void run() throws Exception {

		final var uris = Arrays.asList(new URI("ws://localhost:8081/ws"), new URI("ws://localhost:8080"),
				new URI("ws://localhost:8080/ws"));

		final var authToken = System.getenv("WORTERBUCH_AUTH_TOKEN");

		final var wb = authToken != null ? Worterbuch.connect(uris, authToken, this::exit, this::error)
				: Worterbuch.connect(uris, this::exit, this::error);

		this.log.info("Acquiring lock on testapp/state/leader ...");
		wb.acquireLock("testapp/state/leader").await();
		this.log.info("Lock on testapp/state/leader acquired.");

		wb.set("testapp/state/running", true);

		wb.get("testapp/state/running", Boolean.class).andThen(v -> {
			this.printOptional(v);
		}, this.testExecutor);

		wb.pLs("$SYS/?/?").andThen(this::printOptional, this.testExecutor);
		this.printResponse(wb.pLs("$SYS/?/?").await());

		wb.subscribe("testapp/state/collections/asyncList", true, true, TypeUtil.list(HelloWorld.class), v -> {
			this.printOptional(v);
		}, this.testExecutor);

		wb.subscribe("testapp/state/collections/array", true, true, boolean[].class, v -> {
			this.printOptional(v);
		}, this.testExecutor);

		wb.setLastWill(Collections.emptyList());
		wb.setGraveGoods(Collections.emptyList());

		wb.updateLastWill(will -> will.add(new KeyValuePair("testapp/state/running", false)));
		wb.updateGraveGoods(gg -> gg.add("testapp/state/#"));

		final var map = new WbMap<>(wb, "testapp", "mapTest", "map", HelloWorld.class);

		for (var i = 0; i < 20; i++) {
			final var it = i;
			new Thread(() -> {
				wb.update("testapp/state/cas-list", l -> {
					l.add(it);
				}, TypeUtil.treeSet(Integer.class));
			}).start();
		}

		final var inverted = new AtomicBoolean();

		this.loop(wb, map, inverted, 0);

	}

	private void loop(final WorterbuchClient wb, final WbMap<HelloWorld> map, final AtomicBoolean inverted, final int i)
			throws Exception {

		if (this.running) {

			wb.update("testapp/state/collections/array", (final var maybeArray) -> {
				if (maybeArray.isPresent()) {
					final var arr = maybeArray.get();
					final var idx = i % arr.length;
					arr[idx] = !arr[idx];
					return arr;
				} else {
					return new boolean[] { false, false, false };
				}
			}, boolean[].class);

			wb.<List<HelloWorld>>update("testapp/state/collections/asyncList", list -> {

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

			}, TypeUtil.list(HelloWorld.class));

			this.testExecutor.schedule(() -> {
				try {
					this.loop(wb, map, inverted, i + 1);
				} catch (final Exception e) {
					e.printStackTrace();
					this.running = false;
				}
			}, 1, TimeUnit.SECONDS);

		} else {
			wb.close();
		}
	}

	private void exit(final Integer errorCode, final String message) {
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

	private <T> void printResponse(final Response<T> resp) {
		this.log.info("{}", resp);
	}

	private <T> void printOptional(final Optional<T> optional) {
		optional.ifPresentOrElse(it -> this.log.info("{}", it), () -> this.log.info("empty"));
	}

}
