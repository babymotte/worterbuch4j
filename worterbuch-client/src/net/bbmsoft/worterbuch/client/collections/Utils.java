package net.bbmsoft.worterbuch.client.collections;

public class Utils {

	public static String escape(final String string) {
		return string.replace("/", "%2F");
	}

	public static String unescape(final String string) {
		return string.replace("%2F", "/");
	}

	public static String fullKey(final Object key, String rootKey) {
		return rootKey + "/" + Utils.escape(key.toString());
	}

	public static String trimKey(String fullKey, String rootKey) {
		return Utils.unescape(fullKey).substring(rootKey.length() + 1);
	}
}
