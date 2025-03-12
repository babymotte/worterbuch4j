package net.bbmsoft.worterbuch.client.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bbmsoft.worterbuch.client.error.SerializationFailed;
import net.bbmsoft.worterbuch.client.model.ClientMessage;

public class MessageSerDe {

	private final ObjectMapper objectMapper;

	@SuppressFBWarnings("EI_EXPOSE_REP2")
	public MessageSerDe(final ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public String serializeMessage(final ClientMessage msg) throws SerializationFailed {
		try {
			return msg != null ? this.objectMapper.writeValueAsString(msg) : "\"\"";
		} catch (final JsonProcessingException e) {
			throw new SerializationFailed(e);
		}
	}
}
