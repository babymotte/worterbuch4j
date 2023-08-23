
package net.bbmsoft.worterbuch.client.model;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * A message sent by the server to indicate that there was an error processing a
 * client message
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "transactionId", "errorCode", "metadata" })
@Generated("jsonschema2pojo")
public class Err {

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
	@JsonProperty("errorCode")
	private Short errorCode;
	/**
	 * 
	 * (Required)
	 * 
	 */
	@JsonProperty("metadata")
	private String metadata;

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
	@JsonProperty("errorCode")
	public Short getErrorCode() {
		return this.errorCode;
	}

	/**
	 * 
	 * (Required)
	 * 
	 */
	@JsonProperty("errorCode")
	public void setErrorCode(final Short errorCode) {
		this.errorCode = errorCode;
	}

	/**
	 * 
	 * (Required)
	 * 
	 */
	@JsonProperty("metadata")
	public String getMetadata() {
		return this.metadata;
	}

	/**
	 * 
	 * (Required)
	 * 
	 */
	@JsonProperty("metadata")
	public void setMetadata(final String metadata) {
		this.metadata = metadata;
	}

	@Override
	public String toString() {
		final var sb = new StringBuilder();
		sb.append(Err.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this)))
				.append('[');
		sb.append("transactionId");
		sb.append('=');
		sb.append(((this.transactionId == null) ? "<null>" : this.transactionId));
		sb.append(',');
		sb.append("errorCode");
		sb.append('=');
		sb.append(((this.errorCode == null) ? "<null>" : this.errorCode));
		sb.append(',');
		sb.append("metadata");
		sb.append('=');
		sb.append(((this.metadata == null) ? "<null>" : this.metadata));
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
		result = ((result * 31) + ((this.errorCode == null) ? 0 : this.errorCode.hashCode()));
		result = ((result * 31) + ((this.metadata == null) ? 0 : this.metadata.hashCode()));
		result = ((result * 31) + ((this.transactionId == null) ? 0 : this.transactionId.hashCode()));
		return result;
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if ((other instanceof Err) == false) {
			return false;
		}
		final var rhs = ((Err) other);
		return ((((this.errorCode == rhs.errorCode)
				|| ((this.errorCode != null) && this.errorCode.equals(rhs.errorCode)))
				&& ((this.metadata == rhs.metadata) || ((this.metadata != null) && this.metadata.equals(rhs.metadata))))
				&& ((this.transactionId == rhs.transactionId)
						|| ((this.transactionId != null) && this.transactionId.equals(rhs.transactionId))));
	}

}
