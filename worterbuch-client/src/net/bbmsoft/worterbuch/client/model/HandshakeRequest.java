
package net.bbmsoft.worterbuch.client.model;

import java.util.List;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import net.bbmsoft.worterbuch.client.KeyValuePair;

/**
 * A message sent by a client to initiate a handshake with the server
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "supportedProtocolVersions", "lastWill", "graveGoods" })
@Generated("jsonschema2pojo")
public class HandshakeRequest {

	/**
	 * 
	 * (Required)
	 * 
	 */
	@JsonProperty("supportedProtocolVersions")
	private List<ProtocolVersion> supportedProtocolVersions;
	/**
	 * A list of key/value pairs that will be set when a client disconnects from the
	 * server (Required)
	 * 
	 */
	@JsonProperty("lastWill")
	@JsonPropertyDescription("A list of key/value pairs that will be set when a client disconnects from the server")
	private List<KeyValuePair<?>> lastWill;
	/**
	 * A list of keys that will be deleted when a client disconnects from the server
	 * (Required)
	 * 
	 */
	@JsonProperty("graveGoods")
	@JsonPropertyDescription("A list of keys that will be deleted when a client disconnects from the server")
	private List<String> graveGoods;

	/**
	 * 
	 * (Required)
	 * 
	 */
	@JsonProperty("supportedProtocolVersions")
	public List<ProtocolVersion> getSupportedProtocolVersions() {
		return this.supportedProtocolVersions;
	}

	/**
	 * 
	 * (Required)
	 * 
	 */
	@JsonProperty("supportedProtocolVersions")
	public void setSupportedProtocolVersions(final List<ProtocolVersion> supportedProtocolVersions) {
		this.supportedProtocolVersions = supportedProtocolVersions;
	}

	/**
	 * A list of key/value pairs that will be set when a client disconnects from the
	 * server (Required)
	 * 
	 */
	@JsonProperty("lastWill")
	public List<KeyValuePair<?>> getLastWill() {
		return this.lastWill;
	}

	/**
	 * A list of key/value pairs that will be set when a client disconnects from the
	 * server (Required)
	 * 
	 */
	@JsonProperty("lastWill")
	public void setLastWill(final List<KeyValuePair<?>> lastWill) {
		this.lastWill = lastWill;
	}

	/**
	 * A list of keys that will be deleted when a client disconnects from the server
	 * (Required)
	 * 
	 */
	@JsonProperty("graveGoods")
	public List<String> getGraveGoods() {
		return this.graveGoods;
	}

	/**
	 * A list of keys that will be deleted when a client disconnects from the server
	 * (Required)
	 * 
	 */
	@JsonProperty("graveGoods")
	public void setGraveGoods(final List<String> graveGoods) {
		this.graveGoods = graveGoods;
	}

	@Override
	public String toString() {
		final var sb = new StringBuilder();
		sb.append(HandshakeRequest.class.getName()).append('@')
				.append(Integer.toHexString(System.identityHashCode(this))).append('[');
		sb.append("supportedProtocolVersions");
		sb.append('=');
		sb.append(((this.supportedProtocolVersions == null) ? "<null>" : this.supportedProtocolVersions));
		sb.append(',');
		sb.append("lastWill");
		sb.append('=');
		sb.append(((this.lastWill == null) ? "<null>" : this.lastWill));
		sb.append(',');
		sb.append("graveGoods");
		sb.append('=');
		sb.append(((this.graveGoods == null) ? "<null>" : this.graveGoods));
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
		result = ((result * 31)
				+ ((this.supportedProtocolVersions == null) ? 0 : this.supportedProtocolVersions.hashCode()));
		result = ((result * 31) + ((this.lastWill == null) ? 0 : this.lastWill.hashCode()));
		result = ((result * 31) + ((this.graveGoods == null) ? 0 : this.graveGoods.hashCode()));
		return result;
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if ((other instanceof HandshakeRequest) == false) {
			return false;
		}
		final var rhs = ((HandshakeRequest) other);
		return ((((this.supportedProtocolVersions == rhs.supportedProtocolVersions)
				|| ((this.supportedProtocolVersions != null)
						&& this.supportedProtocolVersions.equals(rhs.supportedProtocolVersions)))
				&& ((this.lastWill == rhs.lastWill) || ((this.lastWill != null) && this.lastWill.equals(rhs.lastWill))))
				&& ((this.graveGoods == rhs.graveGoods)
						|| ((this.graveGoods != null) && this.graveGoods.equals(rhs.graveGoods))));
	}

}
