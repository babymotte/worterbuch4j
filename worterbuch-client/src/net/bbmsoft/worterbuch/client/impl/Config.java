package net.bbmsoft.worterbuch.client.impl;

import java.util.Locale;
import java.util.Optional;

public class Config {

	public static final int KEEPALIVE_TIMEOUT = Config.getIntValue("WORTERBUCH_KEEPALIVE_TIMEOUT", 5) * 1_000;
	public static final int CONNECT_TIMEOUT = Config.getIntValue("WORTERBUCH_CONNECT_TIMEOUT", 5);

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
