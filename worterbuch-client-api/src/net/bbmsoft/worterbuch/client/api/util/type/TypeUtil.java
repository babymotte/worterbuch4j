package net.bbmsoft.worterbuch.client.api.util.type;

import java.lang.reflect.Type;
import java.util.List;

import com.fasterxml.jackson.databind.type.TypeFactory;

public class TypeUtil {

	public static <T> Type listType(final Class<T> elementType) {
		return TypeFactory.defaultInstance().constructCollectionType(List.class, elementType);
	}
}
