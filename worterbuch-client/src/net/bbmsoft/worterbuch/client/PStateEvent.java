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
