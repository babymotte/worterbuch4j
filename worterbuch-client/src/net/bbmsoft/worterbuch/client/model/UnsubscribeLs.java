
package net.bbmsoft.worterbuch.client.model;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * A message sent by a client to request the cancellation of an ls subscription
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "transactionId" })
@Generated("jsonschema2pojo")
public class UnsubscribeLs {

	/**
	 * The transaction ID of the subscription to be cancelled (Required)
	 *
	 */
	@JsonProperty("transactionId")
	@JsonPropertyDescription("The transaction ID of the ls subscription to be cancelled")
	private Long transactionId;

	/**
	 * The transaction ID of the subscription to be cancelled (Required)
	 *
	 */
	@JsonProperty("transactionId")
	public Long getTransactionId() {
		return this.transactionId;
	}

	/**
	 * The transaction ID of the subscription to be cancelled (Required)
	 *
	 */
	@JsonProperty("transactionId")
	public void setTransactionId(final Long transactionId) {
		this.transactionId = transactionId;
	}

	@Override
	public String toString() {
		final var sb = new StringBuilder();
		sb.append(UnsubscribeLs.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this)))
				.append('[');
		sb.append("transactionId");
		sb.append('=');
		sb.append(((this.transactionId == null) ? "<null>" : this.transactionId));
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
		return result;
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if ((other instanceof UnsubscribeLs) == false) {
			return false;
		}
		final var rhs = ((UnsubscribeLs) other);
		return ((this.transactionId == rhs.transactionId)
				|| ((this.transactionId != null) && this.transactionId.equals(rhs.transactionId)));
	}

}
