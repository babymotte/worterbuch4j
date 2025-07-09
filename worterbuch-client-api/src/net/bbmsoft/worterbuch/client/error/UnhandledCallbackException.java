package net.bbmsoft.worterbuch.client.error;

public final class UnhandledCallbackException extends WorterbuchException {

	private static final long serialVersionUID = 4335794329363315991L;

	public UnhandledCallbackException(final Throwable cause) {
		super("Unhandled exception in client callback", cause);
	}
}
