package net.bbmsoft.worterbuch.client;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import net.bbmsoft.worterbuch.client.api.WorterbuchClient;
import net.bbmsoft.worterbuch.client.error.ConnectionFailed;
import net.bbmsoft.worterbuch.client.error.WorterbuchException;
import net.bbmsoft.worterbuch.client.impl.Connector;

public class Worterbuch {

	public static WorterbuchClient connect(final Iterable<URI> uris, final BiConsumer<Integer, String> onDisconnect,
			final Consumer<WorterbuchException> onError) throws TimeoutException, ConnectionFailed {

		return new Connector(uris, Optional.empty(), onDisconnect, onError).connect();
	}

	public static WorterbuchClient connect(final Iterable<URI> uris, final String authToken,
			final BiConsumer<Integer, String> onDisconnect, final Consumer<WorterbuchException> onError)
			throws TimeoutException, ConnectionFailed {

		return new Connector(uris, Optional.ofNullable(authToken), onDisconnect, onError).connect();
	}
}
