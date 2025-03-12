package net.bbmsoft.worterbuch.client.impl;

import java.util.List;
import java.util.Optional;

import net.bbmsoft.worterbuch.client.api.Constants;

public class Protocol {

	public static Optional<Integer> compatibleProtocolVersion(final List<List<Object>> supportedVersions) {
		final var major = Constants.PROTOCOL_VERSION.major();
		final var minor = Constants.PROTOCOL_VERSION.minor();

		for (final List<Object> version : supportedVersions) {
			if (version.size() < 2) {
				continue;
			}
			final var majS = version.get(0);
			final var minS = version.get(1);
			if (majS instanceof final Number majorServer) {
				if (minS instanceof final Number minorServer) {
					if ((majorServer.intValue() == major) && (minorServer.intValue() >= minor)) {
						return Optional.of(major);
					}
				}
			}
		}

		return Optional.empty();
	}
}
