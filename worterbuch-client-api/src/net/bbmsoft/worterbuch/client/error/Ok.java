package net.bbmsoft.worterbuch.client.error;

import java.util.Objects;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bbmsoft.worterbuch.client.model.Err;

public final class Ok<T> implements Result<T> {

	private final T result;

	@SuppressFBWarnings("EI_EXPOSE_REP2")
	public Ok(final T result) {
		this.result = result;
	}

	@Override
	@SuppressFBWarnings("EI_EXPOSE_REP")
	public T get() {
		return this.result;
	}

	@Override
	public Err err() {
		return null;
	}

	@Override
	public boolean isOk() {
		return true;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.result);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		final var other = (Ok<?>) obj;
		return Objects.equals(this.result, other.result);
	}

	@Override
	public String toString() {
		return "Ok [result=" + this.result + "]";
	}

}
