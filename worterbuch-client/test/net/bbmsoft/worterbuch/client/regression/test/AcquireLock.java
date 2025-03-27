package net.bbmsoft.worterbuch.client.regression.test;

import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import net.bbmsoft.worterbuch.client.api.WorterbuchException;
import net.bbmsoft.worterbuch.client.regression.test.Util.ContainerizedWB;

public class AcquireLock {

	private static ContainerizedWB WB;

	@BeforeClass
	public static void before() throws URISyntaxException, TimeoutException, WorterbuchException, InterruptedException {
		AcquireLock.WB = new ContainerizedWB();
		AcquireLock.WB.start();

	}

	@AfterClass
	public static void after() throws Exception {
		AcquireLock.WB.close();
	}

	@Test
	public void lockIsGrantedIfNotHeld() throws InterruptedException, ExecutionException, TimeoutException {
		final var key = "acquireLock/lockIsGrantedIfNotHeld";
		AcquireLock.WB.client.acquireLock(key).result().get(100, TimeUnit.MILLISECONDS).get();
	}

	@Test(expected = TimeoutException.class)
	public void lockIsNotGrantedIfHeld() throws WorterbuchException, URISyntaxException, Exception {
		final var key = "acquireLock/lockIsNotGrantedIfHeld";

		try (final var secondClient = AcquireLock.WB.startSecondClient()) {

			try {
				secondClient.acquireLock(key).result().get(100, TimeUnit.MILLISECONDS).get();
			} catch (final TimeoutException e) {
				// this one is supposed to succeed
				Assert.fail();
			}

			// this one is supposed to fail
			AcquireLock.WB.client.acquireLock(key).result().get(100, TimeUnit.MILLISECONDS).get();

		}
	}

	@Test
	public void lockIsGrantedAfterReleased() throws WorterbuchException, URISyntaxException, Exception {
		final var key = "acquireLock/lockIsGrantedAfterReleased";

		final var latch = new CountDownLatch(1);

		try (final var secondClient = AcquireLock.WB.startSecondClient()) {

			try {
				secondClient.acquireLock(key).result().get(100, TimeUnit.MILLISECONDS).get();
			} catch (final TimeoutException e) {
				// this one is supposed to succeed
				Assert.fail();
			}

			AcquireLock.WB.client.acquireLock(key).result().thenAcceptAsync(it -> latch.countDown());

			// this one is supposed to block
			Assert.assertFalse(latch.await(100, TimeUnit.MILLISECONDS));

			// closing seconds client here should release the lock
		}

		// this one is NOT supposed to block
		Assert.assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
	}

}
