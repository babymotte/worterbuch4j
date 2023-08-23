
package net.bbmsoft.worterbuch.client.model;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * A message sent by a client to publish a new value for a key. The value will
 * not be persisted on the server
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "transactionId", "key", "value" })
@Generated("jsonschema2pojo")
public class Publish {

	/**
	 * A unique transaction ID (Required)
	 * 
	 */
	@JsonProperty("transactionId")
	@JsonPropertyDescription("A unique transaction ID")
	private Long transactionId;
	/**
	 * The key for which to publish the value (Required)
	 * 
	 */
	@JsonProperty("key")
	@JsonPropertyDescription("The key for which to publish the value")
	private String key;
	/**
	 * The value to be published for the key (Required)
	 * 
	 */
	@JsonProperty("value")
	@JsonPropertyDescription("The value to be published for the key")
	private Object value;

	/**
	 * A unique transaction ID (Required)
	 * 
	 */
	@JsonProperty("transactionId")
	public Long getTransactionId() {
		return this.transactionId;
	}

	/**
	 * A unique transaction ID (Required)
	 * 
	 */
	@JsonProperty("transactionId")
	public void setTransactionId(final Long transactionId) {
		this.transactionId = transactionId;
	}

	/**
	 * The key for which to publish the value (Required)
	 * 
	 */
	@JsonProperty("key")
	public String getKey() {
		return this.key;
	}

	/**
	 * The key for which to publish the value (Required)
	 * 
	 */
	@JsonProperty("key")
	public void setKey(final String key) {
		this.key = key;
	}

	/**
	 * The value to be published for the key (Required)
	 * 
	 */
	@JsonProperty("value")
	public Object getValue() {
		return this.value;
	}

	/**
	 * The value to be published for the key (Required)
	 * 
	 */
	@JsonProperty("value")
	public void setValue(final Object value) {
		this.value = value;
	}

	@Override
	public String toString() {
		final var sb = new StringBuilder();
		sb.append(Publish.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this)))
				.append('[');
		sb.append("transactionId");
		sb.append('=');
		sb.append(((this.transactionId == null) ? "<null>" : this.transactionId));
		sb.append(',');
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
		result = ((result * 31) + ((this.transactionId == null) ? 0 : this.transactionId.hashCode()));
		result = ((result * 31) + ((this.key == null) ? 0 : this.key.hashCode()));
		return result;
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if ((other instanceof Publish) == false) {
			return false;
		}
		final var rhs = ((Publish) other);
		return ((((this.value == rhs.value) || ((this.value != null) && this.value.equals(rhs.value)))
				&& ((this.transactionId == rhs.transactionId)
						|| ((this.transactionId != null) && this.transactionId.equals(rhs.transactionId))))
				&& ((this.key == rhs.key) || ((this.key != null) && this.key.equals(rhs.key))));
	}

}
