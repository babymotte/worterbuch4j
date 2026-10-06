package net.bbmsoft.worterbuch.client.test.regression;

import java.net.URISyntaxException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import net.bbmsoft.worterbuch.client.error.WorterbuchException;
import net.bbmsoft.worterbuch.client.response.Future;
import net.bbmsoft.worterbuch.client.test.common.Util.ContainerizedWB;

public class Unsubscribe {

	private static ContainerizedWB WB;

	@BeforeClass
	public static void before() throws URISyntaxException, TimeoutException, WorterbuchException, InterruptedException {
		Unsubscribe.WB = new ContainerizedWB();
		Unsubscribe.WB.start();
	}

	@AfterClass
	public static void after() throws Exception {
		Unsubscribe.WB.close();
	}

	@Test
	public void unsubscribeRemovesExistingSubscription() throws InterruptedException, ExecutionException {

		final var key = "subscribe/hello/world/unsubscribe";

		final var value = new LinkedBlockingQueue<Optional<String>>();

		final Future<Void> future = Unsubscribe.WB.client.subscribe(key, true, false, String.class, value::add);
		final var transactionId = future.transactionId();
		future.responseFuture().get();

		Assert.assertTrue(value.isEmpty());

		Unsubscribe.WB.client.set(key, "hello").responseFuture().get();
		Assert.assertEquals("hello", value.poll(100, TimeUnit.MILLISECONDS).get());
		Assert.assertTrue(value.isEmpty());

		Unsubscribe.WB.client.set(key, "hello2").responseFuture().get();
		Assert.assertEquals("hello2", value.poll(100, TimeUnit.MILLISECONDS).get());
		Assert.assertTrue(value.isEmpty());

		Unsubscribe.WB.client.unsubscribe(transactionId).responseFuture().get();

		Unsubscribe.WB.client.set(key, "hello3").responseFuture().get();
		Assert.assertEquals(null, value.poll(100, TimeUnit.MILLISECONDS));
		Assert.assertTrue(value.isEmpty());

		Unsubscribe.WB.client.set(key, "hello4").responseFuture().get();
		Assert.assertEquals(null, value.poll(100, TimeUnit.MILLISECONDS));
		Assert.assertTrue(value.isEmpty());
	}

}
