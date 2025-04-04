package net.bbmsoft.worterbuch.client.regression.test;

import java.net.URISyntaxException;
import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import net.bbmsoft.worterbuch.client.api.ErrorCode;
import net.bbmsoft.worterbuch.client.error.WorterbuchException;
import net.bbmsoft.worterbuch.client.regression.test.Util.ContainerizedWB;

public class Delete {

	private static ContainerizedWB WB;

	@BeforeClass
	public static void before() throws URISyntaxException, TimeoutException, WorterbuchException, InterruptedException {
		Delete.WB = new ContainerizedWB();
		Delete.WB.start();
	}

	@AfterClass
	public static void after() throws Exception {
		Delete.WB.close();
	}

	@Test
	public void valuesGetDeletedQuietly()
			throws NoSuchElementException, WorterbuchException, InterruptedException, TimeoutException {

		final var key = "delete/valuesGetDeletedQuietly/test/1";
		final var value = "123";

		Delete.WB.client.set(key, value);
		Assert.assertEquals(value, Delete.WB.client.get(key, String.class).await(Duration.ofMillis(100)).value());

		Assert.assertNull(Delete.WB.client.delete(key).await(Duration.ofMillis(100)).value());
		Assert.assertEquals(ErrorCode.NoSuchValue,
				Delete.WB.client.get(key, String.class).await(Duration.ofMillis(100)).err().getErrorCode());

	}

	@Test
	public void valuesGetDeleted()
			throws NoSuchElementException, WorterbuchException, InterruptedException, TimeoutException {

		final var key = "delete/valuesGetDeleted/test/1";
		final var value = "123";

		Delete.WB.client.set(key, value);
		Assert.assertEquals(value, Delete.WB.client.get(key, String.class).await(Duration.ofMillis(100)).value());

		Delete.WB.client.delete(key, String.class).await(Duration.ofMillis(100));
		Assert.assertEquals(ErrorCode.NoSuchValue,
				Delete.WB.client.get(key, String.class).await(Duration.ofMillis(100)).err().getErrorCode());

	}
}
