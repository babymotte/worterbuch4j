package net.bbmsoft.worterbuch.client.regression.test;

import java.net.URISyntaxException;
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

public class Set {

	private static ContainerizedWB WB;

	@BeforeClass
	public static void before() throws URISyntaxException, TimeoutException, WorterbuchException, InterruptedException {
		Set.WB = new ContainerizedWB();
		Set.WB.start();
	}

	@AfterClass
	public static void after() throws Exception {
		Set.WB.close();
	}

	@Test
	public void syncRoundtripWithPrimitiveWorks() throws InterruptedException, ExecutionException {

		final var original = 123;

		Set.WB.client.set("set/hello/world", original).result().get();
		final var value = Set.WB.client.get("set/hello/world", Integer.class).result().get().get();
		Assert.assertEquals(value.intValue(), original);
	}

	@Test
	public void syncRoundtripWithComplexObjectWorks() throws InterruptedException, ExecutionException {

		final var original = new HelloWorld("Hello", "world");

		Set.WB.client.set("set/hello/world", original).result().get();
		final var value = Set.WB.client.get("set/hello/world", HelloWorld.class).result().get().get();
		Assert.assertEquals(original, value);
	}

	@Test
	public void asyncRoundtripWithPrimitiveWorks() throws InterruptedException, ExecutionException {

		final var original = 123;
		final var queue = new SynchronousQueue<Integer>();

		Set.WB.client.set("set/hello/world", original).result().thenAcceptAsync(r -> {
			Set.WB.client.get("set/hello/world", Integer.class).result().thenAcceptAsync(v -> {
				try {
					queue.offer(v.get(), 1, TimeUnit.SECONDS);
				} catch (final InterruptedException e) {
					Assert.fail();
				}
			});
		});

		Assert.assertEquals(queue.poll(1, TimeUnit.SECONDS).intValue(), original);

	}

	@Test
	public void asyncRoundtripWithComplexObjectWorks() throws InterruptedException, ExecutionException {

		final var original = new HelloWorld("Hello", "world");
		final var queue = new SynchronousQueue<HelloWorld>();

		Set.WB.client.set("set/hello/world", original).result().thenAcceptAsync(r -> {
			Set.WB.client.get("set/hello/world", HelloWorld.class).result().thenAcceptAsync(v -> {
				try {
					queue.offer(v.get(), 1, TimeUnit.SECONDS);
				} catch (final InterruptedException e) {
					Assert.fail();
				}
			});
		});

		Assert.assertEquals(queue.poll(1, TimeUnit.SECONDS), original);
	}

}
