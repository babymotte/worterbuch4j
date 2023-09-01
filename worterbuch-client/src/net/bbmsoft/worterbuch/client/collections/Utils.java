package net.bbmsoft.worterbuch.client.collections;

public class Utils {

	public static String escape(final String string) {
		return string.replace("/", "%2F");
	}

	public static String unescape(final String string) {
		return string.replace("%2F", "/");
	}
}
