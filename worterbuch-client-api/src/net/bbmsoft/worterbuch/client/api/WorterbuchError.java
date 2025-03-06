package net.bbmsoft.worterbuch.client.api;

import net.bbmsoft.worterbuch.client.model.Err;

public class WorterbuchError extends Exception {

	private static final long serialVersionUID = 6205646760526253152L;

	public final Err errorMessage;

	public WorterbuchError(final Err errorMessage) {
		this.errorMessage = errorMessage;
	}
}
