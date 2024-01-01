package net.bbmsoft.worterbuch.client.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bbmsoft.worterbuch.client.ClientSocket;

public class TcpClientSocket implements ClientSocket {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private final Socket socket;
	private final PrintStream outs;
	private final InputStream ins;
	private Thread receiveThread;
	private final BiConsumer<Integer, String> onDisconnect;
	private final Consumer<? super Throwable> onError;

	public TcpClientSocket(final URI uri, final BiConsumer<Integer, String> onDisconnect,
			final Consumer<? super Throwable> onError) throws IOException {

		this.socket = new Socket(uri.getHost(), uri.getPort());
		this.onDisconnect = onDisconnect;
		this.onError = onError;
		this.outs = new PrintStream(this.socket.getOutputStream(), true, StandardCharsets.UTF_8);
		this.ins = this.socket.getInputStream();
	}

	public void open(final Consumer<String> messageConsumer) {
		this.receiveThread = new Thread(() -> this.receiveLoop(messageConsumer), "wortebruch-client-tcp-rx");
		this.receiveThread.start();
	}

	@Override
	public void sendString(final String json) throws IOException {
		this.outs.println(json);
	}

	@Override
	public void close() {
		if (this.receiveThread != null) {
			this.receiveThread.interrupt();
		}

//		try {
//			this.ins.close();
//		} catch (final IOException e) {
//			this.log.error("Error closing socket input stream:", e);
//		}

//		this.outs.close();

		try {
			this.socket.close();
		} catch (final IOException e) {
			this.log.error("Error closing socket:", e);
		}
	}

	private void receiveLoop(final Consumer<String> messageConsumer) {

		try (var reader = new BufferedReader(new InputStreamReader(this.ins, StandardCharsets.UTF_8));) {
			for (var line = reader.readLine(); line != null; line = reader.readLine()) {
				messageConsumer.accept(line);
			}
		} catch (final IOException e) {
			if (Thread.currentThread().isInterrupted()) {
				this.log.debug("TCP socket was closed.");
			} else {
				this.onError.accept(e);
			}
		} finally {
			if (!Thread.currentThread().isInterrupted()) {
				this.onDisconnect.accept(1, "Receive loop closed.");
			}
		}

		this.log.debug("TCP socket receiver loop closed.");
	}
}
