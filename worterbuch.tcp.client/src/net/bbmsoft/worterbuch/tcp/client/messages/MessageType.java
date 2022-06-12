package net.bbmsoft.worterbuch.tcp.client.messages;

import net.bbmsoft.worterbuch.tcp.client.error.DecodeException;
import net.bbmsoft.worterbuch.tcp.client.error.UndefinedMessageType;

public enum MessageType {
	GET(0b00000000), SET(0b00000001), SUBSCRIBE(0b00000010), STATE(0b10000000), ACK(0b10000001), EVENT(0b10000010),
	ERR(0b10000011);

	private final int typeByte;

	private MessageType(final int typeByte) {
		this.typeByte = typeByte;
	}

	public byte toByte() {
		return (byte) this.typeByte;
	}

	public static MessageType fromByte(final int typeByte) throws DecodeException {

		return switch (typeByte) {
		// client messages
		case 0b00000000: {
			yield MessageType.GET;
		}
		case 0b00000001: {
			yield MessageType.SET;
		}
		case 0b00000010: {
			yield MessageType.SUBSCRIBE;
		}
		// server messages
		case 0b10000000: {
			yield MessageType.STATE;
		}
		case 0b10000001: {
			yield MessageType.ACK;
		}
		case 0b10000010: {
			yield MessageType.EVENT;
		}
		case 0b10000011: {
			yield MessageType.ERR;
		}
		// undefined
		default:
			throw new UndefinedMessageType(typeByte);
		};

	}

}
