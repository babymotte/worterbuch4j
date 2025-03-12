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

import net.bbmsoft.worterbuch.client.api.ErrorCode;
import net.bbmsoft.worterbuch.client.api.WorterbuchException;
import net.bbmsoft.worterbuch.client.model.Err;
import net.bbmsoft.worterbuch.client.regression.test.Util.ContainerizedWB;

public class Get {

	private static ContainerizedWB WB;

	@BeforeClass
	public static void before() throws URISyntaxException, TimeoutException, WorterbuchException, InterruptedException {
		Get.WB = new ContainerizedWB();
		Get.WB.start();
	}

	@AfterClass
	public static void after() throws Exception {
		Get.WB.close();
	}

	@Test
	public void syncGetWorks() throws InterruptedException, ExecutionException {

		final var value = Get.WB.client.get("$SYS/license", String.class).result().get().get();
		Assert.assertEquals(value, "AGPL-3.0-or-later");
	}

	@Test
	public void asyncGetWorks() throws InterruptedException, ExecutionException {

		final var queue = new SynchronousQueue<String>();

		Get.WB.client.get("$SYS/license", String.class).result().thenAcceptAsync(v -> {
			try {
				queue.offer(v.get(), 1, TimeUnit.SECONDS);
			} catch (final InterruptedException e) {
				Assert.fail();
			}
		});

		Assert.assertEquals(queue.poll(1, TimeUnit.SECONDS), "AGPL-3.0-or-later");
	}

	@Test
	public void syncGetOfNonexistingKeyReturnsNoSuchElement() throws InterruptedException, ExecutionException {

		Assert.assertEquals(Get.WB.client.get("key/not/set", String.class).result().get().err().getErrorCode(),
				ErrorCode.NoSuchValue);
	}

	@Test
	public void asyncGetOfNonexistingKeyReturnsNoSuchElement() throws InterruptedException, ExecutionException {

		final var queue = new SynchronousQueue<Err>();

		Get.WB.client.get("key/not/set", String.class).result().thenAcceptAsync(v -> {
			try {
				queue.offer(v.err(), 1, TimeUnit.SECONDS);
			} catch (final InterruptedException e) {
				Assert.fail();
			}
		});

		Assert.assertEquals(queue.poll(1, TimeUnit.SECONDS).getErrorCode(), ErrorCode.NoSuchValue);
	}

}