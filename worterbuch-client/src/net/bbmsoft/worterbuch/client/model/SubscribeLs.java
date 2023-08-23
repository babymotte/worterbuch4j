
package net.bbmsoft.worterbuch.client.model;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * A message sent by a client to request a subscription to all direct sub-key
 * segments of the provided partial key
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "transactionId", "parent" })
@Generated("jsonschema2pojo")
public class SubscribeLs {

	/**
	 * A unique transaction ID (Required)
	 * 
	 */
	@JsonProperty("transactionId")
	@JsonPropertyDescription("A unique transaction ID")
	private Long transactionId;
	/**
	 * The parent partial key for which to list sub-key segments (Required)
	 * 
	 */
	@JsonProperty("parent")
	@JsonPropertyDescription("The parent partial key for which to list sub-key segments")
	private String parent;

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
	 * The parent partial key for which to list sub-key segments (Required)
	 * 
	 */
	@JsonProperty("parent")
	public String getParent() {
		return this.parent;
	}

	/**
	 * The parent partial key for which to list sub-key segments (Required)
	 * 
	 */
	@JsonProperty("parent")
	public void setParent(final String parent) {
		this.parent = parent;
	}

	@Override
	public String toString() {
		final var sb = new StringBuilder();
		sb.append(SubscribeLs.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this)))
				.append('[');
		sb.append("transactionId");
		sb.append('=');
		sb.append(((this.transactionId == null) ? "<null>" : this.transactionId));
		sb.append(',');
		sb.append("parent");
		sb.append('=');
		sb.append(((this.parent == null) ? "<null>" : this.parent));
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
		result = ((result * 31) + ((this.parent == null) ? 0 : this.parent.hashCode()));
		result = ((result * 31) + ((this.transactionId == null) ? 0 : this.transactionId.hashCode()));
		return result;
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if ((other instanceof SubscribeLs) == false) {
			return false;
		}
		final var rhs = ((SubscribeLs) other);
		return (((this.parent == rhs.parent) || ((this.parent != null) && this.parent.equals(rhs.parent)))
				&& ((this.transactionId == rhs.transactionId)
						|| ((this.transactionId != null) && this.transactionId.equals(rhs.transactionId))));
	}

}
