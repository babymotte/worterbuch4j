package net.bbmsoft.worterbuch.tcp.client.messages;

import java.util.Map;
import java.util.Optional;

public record ServerMessage(MessageType type, long transactionID, Optional<String> requestPattern,
		Optional<Map<String, String>> keyValuePairs, Optional<Byte> errorCode, Optional<String> metadata) {
}
