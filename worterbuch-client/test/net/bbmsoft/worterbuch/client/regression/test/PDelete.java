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

public class PDelete {

	private static ContainerizedWB WB;

	@BeforeClass
	public static void before() throws URISyntaxException, TimeoutException, WorterbuchException, InterruptedException {
		PDelete.WB = new ContainerizedWB();
		PDelete.WB.start();
	}

	@AfterClass
	public static void after() throws Exception {
		PDelete.WB.close();
	}

	@Test
	public void concreteValuesGetDeletedQuietly()
			throws NoSuchElementException, WorterbuchException, InterruptedException, TimeoutException {

		final var key = "pDelete/concreteValuesGetDeletedQuietly/test/1";
		final var value = "123";

		PDelete.WB.client.set(key, value);
		Assert.assertEquals(value, PDelete.WB.client.get(key, String.class).await(Duration.ofMillis(100)).value());

		Assert.assertNull(PDelete.WB.client.pDelete(key).await(Duration.ofMillis(100)).value());
		Assert.assertEquals(ErrorCode.NoSuchValue,
				PDelete.WB.client.get(key, String.class).await(Duration.ofMillis(100)).err().getErrorCode());

	}

	@Test
	public void concreteValuesGetDeleted()
			throws NoSuchElementException, WorterbuchException, InterruptedException, TimeoutException {

		final var key = "pDelete/concreteValuesGetDeleted/test/1";
		final var value = "123";

		PDelete.WB.client.set(key, value);
		Assert.assertEquals(value, PDelete.WB.client.get(key, String.class).await(Duration.ofMillis(100)).value());

		PDelete.WB.client.pDelete(key, String.class).await(Duration.ofMillis(100));
		Assert.assertEquals(ErrorCode.NoSuchValue,
				PDelete.WB.client.get(key, String.class).await(Duration.ofMillis(100)).err().getErrorCode());

	}

	@Test
	public void singleWildcardValuesGetDeletedQuietly()
			throws NoSuchElementException, WorterbuchException, InterruptedException, TimeoutException {

		final var key = "pDelete/singleWildcardValuesGetDeletedQuietly/test/1";
		final var pattern = "pDelete/singleWildcardValuesGetDeletedQuietly/?/1";
		final var value = "123";

		PDelete.WB.client.set(key, value);
		Assert.assertEquals(value, PDelete.WB.client.get(key, String.class).await(Duration.ofMillis(100)).value());

		Assert.assertNull(PDelete.WB.client.pDelete(pattern).await(Duration.ofMillis(100)).value());
		Assert.assertEquals(ErrorCode.NoSuchValue,
				PDelete.WB.client.get(key, String.class).await(Duration.ofMillis(100)).err().getErrorCode());

	}

	@Test
	public void singleWildcardValuesGetDeleted()
			throws NoSuchElementException, WorterbuchException, InterruptedException, TimeoutException {

		final var key = "pDelete/singleWildcardValuesGetDeleted/test/1";
		final var pattern = "pDelete/singleWildcardValuesGetDeleted/?/1";
		final var value = "123";

		PDelete.WB.client.set(key, value);
		Assert.assertEquals(value, PDelete.WB.client.get(key, String.class).await(Duration.ofMillis(100)).value());

		PDelete.WB.client.pDelete(pattern, String.class).await(Duration.ofMillis(100));
		Assert.assertEquals(ErrorCode.NoSuchValue,
				PDelete.WB.client.get(key, String.class).await(Duration.ofMillis(100)).err().getErrorCode());

	}

	@Test
	public void multiWildcardValuesGetDeletedQuietly()
			throws NoSuchElementException, WorterbuchException, InterruptedException, TimeoutException {

		final var key = "pDelete/multiWildcardValuesGetDeletedQuietly/test/1";
		final var pattern = "pDelete/multiWildcardValuesGetDeletedQuietly/#";
		final var value = "123";

		PDelete.WB.client.set(key, value);
		Assert.assertEquals(value, PDelete.WB.client.get(key, String.class).await(Duration.ofMillis(100)).value());

		Assert.assertNull(PDelete.WB.client.pDelete(pattern).await(Duration.ofMillis(100)).value());
		Assert.assertEquals(ErrorCode.NoSuchValue,
				PDelete.WB.client.get(key, String.class).await(Duration.ofMillis(100)).err().getErrorCode());

	}

	@Test
	public void multiWildcardValuesGetDeleted()
			throws NoSuchElementException, WorterbuchException, InterruptedException, TimeoutException {

		final var key = "pDelete/multiWildcardValuesGetDeleted/test/1";
		final var pattern = "pDelete/multiWildcardValuesGetDeleted/#";
		final var value = "123";

		PDelete.WB.client.set(key, value);
		Assert.assertEquals(value, PDelete.WB.client.get(key, String.class).await(Duration.ofMillis(100)).value());

		PDelete.WB.client.pDelete(pattern, String.class).await(Duration.ofMillis(100));
		Assert.assertEquals(ErrorCode.NoSuchValue,
				PDelete.WB.client.get(key, String.class).await(Duration.ofMillis(100)).err().getErrorCode());

	}

}
