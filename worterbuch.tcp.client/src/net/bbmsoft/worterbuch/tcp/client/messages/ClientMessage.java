package net.bbmsoft.worterbuch.tcp.client.messages;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import net.bbmsoft.worterbuch.client.api.Constants;
import net.bbmsoft.worterbuch.tcp.client.error.EncoderException;
import net.bbmsoft.worterbuch.tcp.client.error.KeyTooLong;
import net.bbmsoft.worterbuch.tcp.client.error.PatternTooLong;
import net.bbmsoft.worterbuch.tcp.client.error.ValueTooLong;
import net.bbmsoft.worterbuch.tcp.client.utils.ByteUtils;

public class ClientMessage {

	public static byte[] encodeGet(final long transactionId, final String key) throws EncoderException {

		Objects.requireNonNull(key, "key must not be NULL");
		if (key.isBlank()) {
			throw new IllegalArgumentException("key must not be blank");
		}
		if (key.contains(Constants.WILDCARD)) {
			throw new IllegalArgumentException("key must not contain wildcards");
		}
		if (key.contains(Constants.MULTI_WILDCARD)) {
			throw new IllegalArgumentException("key must not contain multi-wildcards");
		}

		final var keyBytes = key.getBytes(StandardCharsets.UTF_8);
		if (keyBytes.length > Constants.MAX_KEY_LENGTH) {
			throw new PatternTooLong("Cannot encode patterns of length " + keyBytes.length);
		}

		final var outLength = Constants.MESSAGE_TYPE_BYTES + Constants.TRANSACTION_ID_BYTES + Constants.KEY_LENGTH_BYTES
				+ keyBytes.length;

		final var buf = ByteBuffer.allocate(outLength);
		buf.put(MessageType.GET.toByte());
		buf.put(ByteUtils.longToBytes(transactionId));
		buf.put(ByteUtils.shortToBytes(keyBytes.length));
		buf.put(keyBytes);

		return buf.array();
	}

	public static byte[] encodePGet(final long transactionId, final String pattern) throws EncoderException {

		Objects.requireNonNull(pattern, "pattern must not be NULL");
		if (pattern.isBlank()) {
			throw new IllegalArgumentException("pattern must not be blank");
		}
		if (pattern.substring(0, pattern.length() - 1).contains(Constants.MULTI_WILDCARD)) {
			throw new IllegalArgumentException("multi-wildcards are only allowed as the last element");
		}

		final var patternBytes = pattern.getBytes(StandardCharsets.UTF_8);
		if (patternBytes.length > Constants.MAX_PATTERN_LENGTH) {
			throw new PatternTooLong("Cannot encode patterns of length " + patternBytes.length);
		}

		final var outLength = Constants.MESSAGE_TYPE_BYTES + Constants.TRANSACTION_ID_BYTES
				+ Constants.PATTERN_LENGTH_BYTES + patternBytes.length;

		final var buf = ByteBuffer.allocate(outLength);
		buf.put(MessageType.PGET.toByte());
		buf.put(ByteUtils.longToBytes(transactionId));
		buf.put(ByteUtils.shortToBytes(patternBytes.length));
		buf.put(patternBytes);

		return buf.array();
	}

	public static byte[] encodeSet(final long transactionId, final String key, final String value)
			throws EncoderException {

		final var keyBytes = key.getBytes(StandardCharsets.UTF_8);
		if (keyBytes.length > Constants.MAX_KEY_LENGTH) {
			throw new KeyTooLong("Cannot encode keys of length " + keyBytes.length);
		}

		final var valueBytes = value.getBytes(StandardCharsets.UTF_8);
		if (valueBytes.length > Constants.MAX_VALUE_LENGTH) {
			throw new ValueTooLong("Cannot encode values of length " + valueBytes.length);
		}

		final var outLength = Constants.MESSAGE_TYPE_BYTES + Constants.TRANSACTION_ID_BYTES + Constants.KEY_LENGTH_BYTES
				+ Constants.VALUE_LENGTH_BYTES + keyBytes.length + valueBytes.length;

		final var buf = ByteBuffer.allocate(outLength);
		buf.put(MessageType.SET.toByte());
		buf.put(ByteUtils.longToBytes(transactionId));
		buf.put(ByteUtils.shortToBytes(keyBytes.length));
		buf.put(ByteUtils.intToBytes(valueBytes.length));
		buf.put(keyBytes);
		buf.put(valueBytes);

		return buf.array();
	}

	public static byte[] encodeSubscribe(final long transactionId, final String key) throws EncoderException {

		Objects.requireNonNull(key, "key must not be NULL");
		if (key.isBlank()) {
			throw new IllegalArgumentException("key must not be blank");
		}
		if (key.contains(Constants.WILDCARD)) {
			throw new IllegalArgumentException("key must not contain wildcards");
		}
		if (key.contains(Constants.MULTI_WILDCARD)) {
			throw new IllegalArgumentException("key must not contain multi-wildcards");
		}

		final var keyBytes = key.getBytes(StandardCharsets.UTF_8);
		if (keyBytes.length > Constants.MAX_KEY_LENGTH) {
			throw new PatternTooLong("Cannot encode patterns of length " + keyBytes.length);
		}

		final var outLength = Constants.MESSAGE_TYPE_BYTES + Constants.TRANSACTION_ID_BYTES + Constants.KEY_LENGTH_BYTES
				+ keyBytes.length;

		final var buf = ByteBuffer.allocate(outLength);
		buf.put(MessageType.SUBSCRIBE.toByte());
		buf.put(ByteUtils.longToBytes(transactionId));
		buf.put(ByteUtils.shortToBytes(keyBytes.length));
		buf.put(keyBytes);

		return buf.array();
	}

	public static byte[] encodePSubscribe(final long transactionId, final String pattern) throws EncoderException {

		Objects.requireNonNull(pattern, "pattern must not be NULL");
		if (pattern.isBlank()) {
			throw new IllegalArgumentException("pattern must not be blank");
		}
		if (pattern.substring(0, pattern.length() - 1).contains(Constants.MULTI_WILDCARD)) {
			throw new IllegalArgumentException("multi-wildcards are only allowed as the last element");
		}

		final var patternBytes = pattern.getBytes(StandardCharsets.UTF_8);
		if (patternBytes.length > Constants.MAX_PATTERN_LENGTH) {
			throw new PatternTooLong("Cannot encode patterns of length " + patternBytes.length);
		}

		final var outLength = Constants.MESSAGE_TYPE_BYTES + Constants.TRANSACTION_ID_BYTES
				+ Constants.PATTERN_LENGTH_BYTES + patternBytes.length;

		final var buf = ByteBuffer.allocate(outLength);
		buf.put(MessageType.PSUBSCRIBE.toByte());
		buf.put(ByteUtils.longToBytes(transactionId));
		buf.put(ByteUtils.shortToBytes(patternBytes.length));
		buf.put(patternBytes);

		return buf.array();
	}

}
