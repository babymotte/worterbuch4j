package net.bbmsoft.worterbuch.tcp.client.messages;

import net.bbmsoft.worterbuch.client.api.AsyncWorterbuchClient.Handshake;

public record HandshakeMessage(Handshake handshake) implements ServerMessage {

	@Override
	public MessageType type() {
		return MessageType.HSHK;
	}

	@Override
	public long transactionId() {
		return 0;
	}
}
