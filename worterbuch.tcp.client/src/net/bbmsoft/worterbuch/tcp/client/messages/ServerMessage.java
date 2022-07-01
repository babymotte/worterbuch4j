package net.bbmsoft.worterbuch.tcp.client.messages;

public interface ServerMessage {

	public long transactionId();

	public MessageType type();
}
