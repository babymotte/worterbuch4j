package net.bbmsoft.worterbuch.tcp.client.messages;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

import net.bbmsoft.worterbuch.client.api.Constants;
import net.bbmsoft.worterbuch.tcp.client.error.DecodeException;
import net.bbmsoft.worterbuch.tcp.client.utils.ByteUtils;

public class ServerMessageDecoder {

	private final ByteUtils byteUtils = new ByteUtils();

	public Optional<ServerMessage> read(final InputStream data) throws DecodeException {

		try {

			final var typeByte = data.read();
			if (typeByte == -1) {
				// server closed the connection
				return Optional.empty();
			}
			final var type = MessageType.fromByte(typeByte);

			return switch (type) {
			case PSTATE: {
				yield this.readPStateMessage(data);
			}
			case ACK: {
				yield this.readAckMessage(data);
			}
			case STATE: {
				yield this.readStateMessage(data);
			}
			case ERR: {
				yield this.readErrMessage(data);
			}
			default:
				yield Optional.empty();
			};
		} catch (final IOException e) {
			throw new DecodeException(e);
		}
	}

	private Optional<ServerMessage> readPStateMessage(final InputStream data) throws IOException {

		byte[] buf;
		int bytes;

		buf = new byte[bytes = Constants.TRANSACTION_ID_BYTES];
		if (data.readNBytes(buf, 0, bytes) < bytes) {
			return Optional.empty();
		}
		final var transactionID = this.byteUtils.bytesToLong(buf);

		buf = new byte[bytes = Constants.PATTERN_LENGTH_BYTES];
		if (data.readNBytes(buf, 0, bytes) < bytes) {
			return Optional.empty();
		}
		final var requestPatternLength = this.byteUtils.bytesToShort(buf);

		buf = new byte[bytes = Constants.NUM_KEY_VALUE_PARIS_BYTES];
		if (data.readNBytes(buf, 0, bytes) < bytes) {
			return Optional.empty();
		}
		final var numKeyValuePairs = this.byteUtils.bytesToInt(buf);

		final var keyValueLengths = new ArrayList<long[]>();

		for (var i = 0; i < numKeyValuePairs; i++) {

			buf = new byte[bytes = Constants.KEY_LENGTH_BYTES];
			if (data.readNBytes(buf, 0, bytes) < bytes) {
				return Optional.empty();
			}
			final var keyLength = this.byteUtils.bytesToShort(buf);

			buf = new byte[bytes = Constants.VALUE_LENGTH_BYTES];
			if (data.readNBytes(buf, 0, bytes) < bytes) {
				return Optional.empty();
			}
			final var valueLength = this.byteUtils.bytesToInt(buf);

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

			buf = new byte[bytes = keyLength];
			if (data.readNBytes(buf, 0, bytes) < bytes) {
				return Optional.empty();
			}
			final var key = new String(buf, StandardCharsets.UTF_8).intern();

			// TODO this reduces the max length of a value by half due to java ints being
			// signed. How to fix this? (array can't be indexed using long)
			buf = new byte[bytes = (int) valueLength];
			if (data.readNBytes(buf, 0, bytes) < bytes) {
				return Optional.empty();
			}
			final var value = new String(buf, StandardCharsets.UTF_8).intern();

			keyValuePairs.put(key, value);
		}

		return Optional.of(new ServerMessage(MessageType.PSTATE, transactionID, Optional.of(requestPattern),
				Optional.of(keyValuePairs), Optional.empty(), Optional.empty()));
	}

	private Optional<ServerMessage> readAckMessage(final InputStream data) throws IOException {

		byte[] buf;
		int bytes;

		buf = new byte[bytes = Constants.TRANSACTION_ID_BYTES];
		if (data.readNBytes(buf, 0, bytes) < bytes) {
			return Optional.empty();
		}
		final var transactionID = this.byteUtils.bytesToLong(buf);

		return Optional.of(new ServerMessage(MessageType.ACK, transactionID, Optional.empty(), Optional.empty(),
				Optional.empty(), Optional.empty()));
	}

	private Optional<ServerMessage> readStateMessage(final InputStream data) throws IOException {

		byte[] buf;
		int bytes;

		buf = new byte[bytes = Constants.TRANSACTION_ID_BYTES];
		if (data.readNBytes(buf, 0, bytes) < bytes) {
			return Optional.empty();
		}
		final var transactionID = this.byteUtils.bytesToLong(buf);

		buf = new byte[bytes = Constants.KEY_LENGTH_BYTES];
		if (data.readNBytes(buf, 0, bytes) < bytes) {
			return Optional.empty();
		}
		final var keyLength = this.byteUtils.bytesToShort(buf);

		buf = new byte[bytes = Constants.VALUE_LENGTH_BYTES];
		if (data.readNBytes(buf, 0, bytes) < bytes) {
			return Optional.empty();
		}
		final var valueLength = this.byteUtils.bytesToInt(buf);

		buf = new byte[bytes = keyLength];
		if (data.readNBytes(buf, 0, bytes) < bytes) {
			return Optional.empty();
		}
		final var key = new String(buf, StandardCharsets.UTF_8).intern();

		// TODO this reduces the max length of a value by half due to java ints being
		// signed. How to fix this? (array can't be indexed using long)
		buf = new byte[bytes = (int) valueLength];
		if (data.readNBytes(buf, 0, bytes) < bytes) {
			return Optional.empty();
		}
		final var value = new String(buf, StandardCharsets.UTF_8).intern();

		final var keyValuePairs = new HashMap<String, String>();
		keyValuePairs.put(key, value);

		return Optional.of(new ServerMessage(MessageType.STATE, transactionID, Optional.empty(),
				Optional.of(keyValuePairs), Optional.empty(), Optional.empty()));
	}

	private Optional<ServerMessage> readErrMessage(final InputStream data) throws IOException {

		byte[] buf;
		int bytes;

		buf = new byte[bytes = Constants.TRANSACTION_ID_BYTES];
		if (data.readNBytes(buf, 0, bytes) < bytes) {
			return Optional.empty();
		}
		final var transactionID = this.byteUtils.bytesToLong(buf);

		final var errorCode = data.read();

		buf = new byte[bytes = Constants.METADATA_LENGTH_BYTES];
		if (data.readNBytes(buf, 0, bytes) < bytes) {
			return Optional.empty();
		}
		final var metadataLength = this.byteUtils.bytesToInt(buf);

		buf = new byte[bytes = (int) metadataLength];
		if (data.readNBytes(buf, 0, bytes) < bytes) {
			return Optional.empty();
		}
		final var metadata = new String(buf, StandardCharsets.UTF_8).intern();

		return Optional.of(new ServerMessage(MessageType.STATE, transactionID, Optional.empty(), Optional.empty(),
				Optional.of((byte) errorCode), Optional.of(metadata)));
	}
}
