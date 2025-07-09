package net.bbmsoft.worterbuch.client.error;

public final class UnhandledInternalException extends WorterbuchException {

	private static final long serialVersionUID = 4335794329363315991L;

	public UnhandledInternalException(final Throwable cause) {
		super("Unhandled internal exception", cause);
	}
}
