package net.bbmsoft.worterbuch.client.impl.util.type;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Convenience functions for creating value types for deserialization
 */
public class TypeUtil {

	static String NAME_PREFIX = "class ";

	@SuppressWarnings("unchecked")
	public static <T> T instantiate(final Type type) {

		final var resolved = TypeFactory.defaultInstance().constructType(type);

		if (resolved.isArrayType()) {
			return (T) new Object[0];
		}

		final var clazz = resolved.isConcrete() ? resolved.getRawClass() : TypeUtil.defaultConcreteType(resolved);

		try {
			return (T) clazz.getConstructor().newInstance();
		} catch (final Exception e) {
			throw new IllegalStateException("Could not create new instance of type " + clazz, e);
		}
	}

	private static Class<?> defaultConcreteType(final JavaType resolved) {

		if (resolved.isCollectionLikeType()) {
			return ArrayList.class;
		} else if (resolved.isMapLikeType()) {
			return HashMap.class;
		} else {
			throw new IllegalStateException("Don't know how to instantiate type " + resolved.getRawClass());
		}
	}
}
