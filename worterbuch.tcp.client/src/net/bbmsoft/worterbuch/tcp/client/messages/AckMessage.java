package net.bbmsoft.worterbuch.tcp.client.messages;

public record AckMessage(long transactionId) implements ServerMessage {

	@Override
	public MessageType type() {
		return MessageType.ACK;
	}
}
