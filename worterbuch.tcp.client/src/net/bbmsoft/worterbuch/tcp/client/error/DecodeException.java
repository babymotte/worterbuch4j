package net.bbmsoft.worterbuch.tcp.client.error;

import java.io.IOException;

public class DecodeException extends Exception {

	private static final long serialVersionUID = 326459937192577121L;

	public DecodeException() {
	}

	public DecodeException(final IOException e) {
		super(e);
	}

}
