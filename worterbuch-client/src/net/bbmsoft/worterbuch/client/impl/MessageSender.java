package net.bbmsoft.worterbuch.client.impl;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import net.bbmsoft.worterbuch.client.error.WorterbuchException;

public class MessageSender {

	private final ClientSocket client;
	private final Executor exec;
	private final Consumer<WorterbuchException> onError;

	public MessageSender(final ClientSocket client, final Executor exec, final Consumer<WorterbuchException> onError) {
		this.client = client;
		this.exec = exec;
		this.onError = onError;
	}

	public void sendMessage(final String json) {
		this.exec.execute(() -> this.trySendMessage(json));
	}

	private void trySendMessage(final String json) {
		try {
			this.client.sendString(json);
		} catch (final IOException e) {
			this.onError.accept(new WorterbuchException("Could not send message", e));
		} catch (final InterruptedException e) {
			this.onError.accept(new WorterbuchException("Interrupted while sending message", e));
		}
	}
}
