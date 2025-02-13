package net.bbmsoft.worterbuch.client.error;

public class SerializationFailed extends RuntimeException {

	private static final long serialVersionUID = -5890810398616051260L;

	public SerializationFailed(final Exception cause) {
		super(cause);
	}

}
