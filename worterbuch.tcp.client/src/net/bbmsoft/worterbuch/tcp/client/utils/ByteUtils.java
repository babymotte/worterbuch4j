package net.bbmsoft.worterbuch.tcp.client.utils;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class ByteUtils {

	private final ByteBuffer buf = ByteBuffer.allocate(8);

	public byte[] longToBytes(final long n) {
		this.buf.clear();
		this.buf.putLong(0, n);
		return Arrays.copyOf(this.buf.array(), 8);
	}

	public byte[] intToBytes(final long n) {
		this.buf.clear();
		this.buf.putLong(0, n);
		return Arrays.copyOfRange(this.buf.array(), 4, 8);
	}

	public byte[] shortToBytes(final int n) {
		this.buf.clear();
		this.buf.putLong(0, n);
		return Arrays.copyOfRange(this.buf.array(), 6, 8);
	}

	public long bytesToLong(final byte[] bytes) {
		this.buf.clear();
		this.buf.put(bytes, 0, 8);
		this.buf.flip();
		return this.buf.getLong();
	}

	public long bytesToInt(final byte[] bytes) {
		this.buf.clear();
		this.buf.put(new byte[4]);
		this.buf.put(bytes, 0, 4);
		this.buf.flip();
		return this.buf.getLong();
	}

	public int bytesToShort(final byte[] bytes) {
		this.buf.clear();
		this.buf.put(new byte[6]);
		this.buf.put(bytes, 0, 2);
		this.buf.flip();
		return (int) this.buf.getLong();
	}
}
