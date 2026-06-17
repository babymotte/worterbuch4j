/*
 *  Worterbuch Java client library
 *
 *  Copyright (C) 2024 Michael Bachmann
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.bbmsoft.worterbuch.client.impl;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import net.bbmsoft.worterbuch.client.error.ConnectionError;
import net.bbmsoft.worterbuch.client.error.WorterbuchException;

public class WsClientSocket implements ClientSocket, WriteCallback {

	private final WebSocketClient client;
	private final URI uri;
	private final Consumer<WorterbuchException> onError;
	private Session session;
	private final Optional<String> authtoken;
	private final ExecutorService executor;

	public WsClientSocket(final ExecutorService executor, final URI uri, final Consumer<WorterbuchException> onError,
			final Optional<String> authtoken) {
		this.executor = executor;
		this.client = new WebSocketClient();
		this.uri = uri;
		this.onError = onError;
		this.authtoken = authtoken;
	}

	public void open(final WebSocketAdapter socket) throws Exception {

		this.client.start();

		final var request = new ClientUpgradeRequest();
		request.setRequestURI(this.uri);
		request.setLocalEndpoint(this.client);
		this.authtoken.ifPresent(token -> request.setHeader("Authorization", "Bearer " + token));

		this.session = this.client.connect(socket, this.uri, request).get(Config.CONNECT_TIMEOUT, TimeUnit.SECONDS);
		this.session.getPolicy().setMaxTextMessageSize(1024 * 1024 * 1024);
	}

	@Override
	public void close() {
		this.session.close();
		try {
			this.client.stop();
		} catch (final Exception e) {
			final var error = new ConnectionError("Error closing websocket", e);
			final var onError = this.onError;
			this.executor.execute(() -> onError.accept(error));
		}
		this.executor.shutdown();
	}

	@Override
	public ExecutorService executor() {
		return this.executor;
	}

	@Override
	public void sendString(final String json) throws IOException {
		this.session.getRemote().sendString(json, this);
	}

	@Override
	public void writeFailed(final Throwable x) {
		final var error = new ConnectionError("error writing to socket", x);
		final var onError = this.onError;
		this.executor.execute(() -> onError.accept(error));
	}

	@Override
	public void writeSuccess() {

	}

}
