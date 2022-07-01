package net.bbmsoft.worterbuch.tcp.client.messages;

public record ErrMessage(long transactionId, byte errorCode, String metadata) implements ServerMessage {

	@Override
	public MessageType type() {
		return MessageType.ERR;
	}
}
