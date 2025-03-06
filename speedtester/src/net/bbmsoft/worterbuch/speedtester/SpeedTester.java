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

package net.bbmsoft.worterbuch.speedtester;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import net.bbmsoft.worterbuch.speedtester.impl.Agent;
import net.bbmsoft.worterbuch.speedtester.impl.Utils;

@Component
public final class SpeedTester {

	final static Random rand = new Random();

	static class Data {
		public long lastOffset;
		public long lastTimestamp;
		public long deltaOffset;
		public long deltaT;
		public long lag;

		public Data(final long lastOffset, final long lastTimestamp, final long deltaOffset, final long deltaT,
				final long lag) {
			super();
			this.lastOffset = lastOffset;
			this.lastTimestamp = lastTimestamp;
			this.deltaOffset = deltaOffset;
			this.deltaT = deltaT;
			this.lag = lag;
		}
	}

	private final String targetRate;
	private final String numAagents;
	private final ScheduledExecutorService executor;
	private final Agent[] agents;
	private final Data[] data;

	public SpeedTester() {
		this.targetRate = System.getenv("WORTERBUCH_SPEEDTEST_TARGET_RATE");
		this.numAagents = System.getenv("WORTERBUCH_SPEEDTEST_AGENTS");
		this.executor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "worterbuch-speedtest"));
		this.agents = new Agent[this.numAagents != null ? Integer.parseInt(this.numAagents) : 1];
		this.data = new Data[this.numAagents != null ? Integer.parseInt(this.numAagents) : 1];
		final var now = System.currentTimeMillis();
		for (var i = 0; i < this.data.length; i++) {
			this.data[i] = new Data(0, now, 0, 0, 0);
		}
	}

	@Activate
	public void activate() {

		try {
			final var agents = this.numAagents != null ? Long.parseLong(this.numAagents) : 1;
			final var targetRate = this.targetRate != null ? Long.parseLong(this.targetRate) : 1000;

			for (var i = 0; i < agents; i++) {
				this.agents[i] = new Agent(i, ((double) targetRate) / (double) agents, new StatusListener());
			}

			this.executor.scheduleAtFixedRate(this::printStatus, 1, 1, TimeUnit.SECONDS);

			for (final Agent agent : this.agents) {
				this.executor.schedule(agent::start, Math.round(1000 * SpeedTester.rand.nextDouble()),
						TimeUnit.MILLISECONDS);
			}
		} catch (final Throwable th) {
			th.printStackTrace();
			Utils.shutDown();
		}
	}

	void onStatusUpdate(final int id, final long sentoffset, final long receivedOffset) {
		final var now = System.currentTimeMillis();
		final var lastOffset = this.data[id].lastOffset;
		this.data[id].lastOffset = receivedOffset;
		this.data[id].deltaOffset = receivedOffset - lastOffset;
		final var lastTimestamp = this.data[id].lastTimestamp;
		this.data[id].lastTimestamp = now;
		this.data[id].deltaT = now - lastTimestamp;
		this.data[id].lag = sentoffset - receivedOffset;
	}

	private void printStatus() {
		var totalDeltaT = 0l;
		var totalDeltaOffsets = 0l;
		var totalLag = 0l;
		for (var i = 0; i < this.data.length; i++) {
			totalDeltaT += this.data[i].deltaT;
			totalDeltaOffsets += this.data[i].deltaOffset;
			totalLag += this.data[i].lag;
		}
		final var averageDeltaT = ((double) totalDeltaT) / this.data.length;
		final var totalRate = (long) ((totalDeltaOffsets * 1000) / averageDeltaT);

		System.err.printf("Receiving rate: %s msg/s, lag: %s msg%n", totalRate, totalLag);
	}

	public class StatusListener {

		public void onStatusUpdate(final int id, final long sentoffset, final long receivedOffset) {
			SpeedTester.this.executor.execute(() -> SpeedTester.this.onStatusUpdate(id, sentoffset, receivedOffset));
		}
	}
}
