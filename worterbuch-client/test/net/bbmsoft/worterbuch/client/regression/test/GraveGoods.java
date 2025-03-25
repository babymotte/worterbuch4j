package net.bbmsoft.worterbuch.client.regression.test;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import net.bbmsoft.worterbuch.client.api.WorterbuchException;
import net.bbmsoft.worterbuch.client.regression.test.Util.ContainerizedWB;

public class GraveGoods {

	private static ContainerizedWB WB;

	@BeforeClass
	public static void before() throws URISyntaxException, TimeoutException, WorterbuchException, InterruptedException {
		GraveGoods.WB = new ContainerizedWB();
		GraveGoods.WB.start();
	}

	@AfterClass
	public static void after() throws Exception {
		GraveGoods.WB.close();
	}

	@Test
	public void graveGoodsAreSet() throws InterruptedException, ExecutionException {

		final var original = Arrays.asList("grave/goods/test/#", "some/more/#");

		GraveGoods.WB.client.setGraveGoods(original).result().get();
		final var value = GraveGoods.WB.client
				.getList("$SYS/clients/" + GraveGoods.WB.client.getClientId() + "/graveGoods", String.class).result()
				.get().get();

		Assert.assertEquals(original, value);

	}

	@Test
	public void graveGoodsAreUpdated() throws InterruptedException, ExecutionException {

		GraveGoods.WB.client.updateGraveGoods(l -> l.add("and/from/update"));

		final var value = GraveGoods.WB.client
				.getList("$SYS/clients/" + GraveGoods.WB.client.getClientId() + "/graveGoods", String.class).result()
				.get().get();

		Assert.assertEquals(Arrays.asList("grave/goods/test/#", "some/more/#", "and/from/update"), value);

	}
}
