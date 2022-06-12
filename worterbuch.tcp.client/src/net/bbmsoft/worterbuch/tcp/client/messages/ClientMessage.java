package net.bbmsoft.worterbuch.tcp.client.messages;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import net.bbmsoft.worterbuch.tcp.client.error.EncoderException;
import net.bbmsoft.worterbuch.tcp.client.error.KeyTooLong;
import net.bbmsoft.worterbuch.tcp.client.error.PatternTooLong;
import net.bbmsoft.worterbuch.tcp.client.error.ValueTooLong;
import net.bbmsoft.worterbuch.tcp.client.utils.ByteUtils;

public class ClientMessage {

	public static byte[] encodeGet(final long transactionId, final String pattern) throws EncoderException {

		final var patternBytes = pattern.getBytes(StandardCharsets.UTF_8);
		if (patternBytes.length > (2 * Short.MAX_VALUE)) {
			throw new PatternTooLong("Cannot encode patterns of length " + patternBytes.length);
		}

		final var outLength = 1 + 8 + 2 + patternBytes.length;

		final var buf = ByteBuffer.allocate(outLength);
		buf.put(MessageType.GET.toByte());
		buf.put(ByteUtils.longToBytes(transactionId));
		buf.put(ByteUtils.shortToBytes(patternBytes.length));
		buf.put(patternBytes);

		return buf.array();
	}

	public static byte[] encodeSet(final long transactionId, final String key, final String value)
			throws EncoderException {

		final var keyBytes = key.getBytes(StandardCharsets.UTF_8);
		if (keyBytes.length > Math.pow(2, 16)) {
			throw new KeyTooLong("Cannot encode keys of length " + keyBytes.length);
		}

		final var valueBytes = value.getBytes(StandardCharsets.UTF_8);
		if (valueBytes.length > Math.pow(2, 32)) {
			throw new ValueTooLong("Cannot encode values of length " + valueBytes.length);
		}

		final var outLength = 1 + 8 + 2 + 4 + keyBytes.length + valueBytes.length;

		final var buf = ByteBuffer.allocate(outLength);
		buf.put(MessageType.SET.toByte());
		buf.put(ByteUtils.longToBytes(transactionId));
		buf.put(ByteUtils.shortToBytes(keyBytes.length));
		buf.put(ByteUtils.intToBytes(valueBytes.length));
		buf.put(keyBytes);
		buf.put(valueBytes);

		return buf.array();
	}

	public static byte[] encodeSubscribe(final long transactionId, final String pattern) throws EncoderException {

		final var patternBytes = pattern.getBytes(StandardCharsets.UTF_8);
		if (patternBytes.length > (2 * Short.MAX_VALUE)) {
			throw new PatternTooLong("Cannot encode patterns of length " + patternBytes.length);
		}

		final var outLength = 1 + 8 + 2 + patternBytes.length;

		final var buf = ByteBuffer.allocate(outLength);
		buf.put(MessageType.SUBSCRIBE.toByte());
		buf.put(ByteUtils.longToBytes(transactionId));
		buf.put(ByteUtils.shortToBytes(patternBytes.length));
		buf.put(patternBytes);

		return buf.array();
	}

}
