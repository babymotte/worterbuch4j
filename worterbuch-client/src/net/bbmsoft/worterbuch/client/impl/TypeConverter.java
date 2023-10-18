package net.bbmsoft.worterbuch.client.impl;

import java.util.Collection;
import java.util.List;

import net.bbmsoft.worterbuch.client.KeyValuePair;

public class TypeConverter {

	public static net.bbmsoft.worterbuch.client.model.KeyValuePair convert(final KeyValuePair<?> kvp) {
		return new net.bbmsoft.worterbuch.client.model.KeyValuePair(kvp.getKey(), kvp.getValue());
	}

	public static List<net.bbmsoft.worterbuch.client.model.KeyValuePair> convert(
			final Collection<KeyValuePair<?>> kvps) {
		return kvps.stream().map(TypeConverter::convert).toList();
	}
}
