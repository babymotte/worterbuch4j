package net.bbmsoft.worterbuch.client;

public class WorterbuchException extends Exception {

	private static final long serialVersionUID = 2381250353622788425L;

	public WorterbuchException() {
		super();
	}

	public WorterbuchException(final String message) {
		super(message);
	}

	public WorterbuchException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
