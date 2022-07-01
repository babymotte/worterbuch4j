package net.bbmsoft.worterbuch.tcp.client.messages;

import java.util.Optional;

public record StateMessage(long transactionId, String key, Optional<String> value) implements ServerMessage {

	@Override
	public MessageType type() {
		return MessageType.STATE;
	}
}
