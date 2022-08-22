package net.bbmsoft.worterbuch.tcp.client.messages;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

import net.bbmsoft.worterbuch.client.api.AsyncWorterbuchClient.Handshake;
import net.bbmsoft.worterbuch.client.api.AsyncWorterbuchClient.ProtocolVersion;
import net.bbmsoft.worterbuch.client.api.Constants;
import net.bbmsoft.worterbuch.tcp.client.error.DecodeException;
import net.bbmsoft.worterbuch.tcp.client.utils.ByteUtils;

public class ServerMessageDecoder {

	private final ByteUtils byteUtils = new ByteUtils();

	public Optional<? extends ServerMessage> read(final InputStream data) throws DecodeException {

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
			case HSHK: {
				yield this.readHandshakeMessage(data);
			}
			default:
				yield Optional.empty();
			};
		} catch (final IOException e) {
			throw new DecodeException(e);
		}
	}

	private Optional<PStateMessage> readPStateMessage(final InputStream data) throws IOException {

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

		return Optional.of(new PStateMessage(transactionID, requestPattern, keyValuePairs));
	}

	private Optional<AckMessage> readAckMessage(final InputStream data) throws IOException {

		byte[] buf;
		int bytes;

		buf = new byte[bytes = Constants.TRANSACTION_ID_BYTES];
		if (data.readNBytes(buf, 0, bytes) < bytes) {
			return Optional.empty();
		}
		final var transactionID = this.byteUtils.bytesToLong(buf);

		return Optional.of(new AckMessage(transactionID));
	}

	private Optional<StateMessage> readStateMessage(final InputStream data) throws IOException {

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

		Optional<String> value;

		if (valueLength > 0) {

			// TODO this reduces the max length of a value by half due to java ints being
			// signed. How to fix this? (array can't be indexed using long)
			buf = new byte[bytes = (int) valueLength];
			if (data.readNBytes(buf, 0, bytes) < bytes) {
				return Optional.empty();
			}

			value = Optional.of(new String(buf, StandardCharsets.UTF_8).intern());
		} else {
			value = Optional.empty();
		}

		return Optional.of(new StateMessage(transactionID, key, value));
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

		return Optional.of(new ErrMessage(transactionID, (byte) errorCode, metadata));
	}

	private Optional<HandshakeMessage> readHandshakeMessage(final InputStream data) throws IOException {

		byte[] buf;
		int bytes;

		final var numProtocolVersions = data.read();

		var supportedProtocolVersions = new ArrayList<ProtocolVersion>();

		for (int i = 0; i < numProtocolVersions; i++) {

			buf = new byte[bytes = Constants.PROTOCOL_VERSION_SEGMENT_BYTES];
			if (data.readNBytes(buf, 0, bytes) < bytes) {
				return Optional.empty();
			}
			var major = this.byteUtils.bytesToShort(buf);

			buf = new byte[bytes = Constants.PROTOCOL_VERSION_SEGMENT_BYTES];
			if (data.readNBytes(buf, 0, bytes) < bytes) {
				return Optional.empty();
			}
			var minor = this.byteUtils.bytesToShort(buf);

			supportedProtocolVersions.add(new ProtocolVersion(major, minor));
		}

		var separator = (char) data.read();
		var wildcard = (char) data.read();
		var multiWildcard = (char) data.read();

		return Optional
				.of(new HandshakeMessage(new Handshake(separator, wildcard, multiWildcard, supportedProtocolVersions)));
	}
}
