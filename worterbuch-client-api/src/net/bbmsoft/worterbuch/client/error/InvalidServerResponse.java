package net.bbmsoft.worterbuch.client.error;

public final class InvalidServerResponse extends WorterbuchException {

	private static final long serialVersionUID = 1133887634541354209L;

	public InvalidServerResponse() {
		super("Server sent invalid response");
	}

	public InvalidServerResponse(final String msg, final Throwable cause) {
		super(msg, cause);
	}
}
