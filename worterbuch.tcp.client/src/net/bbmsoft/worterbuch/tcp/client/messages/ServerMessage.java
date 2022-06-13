package net.bbmsoft.worterbuch.tcp.client.messages;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import net.bbmsoft.worterbuch.tcp.client.error.DecodeException;
import net.bbmsoft.worterbuch.tcp.client.utils.ByteUtils;

public record ServerMessage(MessageType type, long transactionID, Optional<String> requestPattern,
		Optional<Map<String, String>> keyValuePairs, Optional<Byte> errorCode, Optional<String> metadata) {

	public static Optional<ServerMessage> read(final InputStream data) throws DecodeException {

		try {

			final var typeByte = data.read();
			if (typeByte == -1) {
				// server closed the connection
				return Optional.empty();
			}
			final var type = MessageType.fromByte(typeByte);

			return switch (type) {
			case STATE: {
				yield ServerMessage.readStateMessage(data);
			}
			case ACK: {
				yield ServerMessage.readAckMessage(data);
			}
			case EVENT: {
				yield ServerMessage.readEventMessage(data);
			}
			case ERR: {
				yield ServerMessage.readErrMessage(data);
			}
			default:
				yield Optional.empty();
			};
		} catch (final IOException e) {
			throw new DecodeException(e);
		}
	}

	private static Optional<ServerMessage> readStateMessage(final InputStream data) throws IOException {

		byte[] buf;

		buf = new byte[8];
		if (data.readNBytes(buf, 0, 8) < 8) {
			return Optional.empty();
		}
		final var transactionID = ByteUtils.bytesToLong(buf);

		buf = new byte[2];
		if (data.readNBytes(buf, 0, 2) < 2) {
			return Optional.empty();
		}
		final var requestPatternLength = ByteUtils.bytesToShort(buf);

		buf = new byte[4];
		if (data.readNBytes(buf, 0, 4) < 4) {
			return Optional.empty();
		}
		final var numKeyValuePairs = ByteUtils.bytesToInt(buf);

		final var keyValueLengths = new ArrayList<long[]>();

		for (var i = 0; i < numKeyValuePairs; i++) {

			buf = new byte[2];
			if (data.readNBytes(buf, 0, 2) < 2) {
				return Optional.empty();
			}
			final var keyLength = ByteUtils.bytesToShort(buf);

			buf = new byte[4];
			if (data.readNBytes(buf, 0, 4) < 4) {
				return Optional.empty();
			}
			final var valueLength = ByteUtils.bytesToInt(buf);

			keyValueLengths.add(new long[] { keyLength, valueLength });
		}

		buf = new byte[requestPatternLength];
		if (data.readNBytes(buf, 0, requestPatternLength) < requestPatternLength) {
			return Optional.empty();
		}
		final var requestPattern = new String(buf, StandardCharsets.UTF_8).intern();

		final var keyValuePairs = new HashMap<String, String>();

		for (final var lengths : keyValueLengths) {
			final var keyLength = (int) lengths[0];
			final var valueLength = lengths[1];

			buf = new byte[keyLength];
			if (data.readNBytes(buf, 0, keyLength) < keyLength) {
				return Optional.empty();
			}
			final var key = new String(buf, StandardCharsets.UTF_8).intern();

			// TODO this reduces the max length of a value by half due to java ints being
			// signed. How to fix this? (array can't be indexed using long)
			buf = new byte[(int) valueLength];
			if (data.readNBytes(buf, 0, (int) valueLength) < valueLength) {
				return Optional.empty();
			}
			final var value = new String(buf, StandardCharsets.UTF_8).intern();

			keyValuePairs.put(key, value);
		}

		return Optional.of(new ServerMessage(MessageType.STATE, transactionID, Optional.of(requestPattern),
				Optional.of(keyValuePairs), Optional.empty(), Optional.empty()));
	}

	private static Optional<ServerMessage> readAckMessage(final InputStream data) throws IOException {

		byte[] buf;

		buf = new byte[8];
		if (data.readNBytes(buf, 0, 8) < 8) {
			return Optional.empty();
		}
		final var transactionID = ByteUtils.bytesToLong(buf);

		return Optional.of(new ServerMessage(MessageType.ACK, transactionID, Optional.empty(), Optional.empty(),
				Optional.empty(), Optional.empty()));
	}

	private static Optional<ServerMessage> readEventMessage(final InputStream data) throws IOException {

		byte[] buf;

		buf = new byte[8];
		if (data.readNBytes(buf, 0, 8) < 8) {
			return Optional.empty();
		}
		final var transactionID = ByteUtils.bytesToLong(buf);

		buf = new byte[2];
		if (data.readNBytes(buf, 0, 2) < 2) {
			return Optional.empty();
		}
		final var requestPatternLength = ByteUtils.bytesToShort(buf);

		buf = new byte[2];
		if (data.readNBytes(buf, 0, 2) < 2) {
			return Optional.empty();
		}
		final var keyLength = ByteUtils.bytesToShort(buf);

		buf = new byte[4];
		if (data.readNBytes(buf, 0, 4) < 4) {
			return Optional.empty();
		}
		final var valueLength = ByteUtils.bytesToInt(buf);

		buf = new byte[requestPatternLength];
		if (data.readNBytes(buf, 0, requestPatternLength) < requestPatternLength) {
			return Optional.empty();
		}
		final var requestPattern = new String(buf, StandardCharsets.UTF_8).intern();

		buf = new byte[keyLength];
		if (data.readNBytes(buf, 0, keyLength) < keyLength) {
			return Optional.empty();
		}
		final var key = new String(buf, StandardCharsets.UTF_8).intern();

		// TODO this reduces the max length of a value by half due to java ints being
		// signed. How to fix this? (array can't be indexed using long)
		buf = new byte[(int) valueLength];
		if (data.readNBytes(buf, 0, (int) valueLength) < valueLength) {
			return Optional.empty();
		}
		final var value = new String(buf, StandardCharsets.UTF_8).intern();

		final var keyValuePairs = new HashMap<String, String>();
		keyValuePairs.put(key, value);

		return Optional.of(new ServerMessage(MessageType.EVENT, transactionID, Optional.of(requestPattern),
				Optional.of(keyValuePairs), Optional.empty(), Optional.empty()));
	}

	private static Optional<ServerMessage> readErrMessage(final InputStream data) throws IOException {

		byte[] buf;

		buf = new byte[8];
		if (data.readNBytes(buf, 0, 8) < 8) {
			return Optional.empty();
		}
		final var transactionID = ByteUtils.bytesToLong(buf);

		final var errorCode = data.read();

		buf = new byte[4];
		if (data.readNBytes(buf, 0, 4) < 4) {
			return Optional.empty();
		}
		final var metadataLength = ByteUtils.bytesToInt(buf);

		buf = new byte[(int) metadataLength];
		if (data.readNBytes(buf, 0, (int) metadataLength) < metadataLength) {
			return Optional.empty();
		}
		final var metadata = new String(buf, StandardCharsets.UTF_8).intern();

		return Optional.of(new ServerMessage(MessageType.EVENT, transactionID, Optional.empty(), Optional.empty(),
				Optional.of((byte) errorCode), Optional.of(metadata)));
	}
}
