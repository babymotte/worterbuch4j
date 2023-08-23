
package net.bbmsoft.worterbuch.client.model;

import java.util.List;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * A message sent by the server in response to an Ls message
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "transactionId", "children" })
@Generated("jsonschema2pojo")
public class LsState {

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
	@JsonProperty("children")
	private List<String> children;

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
	@JsonProperty("children")
	public List<String> getChildren() {
		return this.children;
	}

	/**
	 * 
	 * (Required)
	 * 
	 */
	@JsonProperty("children")
	public void setChildren(final List<String> children) {
		this.children = children;
	}

	@Override
	public String toString() {
		final var sb = new StringBuilder();
		sb.append(LsState.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this)))
				.append('[');
		sb.append("transactionId");
		sb.append('=');
		sb.append(((this.transactionId == null) ? "<null>" : this.transactionId));
		sb.append(',');
		sb.append("children");
		sb.append('=');
		sb.append(((this.children == null) ? "<null>" : this.children));
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
		result = ((result * 31) + ((this.children == null) ? 0 : this.children.hashCode()));
		result = ((result * 31) + ((this.transactionId == null) ? 0 : this.transactionId.hashCode()));
		return result;
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if ((other instanceof LsState) == false) {
			return false;
		}
		final var rhs = ((LsState) other);
		return (((this.children == rhs.children) || ((this.children != null) && this.children.equals(rhs.children)))
				&& ((this.transactionId == rhs.transactionId)
						|| ((this.transactionId != null) && this.transactionId.equals(rhs.transactionId))));
	}

}
