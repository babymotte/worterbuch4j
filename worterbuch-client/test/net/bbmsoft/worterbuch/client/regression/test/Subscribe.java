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

import net.bbmsoft.worterbuch.client.api.WorterbuchException;
import net.bbmsoft.worterbuch.client.regression.test.Util.ContainerizedWB;

public class Subscribe {

	private static ContainerizedWB WB;

	@BeforeClass
	public static void before() throws URISyntaxException, TimeoutException, WorterbuchException, InterruptedException {
		Subscribe.WB = new ContainerizedWB();
		Subscribe.WB.start();
	}

	@AfterClass
	public static void after() throws Exception {
		Subscribe.WB.close();
	}

	@Test
	public void subscribingNonExistingKeySucceedsButDoesNothing() throws InterruptedException, ExecutionException {

		final var value = new SynchronousQueue<Optional<String>>();

		Subscribe.WB.client.subscribe("subscribe/hello/world/not/existing", true, false, String.class, value::add)
				.result().get();

		Assert.assertEquals(null, value.poll(100, TimeUnit.MILLISECONDS));
	}

	@Test
	public void subscribingExistingKeySucceedsAndFiresImmediately() throws InterruptedException, ExecutionException {

		final var value = new SynchronousQueue<Optional<String>>();

		Subscribe.WB.client.set("subscribe/hello/world/already/set", "hello").result().get();

		Subscribe.WB.client.subscribe("subscribe/hello/world/already/set", true, false, String.class, v -> {
			try {
				value.put(v);
			} catch (final InterruptedException e) {
				Assert.fail();
			}
		}).result().get();

		Assert.assertEquals("hello", value.poll(100, TimeUnit.MILLISECONDS).get());
	}

}
