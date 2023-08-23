
package net.bbmsoft.worterbuch.client;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * A key/value pair where the key is always a string and the value can be
 * anything
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "key", "value" })
@Generated("jsonschema2pojo")
public class KeyValuePair<T> {

	public static <T> KeyValuePair<T> of(final String key, final T value) {
		final var keyValuePair = new KeyValuePair<T>();
		keyValuePair.setKey(key);
		keyValuePair.setValue(value);
		return keyValuePair;
	}

	/**
	 * The key (Required)
	 *
	 */
	@JsonProperty("key")
	@JsonPropertyDescription("The key")
	private String key;
	/**
	 * The value (Required)
	 *
	 */
	@JsonProperty("value")
	@JsonPropertyDescription("The value")
	private T value;

	/**
	 * The key (Required)
	 *
	 */
	@JsonProperty("key")
	public String getKey() {
		return this.key;
	}

	/**
	 * The key (Required)
	 *
	 */
	@JsonProperty("key")
	public void setKey(final String key) {
		this.key = key;
	}

	/**
	 * The value (Required)
	 *
	 */
	@JsonProperty("value")
	public T getValue() {
		return this.value;
	}

	/**
	 * The value (Required)
	 *
	 */
	@JsonProperty("value")
	public void setValue(final T value) {
		this.value = value;
	}

	@Override
	public String toString() {
		final var sb = new StringBuilder();
		sb.append(KeyValuePair.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this)))
				.append('[');
		sb.append("key");
		sb.append('=');
		sb.append(((this.key == null) ? "<null>" : this.key));
		sb.append(',');
		sb.append("value");
		sb.append('=');
		sb.append(((this.value == null) ? "<null>" : this.value));
		sb.append(',');
		if (sb.charAt((sb.length() - 1)) == ',') {
			sb.setCharAt((sb.length() - 1), ']');
		} else {
			sb.append(']');
		}
		return sb.toString();
	}

	@Override
	public int hashCode() {
		var result = 1;
		result = ((result * 31) + ((this.value == null) ? 0 : this.value.hashCode()));
		result = ((result * 31) + ((this.key == null) ? 0 : this.key.hashCode()));
		return result;
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if ((other instanceof KeyValuePair) == false) {
			return false;
		}
		final KeyValuePair<?> rhs = ((KeyValuePair<?>) other);
		return (((this.value == rhs.value) || ((this.value != null) && this.value.equals(rhs.value)))
				&& ((this.key == rhs.key) || ((this.key != null) && this.key.equals(rhs.key))));
	}

}
