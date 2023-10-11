package net.bbmsoft.worterbuch.client.impl;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import net.bbmsoft.worterbuch.client.ClientSocket;
import net.bbmsoft.worterbuch.client.WorterbuchException;

public class WsClientSocket implements ClientSocket, WriteCallback {

	private final WebSocketClient client;
	private final URI uri;
	private final Consumer<Throwable> onError;
	private Session session;

	public WsClientSocket(final WebSocketClient client, final URI uri, final Consumer<Throwable> onError) {
		this.client = client;
		this.uri = uri;
		this.onError = onError;
	}

	public void open(final WebSocketAdapter socket) throws IOException {

		try {
			this.session = this.client.connect(socket, this.uri).get(Config.CONNECT_TIMEOUT, TimeUnit.SECONDS);
			this.session.getPolicy().setMaxTextMessageSize(1024 * 1024 * 1024);
		} catch (final ExecutionException | IOException e) {
			this.onError.accept(new WorterbuchException("Failed to connect to server", e));
		} catch (final TimeoutException e) {
			this.onError.accept(new WorterbuchException("Connection to server timed out", e));
		} catch (final InterruptedException e) {
			this.onError.accept(new WorterbuchException("Client thread interrupted while establishing connection", e));
		}
	}

	@Override
	public void close() throws Exception {
		this.session.close();
		this.client.stop();
	}

	@Override
	public void sendString(final String json) throws IOException {
		this.session.getRemote().sendString(json, this);
	}

	@Override
	public void writeFailed(final Throwable x) {
		this.onError.accept(x);
	}

	@Override
	public void writeSuccess() {

	}

}
