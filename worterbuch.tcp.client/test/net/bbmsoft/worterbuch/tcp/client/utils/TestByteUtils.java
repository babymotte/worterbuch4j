package net.bbmsoft.worterbuch.tcp.client.utils;

import org.junit.Assert;
import org.junit.Test;

public class TestByteUtils {

	@Test
	public void testShortToBytes() {

		Assert.assertArrayEquals(ByteUtils.shortToBytes(0), new byte[] { 0b00000000, 0b00000000 });
		Assert.assertArrayEquals(ByteUtils.shortToBytes(1), new byte[] { 0b00000000, 0b00000001 });
		Assert.assertArrayEquals(ByteUtils.shortToBytes(2), new byte[] { 0b00000000, 0b00000010 });
		Assert.assertArrayEquals(ByteUtils.shortToBytes((int) Math.pow(2, 16) - 1),
				new byte[] { (byte) 0b11111111, (byte) 0b11111111 });
	}

	@Test
	public void testBytesToShort() {

		Assert.assertEquals(ByteUtils.bytesToShort(new byte[] { 0b00000000, 0b00000000 }), 0);
		Assert.assertEquals(ByteUtils.bytesToShort(new byte[] { 0b00000000, 0b00000001 }), 1);
		Assert.assertEquals(ByteUtils.bytesToShort(new byte[] { 0b00000000, 0b00000010 }), 2);
		Assert.assertEquals(ByteUtils.bytesToShort(new byte[] { (byte) 0b11111111, (byte) 0b11111111 }),
				(int) Math.pow(2, 16) - 1);

	}
}
