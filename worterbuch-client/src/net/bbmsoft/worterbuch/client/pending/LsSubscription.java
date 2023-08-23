package net.bbmsoft.worterbuch.client.pending;

import java.util.List;
import java.util.function.Consumer;

public class LsSubscription {

	public final Consumer<List<String>> callback;

	public LsSubscription(final Consumer<List<String>> callback) {
		super();
		this.callback = callback;
	}
}
