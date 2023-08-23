
package net.bbmsoft.worterbuch.client.model;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * A message sent by a client to request the values of all keys matching the
 * provided pattern from the server
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "transactionId", "requestPattern" })
@Generated("jsonschema2pojo")
public class PGet {

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

	@Override
	public String toString() {
		final var sb = new StringBuilder();
		sb.append(PGet.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this)))
				.append('[');
		sb.append("transactionId");
		sb.append('=');
		sb.append(((this.transactionId == null) ? "<null>" : this.transactionId));
		sb.append(',');
		sb.append("requestPattern");
		sb.append('=');
		sb.append(((this.requestPattern == null) ? "<null>" : this.requestPattern));
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
		result = ((result * 31) + ((this.requestPattern == null) ? 0 : this.requestPattern.hashCode()));
		return result;
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if ((other instanceof PGet) == false) {
			return false;
		}
		final var rhs = ((PGet) other);
		return (((this.transactionId == rhs.transactionId)
				|| ((this.transactionId != null) && this.transactionId.equals(rhs.transactionId)))
				&& ((this.requestPattern == rhs.requestPattern)
						|| ((this.requestPattern != null) && this.requestPattern.equals(rhs.requestPattern))));
	}

}
