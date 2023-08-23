
package net.bbmsoft.worterbuch.client.model;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * A protocol version with a major and minor version tag
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "major", "minor" })
@Generated("jsonschema2pojo")
public class ProtocolVersion {

	/**
	 * The protocol's major version tag (Required)
	 *
	 */
	@JsonProperty("major")
	@JsonPropertyDescription("The protocol's major version tag")
	private Integer major;
	/**
	 * The protocol's minor version tag (Required)
	 *
	 */
	@JsonProperty("minor")
	@JsonPropertyDescription("The protocol's minor version tag")
	private Integer minor;

	/**
	 * The protocol's major version tag (Required)
	 *
	 */
	@JsonProperty("major")
	public Integer getMajor() {
		return this.major;
	}

	/**
	 * The protocol's major version tag (Required)
	 *
	 */
	@JsonProperty("major")
	public void setMajor(final Integer major) {
		this.major = major;
	}

	/**
	 * The protocol's minor version tag (Required)
	 *
	 */
	@JsonProperty("minor")
	public Integer getMinor() {
		return this.minor;
	}

	/**
	 * The protocol's minor version tag (Required)
	 *
	 */
	@JsonProperty("minor")
	public void setMinor(final Integer minor) {
		this.minor = minor;
	}

	@Override
	public String toString() {
		final var sb = new StringBuilder();
		sb.append(ProtocolVersion.class.getName()).append('@')
				.append(Integer.toHexString(System.identityHashCode(this))).append('[');
		sb.append("major");
		sb.append('=');
		sb.append(((this.major == null) ? "<null>" : this.major));
		sb.append(',');
		sb.append("minor");
		sb.append('=');
		sb.append(((this.minor == null) ? "<null>" : this.minor));
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
		result = ((result * 31) + ((this.major == null) ? 0 : this.major.hashCode()));
		result = ((result * 31) + ((this.minor == null) ? 0 : this.minor.hashCode()));
		return result;
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if ((other instanceof ProtocolVersion) == false) {
			return false;
		}
		final var rhs = ((ProtocolVersion) other);
		return (((this.major == rhs.major) || ((this.major != null) && this.major.equals(rhs.major)))
				&& ((this.minor == rhs.minor) || ((this.minor != null) && this.minor.equals(rhs.minor))));
	}

}
