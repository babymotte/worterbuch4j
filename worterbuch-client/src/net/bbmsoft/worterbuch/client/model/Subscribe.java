
package net.bbmsoft.worterbuch.client.model;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * A message sent by a client to subscribe to values of the provided key
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "transactionId", "key", "unique" })
@Generated("jsonschema2pojo")
public class Subscribe {

	/**
	 * A unique transaction ID (Required)
	 * 
	 */
	@JsonProperty("transactionId")
	@JsonPropertyDescription("A unique transaction ID")
	private Long transactionId;
	/**
	 * The key to subscribe to (Required)
	 * 
	 */
	@JsonProperty("key")
	@JsonPropertyDescription("The key to subscribe to")
	private String key;
	/**
	 * Indicate whether all or only unique values should be received (Required)
	 * 
	 */
	@JsonProperty("unique")
	@JsonPropertyDescription("Indicate whether all or only unique values should be received")
	private Boolean unique;

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
	 * The key to subscribe to (Required)
	 * 
	 */
	@JsonProperty("key")
	public String getKey() {
		return this.key;
	}

	/**
	 * The key to subscribe to (Required)
	 * 
	 */
	@JsonProperty("key")
	public void setKey(final String key) {
		this.key = key;
	}

	/**
	 * Indicate whether all or only unique values should be received (Required)
	 * 
	 */
	@JsonProperty("unique")
	public Boolean getUnique() {
		return this.unique;
	}

	/**
	 * Indicate whether all or only unique values should be received (Required)
	 * 
	 */
	@JsonProperty("unique")
	public void setUnique(final Boolean unique) {
		this.unique = unique;
	}

	@Override
	public String toString() {
		final var sb = new StringBuilder();
		sb.append(Subscribe.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this)))
				.append('[');
		sb.append("transactionId");
		sb.append('=');
		sb.append(((this.transactionId == null) ? "<null>" : this.transactionId));
		sb.append(',');
		sb.append("key");
		sb.append('=');
		sb.append(((this.key == null) ? "<null>" : this.key));
		sb.append(',');
		sb.append("unique");
		sb.append('=');
		sb.append(((this.unique == null) ? "<null>" : this.unique));
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
		result = ((result * 31) + ((this.transactionId == null) ? 0 : this.transactionId.hashCode()));
		result = ((result * 31) + ((this.key == null) ? 0 : this.key.hashCode()));
		result = ((result * 31) + ((this.unique == null) ? 0 : this.unique.hashCode()));
		return result;
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if ((other instanceof Subscribe) == false) {
			return false;
		}
		final var rhs = ((Subscribe) other);
		return ((((this.transactionId == rhs.transactionId)
				|| ((this.transactionId != null) && this.transactionId.equals(rhs.transactionId)))
				&& ((this.key == rhs.key) || ((this.key != null) && this.key.equals(rhs.key))))
				&& ((this.unique == rhs.unique) || ((this.unique != null) && this.unique.equals(rhs.unique))));
	}

}
