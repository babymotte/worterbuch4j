package net.bbmsoft.worterbuch.client.pending;

import java.util.List;
import java.util.function.Consumer;

public class PendingLsState {

	public final Consumer<List<String>> callback;

	public PendingLsState(final Consumer<List<String>> callback) {
		super();
		this.callback = callback;
	}
}
