package net.bbmsoft.worterbuch.client.regression.test;

import java.net.URISyntaxException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import net.bbmsoft.worterbuch.client.api.ErrorCode;
import net.bbmsoft.worterbuch.client.api.WorterbuchException;
import net.bbmsoft.worterbuch.client.regression.test.Util.ContainerizedWB;

public class Cas {

	private static ContainerizedWB WB;

	@BeforeClass
	public static void before() throws URISyntaxException, TimeoutException, WorterbuchException, InterruptedException {
		Cas.WB = new ContainerizedWB();
		Cas.WB.start();
	}

	@AfterClass
	public static void after() throws Exception {
		Cas.WB.close();
	}

	@Test
	public void casValuesCannotBeSetRegularly() throws InterruptedException, ExecutionException {

		final var key = "cas/Values/Cannot/Be/Set/Regularly";

		Cas.WB.client.cSet(key, "hello", 0).result().get().get();

		Assert.assertEquals(ErrorCode.Cas, Cas.WB.client.set(key, "world").result().get().err().getErrorCode());
	}

	@Test
	public void regularGetOnCasValueReturnsOnlyValue() throws InterruptedException, ExecutionException {

		final var key = "regular/Get/On/Cas/Value/Returns/Only/Value";
		final var original = "hello";

		Cas.WB.client.cSet(key, original, 0).result().get().get();

		Assert.assertEquals(original, Cas.WB.client.get(key, String.class).result().get().get());
	}

	@Test
	public void pGetOnCasValueReturnsOnlyValue() throws InterruptedException, ExecutionException {

		final var key = "pGet/On/Cas/Value/Returns/Only/Value";
		final var original = "hello";

		Cas.WB.client.cSet(key, original, 0).result().get().get();

		Assert.assertEquals(original,
				Cas.WB.client.pGet(key, String.class).result().get().get().iterator().next().getValue());
	}

	@Test
	public void pSubscribeOnCasValueReturnsOnlyValue() throws InterruptedException, ExecutionException {

		final var key = "pSubscribe/On/Cas/Value/Returns/Only/Value";
		final var original = "hello";

		Cas.WB.client.cSet(key, original, 0).result().get().get();

		final var queue = new SynchronousQueue<String>();

		Cas.WB.client.pSubscribe(key, false, false, Optional.empty(), String.class, e -> {
			try {
				queue.offer(e.keyValuePairs.iterator().next().getValue(), 1, TimeUnit.SECONDS);
			} catch (final InterruptedException e1) {
				Assert.fail();
			}
		}).result().get();

		Assert.assertEquals(original, queue.poll(1, TimeUnit.SECONDS));
	}

}