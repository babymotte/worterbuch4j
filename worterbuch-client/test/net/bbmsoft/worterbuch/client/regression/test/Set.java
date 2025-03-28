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

import net.bbmsoft.worterbuch.client.error.WorterbuchException;
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

		Set.WB.client.set("set/hello/world", original).responseFuture().get();
		final var value = Set.WB.client.get("set/hello/world", Integer.class).responseFuture().get().value();
		Assert.assertEquals(original, value.intValue());
	}

	@Test
	public void syncRoundtripWithComplexObjectWorks() throws InterruptedException, ExecutionException {

		final var original = new HelloWorld("Hello", "world");

		Set.WB.client.set("set/hello/world", original).responseFuture().get();
		final var value = Set.WB.client.get("set/hello/world", HelloWorld.class).responseFuture().get().value();
		Assert.assertEquals(original, value);
	}

	@Test
	public void asyncRoundtripWithPrimitiveWorks() throws InterruptedException, ExecutionException {

		final var original = 123;
		final var queue = new SynchronousQueue<Integer>();

		Set.WB.client.set("set/hello/world", original).responseFuture().thenAcceptAsync(r -> {
			Set.WB.client.get("set/hello/world", Integer.class).responseFuture().thenAcceptAsync(v -> {
				try {
					queue.offer(v.value(), 1, TimeUnit.SECONDS);
				} catch (final InterruptedException e) {
					Assert.fail();
				}
			});
		});

		Assert.assertEquals(original, queue.poll(1, TimeUnit.SECONDS).intValue());

	}

	@Test
	public void asyncRoundtripWithComplexObjectWorks() throws InterruptedException, ExecutionException {

		final var original = new HelloWorld("Hello", "world");
		final var queue = new SynchronousQueue<HelloWorld>();

		Set.WB.client.set("set/hello/world", original).responseFuture().thenAcceptAsync(r -> {
			Set.WB.client.get("set/hello/world", HelloWorld.class).responseFuture().thenAcceptAsync(v -> {
				try {
					queue.offer(v.value(), 1, TimeUnit.SECONDS);
				} catch (final InterruptedException e) {
					Assert.fail();
				}
			});
		});

		Assert.assertEquals(original, queue.poll(1, TimeUnit.SECONDS));
	}

}
