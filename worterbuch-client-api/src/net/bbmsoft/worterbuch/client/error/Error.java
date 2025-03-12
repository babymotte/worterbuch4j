package net.bbmsoft.worterbuch.client.error;

import java.util.Objects;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bbmsoft.worterbuch.client.model.Err;

public final class Error<T> implements Result<T> {

	private final Err error;

	@SuppressFBWarnings("EI_EXPOSE_REP2")
	public Error(final Err error) {
		this.error = error;
	}

	@Override
	public T get() {
		throw new IllegalStateException("called get on an error result: " + this.error);
	}

	@Override
	@SuppressFBWarnings("EI_EXPOSE_REP")
	public Err err() {
		return this.error;
	}

	@Override
	public boolean isOk() {
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.error);
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
		final var other = (Error<?>) obj;
		return Objects.equals(this.error, other.error);
	}

	@Override
	public String toString() {
		return "Error [error=" + this.error + "]";
	}

}
