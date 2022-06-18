package net.bbmsoft.worterbuch.client.api;

import java.util.Optional;

public class Constants {

	public static final int MESSAGE_TYPE_BYTES = 1;
	public static final int TRANSACTION_ID_BYTES = 8;
	public static final int KEY_LENGTH_BYTES = 2;
	public static final int PATTERN_LENGTH_BYTES = 2;
	public static final int VALUE_LENGTH_BYTES = 4;
	public static final int NUM_KEY_VALUE_PARIS_BYTES = 4;
	public static final int ERROR_CODE_BYTES = 1;
	public static final int METADATA_LENGTH_BYTES = 4;

	public static final long MAX_KEY_LENGTH = (long) (Math.pow(2, Constants.KEY_LENGTH_BYTES * 8) - 1);
	public static final long MAX_PATTERN_LENGTH = (long) (Math.pow(2, Constants.PATTERN_LENGTH_BYTES * 8) - 1);
	public static final long MAX_VALUE_LENGTH = (long) (Math.pow(2, Constants.VALUE_LENGTH_BYTES * 8) - 1);
	public static final long MAX_METADATA_LENGTH = (long) (Math.pow(2, Constants.METADATA_LENGTH_BYTES * 8) - 1);

	public static final String WILDCARD = Constants.prop("worterbuch.wildcard", "?");
	public static final String MULTI_WILDCARD = Constants.prop("worterbuch.multi.wildcard", "#");
	public static final String SEPARATOR = Constants.prop("worterbuch.separator", "/");

	private static final String prop(final String key, final String def) {
		return Optional.ofNullable(System.getProperty(key)).orElse(def);
	}
}
