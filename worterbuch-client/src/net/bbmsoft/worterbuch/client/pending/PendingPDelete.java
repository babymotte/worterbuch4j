package net.bbmsoft.worterbuch.client.pending;

import java.util.List;
import java.util.function.Consumer;

import net.bbmsoft.worterbuch.client.KeyValuePair;

public class PendingPDelete<T> {

	public final Consumer<List<KeyValuePair<T>>> callback;

	public final Class<T> type;

	public PendingPDelete(final Consumer<List<KeyValuePair<T>>> callback, final Class<T> type) {
		super();
		this.callback = callback;
		this.type = type;
	}

}
