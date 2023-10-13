package net.bbmsoft.worterbuch.client.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import net.bbmsoft.worterbuch.client.ClientSocket;

public class TcpClientSocket implements ClientSocket {

	private final Socket socket;
	private final PrintStream outs;
	private final InputStream ins;
	private Thread receiveThread;
	private final BiConsumer<Integer, String> onDisconnect;
	private final Consumer<Throwable> onError;

	public TcpClientSocket(final Socket socket, final BiConsumer<Integer, String> onDisconnect,
			final Consumer<Throwable> onError) throws IOException {
		this.socket = socket;
		this.onDisconnect = onDisconnect;
		this.onError = onError;
		this.outs = new PrintStream(socket.getOutputStream(), true, StandardCharsets.UTF_8);
		this.ins = socket.getInputStream();
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

		this.outs.close();

		try {
			this.ins.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		try {
			this.socket.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private void receiveLoop(final Consumer<String> messageConsumer) {

		try (var reader = new BufferedReader(new InputStreamReader(this.ins, StandardCharsets.UTF_8));) {
			for (var line = reader.readLine(); line != null; line = reader.readLine()) {
				messageConsumer.accept(line);
			}
		} catch (final Exception e) {
			this.onError.accept(e);
		} finally {
			this.onDisconnect.accept(1, "Receive loop closed.");
		}

	}
}
