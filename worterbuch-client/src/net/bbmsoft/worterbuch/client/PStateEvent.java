package net.bbmsoft.worterbuch.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PStateEvent<T> {

	public final List<KeyValuePair<T>> keyValuePairs;

	public final List<KeyValuePair<T>> deleted;

	public PStateEvent(final List<KeyValuePair<T>> keyValuePairs, final List<KeyValuePair<T>> deleted) {
		super();
		this.keyValuePairs = keyValuePairs != null ? new ArrayList<>(keyValuePairs) : null;
		this.deleted = deleted != null ? new ArrayList<>(deleted) : null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.deleted, this.keyValuePairs);
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
		final PStateEvent<?> other = (PStateEvent<?>) obj;
		return Objects.equals(this.deleted, other.deleted) && Objects.equals(this.keyValuePairs, other.keyValuePairs);
	}

	@Override
	public String toString() {
		return "PStateEvent [keyValuePairs=" + this.keyValuePairs + ", deleted=" + this.deleted + "]";
	}

}
