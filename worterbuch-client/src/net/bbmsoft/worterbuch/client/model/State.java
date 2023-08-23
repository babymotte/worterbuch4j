
package net.bbmsoft.worterbuch.client.model;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import net.bbmsoft.worterbuch.client.KeyValuePair;

/**
 * A message sent by the server in response to a Get or Delete message
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "transactionId", "keyValue", "deleted" })
@Generated("jsonschema2pojo")
public class State<T> {

	/**
	 * A unique transaction ID (Required)
	 *
	 */
	@JsonProperty("transactionId")
	@JsonPropertyDescription("A unique transaction ID")
	private Long transactionId;
	/**
	 * A key/value pair where the key is always a string and the value can be
	 * anything
	 *
	 */
	@JsonProperty("keyValue")
	@JsonPropertyDescription("A key/value pair where the key is always a string and the value can be anything")
	private KeyValuePair<T> keyValue;
	/**
	 * A key/value pair where the key is always a string and the value can be
	 * anything
	 *
	 */
	@JsonProperty("deleted")
	@JsonPropertyDescription("A key/value pair where the key is always a string and the value can be anything")
	private KeyValuePair<T> deleted;

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
	 * A key/value pair where the key is always a string and the value can be
	 * anything
	 *
	 */
	@JsonProperty("keyValue")
	public KeyValuePair<T> getKeyValue() {
		return this.keyValue;
	}

	/**
	 * A key/value pair where the key is always a string and the value can be
	 * anything
	 *
	 */
	@JsonProperty("keyValue")
	public void setKeyValue(final KeyValuePair<T> keyValue) {
		this.keyValue = keyValue;
	}

	/**
	 * A key/value pair where the key is always a string and the value can be
	 * anything
	 *
	 */
	@JsonProperty("deleted")
	public KeyValuePair<T> getDeleted() {
		return this.deleted;
	}

	/**
	 * A key/value pair where the key is always a string and the value can be
	 * anything
	 *
	 */
	@JsonProperty("deleted")
	public void setDeleted(final KeyValuePair<T> deleted) {
		this.deleted = deleted;
	}

	@Override
	public String toString() {
		final var sb = new StringBuilder();
		sb.append(State.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this)))
				.append('[');
		sb.append("transactionId");
		sb.append('=');
		sb.append(((this.transactionId == null) ? "<null>" : this.transactionId));
		sb.append(',');
		sb.append("keyValue");
		sb.append('=');
		sb.append(((this.keyValue == null) ? "<null>" : this.keyValue));
		sb.append(',');
		sb.append("deleted");
		sb.append('=');
		sb.append(((this.deleted == null) ? "<null>" : this.deleted));
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
		result = ((result * 31) + ((this.deleted == null) ? 0 : this.deleted.hashCode()));
		result = ((result * 31) + ((this.keyValue == null) ? 0 : this.keyValue.hashCode()));
		result = ((result * 31) + ((this.transactionId == null) ? 0 : this.transactionId.hashCode()));
		return result;
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if ((other instanceof State) == false) {
			return false;
		}
		final State<?> rhs = ((State<?>) other);
		return ((((this.deleted == rhs.deleted) || ((this.deleted != null) && this.deleted.equals(rhs.deleted)))
				&& ((this.keyValue == rhs.keyValue) || ((this.keyValue != null) && this.keyValue.equals(rhs.keyValue))))
				&& ((this.transactionId == rhs.transactionId)
						|| ((this.transactionId != null) && this.transactionId.equals(rhs.transactionId))));
	}

}
