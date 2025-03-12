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

package net.bbmsoft.worterbuch.client.api;

import net.bbmsoft.worterbuch.client.error.WorterbuchError;

public class WorterbuchException extends Exception {

	private static final long serialVersionUID = 2381250353622788425L;

	public WorterbuchException() {
		super();
	}

	public WorterbuchException(final String message) {
		super(message);
	}

	public WorterbuchException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public WorterbuchException(final WorterbuchError cause) {
		super(cause);
	}

}
