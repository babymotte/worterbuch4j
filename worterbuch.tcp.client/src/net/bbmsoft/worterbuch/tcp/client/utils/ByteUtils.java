package net.bbmsoft.worterbuch.tcp.client.utils;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class ByteUtils {

	private static ByteBuffer BUF = ByteBuffer.allocate(8);

	public static byte[] longToBytes(final long n) {
		ByteUtils.BUF.clear();
		ByteUtils.BUF.putLong(0, n);
		return Arrays.copyOf(ByteUtils.BUF.array(), 8);
	}

	public static byte[] intToBytes(final long n) {
		ByteUtils.BUF.clear();
		ByteUtils.BUF.putLong(0, n);
		return Arrays.copyOfRange(ByteUtils.BUF.array(), 4, 8);
	}

	public static byte[] shortToBytes(final int n) {
		ByteUtils.BUF.clear();
		ByteUtils.BUF.putLong(0, n);
		return Arrays.copyOfRange(ByteUtils.BUF.array(), 6, 8);
	}

	public static long bytesToLong(final byte[] bytes) {
		ByteUtils.BUF.clear();
		ByteUtils.BUF.put(bytes, 0, 8);
		ByteUtils.BUF.flip();
		return ByteUtils.BUF.getLong();
	}

	public static long bytesToInt(final byte[] bytes) {
		ByteUtils.BUF.clear();
		ByteUtils.BUF.put(new byte[4]);
		ByteUtils.BUF.put(bytes, 0, 4);
		ByteUtils.BUF.flip();
		return ByteUtils.BUF.getLong();
	}

	public static int bytesToShort(final byte[] bytes) {
		ByteUtils.BUF.clear();
		ByteUtils.BUF.put(new byte[6]);
		ByteUtils.BUF.put(bytes, 0, 2);
		ByteUtils.BUF.flip();
		return (int) ByteUtils.BUF.getLong();
	}
}
