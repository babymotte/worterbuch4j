
package net.bbmsoft.worterbuch.client.model;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * A message sent by a client to subscribe to values of all keys matching the
 * provided pattern
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "transactionId", "requestPattern", "unique", "aggregateEvents" })
@Generated("jsonschema2pojo")
public class PSubscribe {

	/**
	 * A unique transaction ID (Required)
	 * 
	 */
	@JsonProperty("transactionId")
	@JsonPropertyDescription("A unique transaction ID")
	private Long transactionId;
	/**
	 * The pattern to subscribe to (Required)
	 * 
	 */
	@JsonProperty("requestPattern")
	@JsonPropertyDescription("The pattern to subscribe to")
	private String requestPattern;
	/**
	 * Indicate whether all or only unique values should be received (Required)
	 * 
	 */
	@JsonProperty("unique")
	@JsonPropertyDescription("Indicate whether all or only unique values should be received")
	private Boolean unique;
	/**
	 * Optionally aggregate events for the given number of milliseconds before
	 * sending them to the client to reduce network traffic
	 * 
	 */
	@JsonProperty("aggregateEvents")
	@JsonPropertyDescription("Optionally aggregate events for the given number of milliseconds before sending them to the client to reduce network traffic")
	private Long aggregateEvents;

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
	 * The pattern to subscribe to (Required)
	 * 
	 */
	@JsonProperty("requestPattern")
	public String getRequestPattern() {
		return this.requestPattern;
	}

	/**
	 * The pattern to subscribe to (Required)
	 * 
	 */
	@JsonProperty("requestPattern")
	public void setRequestPattern(final String requestPattern) {
		this.requestPattern = requestPattern;
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

	/**
	 * Optionally aggregate events for the given number of milliseconds before
	 * sending them to the client to reduce network traffic
	 * 
	 */
	@JsonProperty("aggregateEvents")
	public Long getAggregateEvents() {
		return this.aggregateEvents;
	}

	/**
	 * Optionally aggregate events for the given number of milliseconds before
	 * sending them to the client to reduce network traffic
	 * 
	 */
	@JsonProperty("aggregateEvents")
	public void setAggregateEvents(final Long aggregateEvents) {
		this.aggregateEvents = aggregateEvents;
	}

	@Override
	public String toString() {
		final var sb = new StringBuilder();
		sb.append(PSubscribe.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this)))
				.append('[');
		sb.append("transactionId");
		sb.append('=');
		sb.append(((this.transactionId == null) ? "<null>" : this.transactionId));
		sb.append(',');
		sb.append("requestPattern");
		sb.append('=');
		sb.append(((this.requestPattern == null) ? "<null>" : this.requestPattern));
		sb.append(',');
		sb.append("unique");
		sb.append('=');
		sb.append(((this.unique == null) ? "<null>" : this.unique));
		sb.append(',');
		sb.append("aggregateEvents");
		sb.append('=');
		sb.append(((this.aggregateEvents == null) ? "<null>" : this.aggregateEvents));
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
		result = ((result * 31) + ((this.aggregateEvents == null) ? 0 : this.aggregateEvents.hashCode()));
		result = ((result * 31) + ((this.transactionId == null) ? 0 : this.transactionId.hashCode()));
		result = ((result * 31) + ((this.requestPattern == null) ? 0 : this.requestPattern.hashCode()));
		result = ((result * 31) + ((this.unique == null) ? 0 : this.unique.hashCode()));
		return result;
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if ((other instanceof PSubscribe) == false) {
			return false;
		}
		final var rhs = ((PSubscribe) other);
		return (((((this.aggregateEvents == rhs.aggregateEvents)
				|| ((this.aggregateEvents != null) && this.aggregateEvents.equals(rhs.aggregateEvents)))
				&& ((this.transactionId == rhs.transactionId)
						|| ((this.transactionId != null) && this.transactionId.equals(rhs.transactionId))))
				&& ((this.requestPattern == rhs.requestPattern)
						|| ((this.requestPattern != null) && this.requestPattern.equals(rhs.requestPattern))))
				&& ((this.unique == rhs.unique) || ((this.unique != null) && this.unique.equals(rhs.unique))));
	}

}
