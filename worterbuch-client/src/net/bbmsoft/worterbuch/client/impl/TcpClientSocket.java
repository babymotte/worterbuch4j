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
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bbmsoft.worterbuch.client.ClientSocket;

public class TcpClientSocket implements ClientSocket {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private final URI uri;
	private final AsynchronousSocketChannel socket;
	private final BiConsumer<Integer, String> onDisconnect;
	private final Consumer<? super Throwable> onError;
	private final LinkedBlockingQueue<String> outs;
	private final AtomicBoolean disconnected;

	private Thread receiveThread;
	private Thread transmitThread;

	public TcpClientSocket(final URI uri, final BiConsumer<Integer, String> onDisconnect,
			final Consumer<? super Throwable> onError, final int bufferSize) throws IOException {

		this.uri = uri;
		this.socket = AsynchronousSocketChannel.open();
		this.onDisconnect = onDisconnect;
		this.onError = onError;
		this.outs = new LinkedBlockingQueue<>(bufferSize);
		this.disconnected = new AtomicBoolean();

		this.socket.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
		this.socket.setOption(StandardSocketOptions.TCP_NODELAY, true);

	}

	public void open(final Consumer<String> messageConsumer, final long writeTimeout, final TimeUnit timeoutUnit) {
		this.socket.connect(new InetSocketAddress(this.uri.getHost(), this.uri.getPort()), this.socket,
				new CompletionHandler<Void, AsynchronousSocketChannel>() {
					@Override
					public void completed(final Void result, final AsynchronousSocketChannel channel) {
						TcpClientSocket.this.receiveThread = new Thread(() -> {
							TcpClientSocket.this.receiveLoop(messageConsumer);
						}, "wortebruch-client-tcp-rx");
						TcpClientSocket.this.receiveThread.start();
						TcpClientSocket.this.transmitThread = new Thread(() -> {
							TcpClientSocket.this.transmitLoop(writeTimeout, timeoutUnit);
						}, "wortebruch-client-tcp-tx");
						TcpClientSocket.this.transmitThread.start();
					}

					@Override
					public void failed(final Throwable exc, final AsynchronousSocketChannel channel) {
						TcpClientSocket.this.onError.accept(exc);
					}
				});
	}

	@Override
	public void sendString(final String json) throws IOException, InterruptedException {

		this.outs.put(json);

	}

	@Override
	public void close() {
		if (this.receiveThread != null) {
			this.receiveThread.interrupt();
		}
		if (this.transmitThread != null) {
			this.transmitThread.interrupt();
		}

		try {
			this.socket.close();
		} catch (final IOException e) {
			this.onError.accept(e);
		}
	}

	private void receiveLoop(final Consumer<String> messageConsumer) {

		final var buf = ByteBuffer.allocate(1024);
		final var sb = new Ref<StringBuilder>();
		sb.item = new StringBuilder();
		final var errorCode = new Ref<Integer>();
		final var message = new Ref<String>();

		var alreadyDisconnected = false;

		while (!Thread.currentThread().isInterrupted() && !this.disconnected.get()) {

			buf.clear();

			Integer read;
			try {
				read = this.socket.read(buf).get();
			} catch (final InterruptedException e) {
				alreadyDisconnected = this.interrupted(errorCode, message, e);
				break;
			} catch (final ExecutionException e) {
				alreadyDisconnected = this.socketReadException(errorCode, message, e.getCause());
				break;
			}

			if (read == -1) {
				alreadyDisconnected = this.disconnected.getAndSet(true);
				errorCode.item = 0;
				message.item = "stream closed";
				break;
			}

			var str = new String(buf.array(), 0, read, StandardCharsets.UTF_8);

			while (!str.isBlank()) {
				final var lineBreak = str.indexOf('\n');
				if (lineBreak == -1) {
					sb.item.append(str);
					break;
				} else {
					sb.item.append(str.substring(0, lineBreak));
					final var line = sb.item.toString();
					sb.item = new StringBuilder();
					if (!line.isBlank()) {
						messageConsumer.accept(line);
					}
					str = str.substring(lineBreak + 1);
				}
			}
		}

		this.log.debug("TCP socket receiver loop closed.");

		this.closed(errorCode, message, alreadyDisconnected);
	}

