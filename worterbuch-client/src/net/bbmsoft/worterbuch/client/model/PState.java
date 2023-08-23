
package net.bbmsoft.worterbuch.client.model;

import java.util.List;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import net.bbmsoft.worterbuch.client.KeyValuePair;

/**
 * A message sent by the server in response to a PGet, PDelete or
 * Subscribe/PSubscribe message
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "transactionId", "requestPattern", "keyValuePairs", "deleted" })
@Generated("jsonschema2pojo")
public class PState<T> {

	/**
	 * A unique transaction ID (Required)
	 *
	 */
	@JsonProperty("transactionId")
	@JsonPropertyDescription("A unique transaction ID")
	private Long transactionId;
	/**
	 *
	 * (Required)
	 *
	 */
	@JsonProperty("requestPattern")
	private String requestPattern;
	@JsonProperty("keyValuePairs")
	private List<KeyValuePair<T>> keyValuePairs;
	@JsonProperty("deleted")
	private List<KeyValuePair<T>> deleted;

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
	 *
	 * (Required)
	 *
	 */
	@JsonProperty("requestPattern")
	public String getRequestPattern() {
		return this.requestPattern;
	}

	/**
	 *
	 * (Required)
	 *
	 */
	@JsonProperty("requestPattern")
	public void setRequestPattern(final String requestPattern) {
		this.requestPattern = requestPattern;
	}

	@JsonProperty("keyValuePairs")
	public List<KeyValuePair<T>> getKeyValuePairs() {
		return this.keyValuePairs;
	}

	@JsonProperty("keyValuePairs")
	public void setKeyValuePairs(final List<KeyValuePair<T>> keyValuePairs) {
		this.keyValuePairs = keyValuePairs;
	}

	@JsonProperty("deleted")
	public List<KeyValuePair<T>> getDeleted() {
		return this.deleted;
	}

	@JsonProperty("deleted")
	public void setDeleted(final List<KeyValuePair<T>> deleted) {
		this.deleted = deleted;
	}

	@Override
	public String toString() {
		final var sb = new StringBuilder();
		sb.append(PState.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this)))
				.append('[');
		sb.append("transactionId");
		sb.append('=');
		sb.append(((this.transactionId == null) ? "<null>" : this.transactionId));
		sb.append(',');
		sb.append("requestPattern");
		sb.append('=');
		sb.append(((this.requestPattern == null) ? "<null>" : this.requestPattern));
		sb.append(',');
		sb.append("keyValuePairs");
		sb.append('=');
		sb.append(((this.keyValuePairs == null) ? "<null>" : this.keyValuePairs));
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
		result = ((result * 31) + ((this.keyValuePairs == null) ? 0 : this.keyValuePairs.hashCode()));
		result = ((result * 31) + ((this.transactionId == null) ? 0 : this.transactionId.hashCode()));
		result = ((result * 31) + ((this.requestPattern == null) ? 0 : this.requestPattern.hashCode()));
		return result;
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if ((other instanceof PState) == false) {
			return false;
		}
		@SuppressWarnings("unchecked")
		final var rhs = ((PState<T>) other);
		return (((((this.deleted == rhs.deleted) || ((this.deleted != null) && this.deleted.equals(rhs.deleted)))
				&& ((this.keyValuePairs == rhs.keyValuePairs)
						|| ((this.keyValuePairs != null) && this.keyValuePairs.equals(rhs.keyValuePairs))))
				&& ((this.transactionId == rhs.transactionId)
						|| ((this.transactionId != null) && this.transactionId.equals(rhs.transactionId))))
				&& ((this.requestPattern == rhs.requestPattern)
						|| ((this.requestPattern != null) && this.requestPattern.equals(rhs.requestPattern))));
	}

}
