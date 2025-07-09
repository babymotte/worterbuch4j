package net.bbmsoft.worterbuch.client.error;

import net.bbmsoft.worterbuch.client.api.Constants;

public final class ProtocolVersionNotSupported extends WorterbuchException {

	private static final long serialVersionUID = -5928799679679251298L;

	public ProtocolVersionNotSupported() {
		super("Protocol version " + Constants.PROTOCOL_VERSION + " is not supported by the server.");
	}
}
