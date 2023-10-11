package net.bbmsoft.worterbuch.client.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.function.Consumer;

import net.bbmsoft.worterbuch.client.ClientSocket;

public class TcpClientSocket implements ClientSocket {

	private final Socket socket;
	private final PrintStream outs;
	private final InputStream ins;
	private Thread receiveThread;

	public TcpClientSocket(final Socket socket) throws IOException {
		this.socket = socket;
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

		try (var sc = new Scanner(this.ins)) {
			while (sc.hasNextLine()) {
				final var line = sc.nextLine();
				messageConsumer.accept(line);
			}
		}

	}
}