	private void transmitLoop(final long timeout, final TimeUnit timeoutUnit) {

		final var blockSize = 1024;
		final var buf = ByteBuffer.allocate(blockSize);
		final var errorCode = new Ref<Integer>();
		final var message = new Ref<String>();

		var alreadyDisconnected = false;

		while (!Thread.currentThread().isInterrupted() && !this.disconnected.get()) {

			try {
				final var json = this.outs.take();

				final var bytes = (json + "\n").getBytes(StandardCharsets.UTF_8);

				final var fullChunks = bytes.length / blockSize;
				final var partialChunkLen = bytes.length % blockSize;

				for (var i = 0; i < fullChunks; i++) {
					alreadyDisconnected = this.writeBuffer(timeout, timeoutUnit, buf, errorCode, message,
							alreadyDisconnected, bytes, i * blockSize, blockSize);

				}
				if (partialChunkLen != 0) {
					alreadyDisconnected = this.writeBuffer(timeout, timeoutUnit, buf, errorCode, message,
							alreadyDisconnected, bytes, fullChunks * blockSize, partialChunkLen);
				}

			} catch (final InterruptedException e) {
				alreadyDisconnected = this.interrupted(errorCode, message, e);
			}
		}

		this.log.debug("TCP socket transmitter loop closed.");

		this.closed(errorCode, message, alreadyDisconnected);
	}

	private boolean writeBuffer(final long timeout, final TimeUnit timeoutUnit, final ByteBuffer buf,
			final Ref<Integer> errorCode, final Ref<String> message, boolean alreadyDisconnected, final byte[] bytes,
			final int offset, final int len) throws InterruptedException {
		buf.clear();
		buf.put(bytes, offset, len);
		buf.flip();
		var written = 0;
		while (written < len) {
			try {
				written += this.socket.write(buf).get(timeout, timeoutUnit);
			} catch (ExecutionException | TimeoutException e) {
				alreadyDisconnected = this.socketWriteException(errorCode, message, e);
			}
		}
		return alreadyDisconnected;
	}

	private boolean interrupted(final Ref<Integer> errorCode, final Ref<String> message, final Throwable e) {
		final var alreadyDisconnected = this.disconnected.getAndSet(true);
		errorCode.item = 1;
		message.item = "thread interrupted";
		TcpClientSocket.this.onError.accept(e);
		Thread.currentThread().interrupt();
		return alreadyDisconnected;
	}

	private boolean socketReadException(final Ref<Integer> errorCode, final Ref<String> message, final Throwable e) {
		final var alreadyDisconnected = this.disconnected.getAndSet(true);
		errorCode.item = 2;
		message.item = "socket read error";
		TcpClientSocket.this.onError.accept(e);
		Thread.currentThread().interrupt();
		return alreadyDisconnected;
	}

	private boolean socketWriteException(final Ref<Integer> errorCode, final Ref<String> message, final Throwable e) {
		final var alreadyDisconnected = this.disconnected.getAndSet(true);
		errorCode.item = 3;
		message.item = "socket write error";
		TcpClientSocket.this.onError.accept(e);
		Thread.currentThread().interrupt();
		return alreadyDisconnected;
	}

	private void closed(final Ref<Integer> errorCode, final Ref<String> message, final boolean alreadyDisconnected) {
		if (!alreadyDisconnected) {
			this.onDisconnect.accept(errorCode.item, message.item);
			try {
				this.socket.close();
			} catch (final IOException e) {
				this.onError.accept(e);
			}
		}
	}
}
