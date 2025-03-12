/*
 *  Worterbuch Java speed tester
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

package net.bbmsoft.worterbuch.speedtester.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.bbmsoft.worterbuch.client.Worterbuch;
import net.bbmsoft.worterbuch.client.api.WorterbuchClient;
import net.bbmsoft.worterbuch.client.api.WorterbuchException;
import net.bbmsoft.worterbuch.speedtester.SpeedTester.StatusListener;

public final class Agent {

	private final int id;
	private final StatusListener statusListener;
	private final String key;
	private final ScheduledExecutorService executor;
	private final long delay;

	private long sentOffset;
	private long receivedOffset;
	private final WorterbuchClient wb;

	public Agent(final int id, final double targetRate, final StatusListener statusListener)
			throws InterruptedException, TimeoutException, URISyntaxException, WorterbuchException {
		this.id = id;
		this.statusListener = statusListener;
		this.executor = Executors
				.newSingleThreadScheduledExecutor(r -> new Thread(r, "worterbuch-speedtest-agent-" + id));
		this.key = "speedtest/agent/" + id + "/offset";
		this.delay = Math.round(1_000_000 / targetRate);
		this.sentOffset = 0;
		this.receivedOffset = 0;
		final var hostAddress = System.getenv("WORTERBUCH_HOST_ADDRESS");
		final var port = System.getenv("WORTERBUCH_PORT");
		final var proto = System.getenv("WORTERBUCH_PROTO");

		final var uri = "ws".equalsIgnoreCase(proto)
				? new URI(("ws://" + (hostAddress != null ? hostAddress : "localhost") + ":"
						+ (port != null ? port : "8081") + "/ws"))
				: new URI(("tcp://" + (hostAddress != null ? hostAddress : "localhost") + ":"
						+ (port != null ? port : "8081")));

		System.err.println("Connecting to worterbuch server " + uri);
		this.wb = Worterbuch.connect(Arrays.asList(uri), null, this.executor, this::onDisconnect, this::onError);
	}

	public void start() {

		this.wb.subscribe(this.key, false, true, Long.class, this::pong);

		this.executor.scheduleAtFixedRate(() -> this.ping(this.wb), 0, this.delay, TimeUnit.MICROSECONDS);
		this.executor.scheduleAtFixedRate(() -> this.reportStatus(this.wb), 1, 1, TimeUnit.SECONDS);
	}

	private void ping(final WorterbuchClient wb) {
		wb.set(this.key, ++this.sentOffset);
	}

	private void pong(final Optional<Long> msg) {
		msg.ifPresent(o -> this.receivedOffset = o);
	}

	private void reportStatus(final WorterbuchClient wb) {
		this.statusListener.onStatusUpdate(this.id, this.sentOffset, this.receivedOffset);
	}

	private void onDisconnect(final Integer errorCode, final String message) {
		System.err.println(message);
		Utils.shutDown();
	}

	private void onError(final WorterbuchException e) {
		e.printStackTrace();
		Utils.shutDown();
	}

}
