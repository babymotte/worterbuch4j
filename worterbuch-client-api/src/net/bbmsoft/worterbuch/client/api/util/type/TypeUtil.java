package net.bbmsoft.worterbuch.client.api.util.type;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Convenience functions for creating value types for deserialization
 */
public class TypeUtil {

	/**
	 * Creates a type object representing an {@link List List&lt;T&gt;}
	 *
	 * @param <T>
	 * @param elementType
	 * @return
	 */
	public static <T> TypeReference<List<T>> list(final Class<T> elementType) {
		return new TypeReference<>() {
		};
	}

	/**
	 * Creates a type object representing an {@link ArrayList ArrayList&lt;T&gt;}
	 *
	 * @param <T>
	 * @param elementType
	 * @return
	 */
	public static <T> TypeReference<ArrayList<T>> arrayList(final Class<T> elementType) {
		return new TypeReference<>() {
		};
	}

	/**
	 * Creates a type object representing a {@link LinkedList LinkedList&lt;T&gt;}
	 *
	 * @param <T>
	 * @param elementType
	 * @return
	 */
	public static <T> TypeReference<LinkedList<T>> linkedList(final Class<T> elementType) {
		return new TypeReference<>() {
		};
	}

	/**
	 * Creates a type object representing a {@link Set Set&lt;T&gt;}
	 *
	 * @param <T>
	 * @param elementType
	 * @return
	 */
	public static <T> TypeReference<Set<T>> set(final Class<T> elementType) {
		return new TypeReference<>() {
		};
	}

	/**
	 * Creates a type object representing a {@link HashSet HashSet&lt;T&gt;}
	 *
	 * @param <T>
	 * @param elementType
	 * @return
	 */
	public static <T> TypeReference<HashSet<T>> hashSet(final Class<T> elementType) {
		return new TypeReference<>() {
		};
	}

	/**
	 * Creates a type object representing a {@link LinkedHashSet
	 * LinkedHashSet&lt;T&gt;}
	 *
	 * @param <T>
	 * @param elementType
	 * @return
	 */
	public static <T> TypeReference<LinkedHashSet<T>> linkedSet(final Class<T> elementType) {
		return new TypeReference<>() {
		};
	}

	/**
	 * Creates a type object representing a {@link TreeSet TreeSet&lt;T&gt;}
	 *
	 * @param <T>
	 * @param elementType
	 * @return
	 */
	public static <T> TypeReference<TreeSet<T>> treeSet(final Class<T> elementType) {
		return new TypeReference<>() {
		};
	}

	/**
	 * Creates a type object representing a {@link Map Map&lt;String, T&gt;}
	 *
	 * @param <T>
	 * @param elementType
	 * @return
	 */
	public static <T> TypeReference<Map<String, T>> map(final Class<T> valueType) {
		return new TypeReference<>() {
		};
	}

	/**
	 * Creates a type object representing a {@link HashMap HashMap&lt;String, T&gt;}
	 *
	 * @param <T>
	 * @param elementType
	 * @return
	 */
	public static <T> TypeReference<HashMap<String, T>> hashMap(final Class<T> valueType) {
		return new TypeReference<>() {
		};
	}

	/**
	 * Creates a type object representing a {@link LinkedHashMap
	 * LinkedHashMap&lt;String, T&gt;}
	 *
	 * @param <T>
	 * @param elementType
	 * @return
	 */
	public static <T> TypeReference<LinkedHashMap<String, T>> linkedMap(final Class<T> valueType) {
		return new TypeReference<>() {
		};
	}

	/**
	 * Creates a type object representing a {@link TreeMap TreeMap&lt;String, T&gt;}
	 *
	 * @param <T>
	 * @param elementType
	 * @return
	 */
	public static <T> TypeReference<TreeMap<String, T>> treeMap(final Class<T> valueType) {
		return new TypeReference<>() {
		};
	}

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
