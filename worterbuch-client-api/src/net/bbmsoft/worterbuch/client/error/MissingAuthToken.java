package net.bbmsoft.worterbuch.client.error;

public final class MissingAuthToken extends WorterbuchException {

	private static final long serialVersionUID = 8822757262252227293L;

	public MissingAuthToken() {
		super("server requires authorization but no auth token was provided");
	}
}
