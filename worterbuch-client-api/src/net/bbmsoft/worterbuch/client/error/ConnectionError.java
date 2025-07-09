package net.bbmsoft.worterbuch.client.error;

public final class ConnectionError extends WorterbuchException {

	private static final long serialVersionUID = -7968537765890853142L;

	public ConnectionError(final String msg, final Throwable cause) {
		super(msg, cause);
	}
}
