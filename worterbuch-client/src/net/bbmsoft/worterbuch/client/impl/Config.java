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

package net.bbmsoft.worterbuch.client.impl;

import java.util.Locale;
import java.util.Optional;

public class Config {

	public static final int KEEPALIVE_TIMEOUT = Config.getIntValue("WORTERBUCH_KEEPALIVE_TIMEOUT", 5) * 1_000;
	public static final int CONNECT_TIMEOUT = Config.getIntValue("WORTERBUCH_CONNECT_TIMEOUT", 5);
	public static final int SEND_TIMEOUT = Config.getIntValue("WORTERBUCH_SEND_TIMEOUT", 5);
	public static final int CHANNEL_BUFFER_SIZE = Config.getIntValue("WORTERBUCH_CHANNEL_BUFFER_SIZE", 1);

	public static int getIntValue(final String key, final int defaultValue) {
		final var stringValue = Config.getValue(key);
		if (stringValue.isEmpty()) {
			return defaultValue;
		} else {
			final var strVal = stringValue.get();
			try {
				return Integer.parseInt(strVal);
			} catch (final NumberFormatException e) {
				return defaultValue;
			}
		}
	}

	private static Optional<String> getValue(final String key) {

		final var envVarName = key.replace('.', '_').toUpperCase(Locale.US);
		final var envVarValue = System.getenv().get(envVarName);
		if (envVarValue != null) {
			return Optional.of(envVarValue);
		}

		final var systemPropertyValue = System.getProperty(key);
		if (systemPropertyValue != null) {
			return Optional.of(systemPropertyValue);
		}

		return Optional.empty();
	}

}
