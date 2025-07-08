package net.bbmsoft.worterbuch.client.regression.test;

import java.net.URISyntaxException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import net.bbmsoft.worterbuch.client.api.TypedPStateEvent;
import net.bbmsoft.worterbuch.client.api.WorterbuchClient;
import net.bbmsoft.worterbuch.client.error.WorterbuchException;
import net.bbmsoft.worterbuch.client.regression.test.Util.ContainerizedWB;

public class PSubscribe {

	private static ContainerizedWB WB;
	private static WorterbuchClient TCP;
	private static WorterbuchClient WS;

	@BeforeClass
	public static void before() throws URISyntaxException, TimeoutException, WorterbuchException, InterruptedException {
		PSubscribe.WB = new ContainerizedWB();
		PSubscribe.WB.start();
		PSubscribe.TCP = PSubscribe.WB.client;
		PSubscribe.WS = PSubscribe.WB.startWsClient();
	}

	@AfterClass
	public static void after() throws Exception {
		PSubscribe.WS.close();
		PSubscribe.WB.close();
	}

	@Test
	public void exceptionsInCallbackAreHandledGracefully() throws NoSuchElementException, WorterbuchException,
			InterruptedException, TimeoutException, ExecutionException {

		final var latch = new CountDownLatch(6);

		final Consumer<TypedPStateEvent<Integer>> callback = e -> {
			for (var i = 0; i < e.keyValuePairs.size(); i++) {
				latch.countDown();
			}
			throw new RuntimeException();
		};

		PSubscribe.WB.client.pSubscribe("exceptionsInCallbackAreHandledGracefully/#", false, false, Optional.of(0L),
				Integer.class, callback);

		PSubscribe.TCP.set("exceptionsInCallbackAreHandledGracefully/1", 1);
		PSubscribe.TCP.set("exceptionsInCallbackAreHandledGracefully/2", 2);
		PSubscribe.TCP.set("exceptionsInCallbackAreHandledGracefully/3", 3);
		Thread.sleep(100);
		PSubscribe.TCP.set("exceptionsInCallbackAreHandledGracefully/4", 4);
		PSubscribe.TCP.set("exceptionsInCallbackAreHandledGracefully/5", 5);
		PSubscribe.TCP.set("exceptionsInCallbackAreHandledGracefully/6", 6);

		if (!latch.await(5, TimeUnit.SECONDS)) {
			Assert.fail("did not receive callbacks in time");
		}

	}

	@Test
	public void exceptionsInWsCallbackAreHandledGracefully() throws NoSuchElementException, WorterbuchException,
			InterruptedException, TimeoutException, ExecutionException {

		final var latch = new CountDownLatch(6);

		final Consumer<TypedPStateEvent<Integer>> callback = e -> {
			for (var i = 0; i < e.keyValuePairs.size(); i++) {
				latch.countDown();
			}
			throw new RuntimeException();
		};

		PSubscribe.WS.pSubscribe("exceptionsInCallbackAreHandledGracefully/#", false, false, Optional.of(0L),
				Integer.class, callback);

		PSubscribe.WS.set("exceptionsInCallbackAreHandledGracefully/1", 1);
		PSubscribe.WS.set("exceptionsInCallbackAreHandledGracefully/2", 2);
		PSubscribe.WS.set("exceptionsInCallbackAreHandledGracefully/3", 3);
		Thread.sleep(100);
		PSubscribe.WS.set("exceptionsInCallbackAreHandledGracefully/4", 4);
		PSubscribe.WS.set("exceptionsInCallbackAreHandledGracefully/5", 5);
		PSubscribe.WS.set("exceptionsInCallbackAreHandledGracefully/6", 6);

		if (!latch.await(5, TimeUnit.SECONDS)) {
			Assert.fail("did not receive callbacks in time");
		}

	}

}
