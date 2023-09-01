package net.bbmsoft.worterbuch.client.pending;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.function.Consumer;

public class PendingGet<T> {

	public final Consumer<Optional<T>> callback;

	public final Type type;

	public PendingGet(final Consumer<Optional<T>> callback, final Type type) {
		super();
		this.callback = callback;
		this.type = type;
	}

}
