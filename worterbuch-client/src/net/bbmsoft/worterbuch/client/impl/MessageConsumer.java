package net.bbmsoft.worterbuch.client.impl;

import net.bbmsoft.worterbuch.client.error.UnhandledCallbackException;

public interface MessageConsumer {

	public void accept(String message) throws UnhandledCallbackException;
}
