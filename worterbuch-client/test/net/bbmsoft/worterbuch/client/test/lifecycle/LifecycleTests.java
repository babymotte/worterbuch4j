package net.bbmsoft.worterbuch.client.test.lifecycle;

import java.net.URISyntaxException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import net.bbmsoft.worterbuch.client.error.WorterbuchException;
import net.bbmsoft.worterbuch.client.impl.WorterbuchClientImpl;
import net.bbmsoft.worterbuch.client.response.Future;
import net.bbmsoft.worterbuch.client.test.common.Util.ContainerizedWB;

public class LifecycleTests {

	private static ContainerizedWB WB;

	@BeforeClass
	public static void before() throws URISyntaxException, TimeoutException, WorterbuchException, InterruptedException {
		LifecycleTests.WB = new ContainerizedWB();
		LifecycleTests.WB.start();

	}

	@AfterClass
	public static void after() throws Exception {
		LifecycleTests.WB.close();
	}

	@Test
	public void executorsAreClosedAlongWithClients() throws Exception {

		// create three clients
		final var client1 = LifecycleTests.WB.startSecondClient();
		final var client2 = LifecycleTests.WB.startSecondClient();
		final var client3 = LifecycleTests.WB.startSecondClient();

		// check if all three clients behave as expected
		Assert.assertFalse(client1.get("hello/world", Long.class).await(Duration.ofSeconds(1)).isOk());
		Assert.assertFalse(client2.get("hello/world", Long.class).await(Duration.ofSeconds(1)).isOk());
		Assert.assertFalse(client3.get("hello/world", Long.class).await(Duration.ofSeconds(1)).isOk());

		Assert.assertTrue(client3.set("hello/world", 42).await().isOk());

		Assert.assertEquals(42,
				client1.get("hello/world", Long.class).await(Duration.ofSeconds(1)).value().longValue());
		Assert.assertEquals(42,
				client2.get("hello/world", Long.class).await(Duration.ofSeconds(1)).value().longValue());
		Assert.assertEquals(42,
				client3.get("hello/world", Long.class).await(Duration.ofSeconds(1)).value().longValue());

		// stop client 2 and check if it gets cleaned up correctly
		client2.close();
		Assert.assertTrue(client2.executor().awaitTermination(1, TimeUnit.SECONDS));

		// stop client 3 and check if it gets cleaned up correctly
		client3.close();
		Assert.assertTrue(client3.executor().awaitTermination(1, TimeUnit.SECONDS));

		// check if client1 still works as expected
		Assert.assertTrue(client1.get("$SYS/uptime", Long.class).await(Duration.ofSeconds(1)).value() >= 0);

		client1.close();
		Assert.assertTrue(client1.executor().awaitTermination(1, TimeUnit.SECONDS));
	}

	@Test
	public void clientsArgeGarbageCollected() throws Exception {

		// close initial client to not pollute the memory profile
		LifecycleTests.WB.client.close();

		final var exception = new AtomicReference<Exception>(null);

		final var thread = new Thread(() -> {
			try {
				this.testGarbageCollection();
			} catch (TimeoutException | WorterbuchException | URISyntaxException | InterruptedException e) {
				exception.set(e);
			}
		});
		thread.start();
		thread.join();

		// TODO acquire another heap dump here, it should not contain the 200
		// WorterbuchClient instances we created on the other thread anymore

		final var e = exception.get();
		if (e != null) {
			throw e;
		}

	}

	private void testGarbageCollection()
			throws TimeoutException, WorterbuchException, URISyntaxException, InterruptedException {

		final var size = 200;
		final var clients = new WorterbuchClientImpl[size];
		final var futures = new Future[size];

		// create 200 clients
		for (var i = 0; i < size; i++) {
			clients[i] = LifecycleTests.WB.startSecondClient();
		}

		// do something with each client
		for (var i = 0; i < size; i++) {
			futures[i] = clients[i].set("hello/world", i);
		}
		for (var i = 0; i < size; i++) {
			Assert.assertTrue(futures[i].await().isOk());
		}

		// TODO acquire heap dump here, it should contain at least 200 WorterbuchClient
		// instances

		// close clients
		for (var i = 0; i < size; i++) {
			clients[i].close();
		}
		// wait for all clients to shut down
		for (var i = 0; i < size; i++) {
			Assert.assertTrue(clients[i].executor().awaitTermination(1, TimeUnit.SECONDS));
		}

	}
}
