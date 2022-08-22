package net.bbmsoft.worterbuch.tcp.client.messages;

import net.bbmsoft.worterbuch.tcp.client.error.DecodeException;
import net.bbmsoft.worterbuch.tcp.client.error.UndefinedMessageType;

public enum MessageType {

	// server messages
	PSTATE(0b10000000), ACK(0b10000001), STATE(0b10000010), ERR(0b10000011), HSHK(0b10000100),
	// client messages
	GET(0b00000000), SET(0b00000001), SUBSCRIBE(0b00000010), PGET(0b00000011), PSUBSCRIBE(0b00000100);

	private final int typeByte;

	private MessageType(final int typeByte) {
		this.typeByte = typeByte;
	}

	public byte toByte() {
		return (byte) this.typeByte;
	}

	public static MessageType fromByte(final int typeByte) throws DecodeException {

		for (final MessageType messageType : MessageType.values()) {
			if (messageType.typeByte == typeByte) {
				return messageType;
			}
		}

		throw new UndefinedMessageType(typeByte);
	}

}
