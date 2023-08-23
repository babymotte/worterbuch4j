package net.bbmsoft.worterbuch.client.pending;

import java.util.function.Consumer;

import net.bbmsoft.worterbuch.client.PStateEvent;

public class PSubscription<T> {

	public final Consumer<PStateEvent<T>> callback;

	public final Class<T> type;

	public PSubscription(final Consumer<PStateEvent<T>> callback, final Class<T> type) {
		super();
		this.callback = callback;
		this.type = type;
	}
}
