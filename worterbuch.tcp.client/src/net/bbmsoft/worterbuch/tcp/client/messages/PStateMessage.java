package net.bbmsoft.worterbuch.tcp.client.messages;

import java.util.Map;

public record PStateMessage(long transactionId, String requestPattern, Map<String, String> keyValuePairs)
		implements ServerMessage {

	@Override
	public MessageType type() {
		return MessageType.PSTATE;
	}
}
