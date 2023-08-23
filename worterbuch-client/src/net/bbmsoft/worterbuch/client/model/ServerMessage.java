
package net.bbmsoft.worterbuch.client.model;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "handshake", "state", "pState", "ack", "err", "lsState" })
@Generated("jsonschema2pojo")
public class ServerMessage<T> {

	/**
	 * A message sent by the server to indicate a successful handshake with the
	 * client
	 *
	 */
	@JsonProperty("handshake")
	@JsonPropertyDescription("A message sent by the server to indicate a successful handshake with the client")
	private Handshake handshake;
	/**
	 * A message sent by the server in response to a Get or Delete message
	 *
	 */
	@JsonProperty("state")
	@JsonPropertyDescription("A message sent by the server in response to a Get or Delete message")
	private State<T> state;
	/**
	 * A message sent by the server in response to a PGet, PDelete or
	 * Subscribe/PSubscribe message
	 *
	 */
	@JsonProperty("pState")
	@JsonPropertyDescription("A message sent by the server in response to a PGet, PDelete or Subscribe/PSubscribe message")
	private PState<T> pState;
	/**
	 * A message sent by the server to confirm that a client message has been
	 * processed
	 *
	 */
	@JsonProperty("ack")
	@JsonPropertyDescription("A message sent by the server to confirm that a client message has been processed")
	private Ack ack;
	/**
	 * A message sent by the server to indicate that there was an error processing a
	 * client message
	 *
	 */
	@JsonProperty("err")
	@JsonPropertyDescription("A message sent by the server to indicate that there was an error processing a client message")
	private Err err;
	/**
	 * A message sent by the server in response to an Ls message
	 *
	 */
	@JsonProperty("lsState")
	@JsonPropertyDescription("A message sent by the server in response to an Ls message")
	private LsState lsState;

	/**
	 * A message sent by the server to indicate a successful handshake with the
	 * client
	 *
	 */
	@JsonProperty("handshake")
	public Handshake getHandshake() {
		return this.handshake;
	}

	/**
	 * A message sent by the server to indicate a successful handshake with the
	 * client
	 *
	 */
	@JsonProperty("handshake")
	public void setHandshake(final Handshake handshake) {
		this.handshake = handshake;
	}

	/**
	 * A message sent by the server in response to a Get or Delete message
	 *
	 */
	@JsonProperty("state")
	public State<T> getState() {
		return this.state;
	}

	/**
	 * A message sent by the server in response to a Get or Delete message
	 *
	 */
	@JsonProperty("state")
	public void setState(final State<T> state) {
		this.state = state;
	}

	/**
	 * A message sent by the server in response to a PGet, PDelete or
	 * Subscribe/PSubscribe message
	 *
	 */
	@JsonProperty("pState")
	public PState<T> getpState() {
		return this.pState;
	}

	/**
	 * A message sent by the server in response to a PGet, PDelete or
	 * Subscribe/PSubscribe message
	 *
	 */
	@JsonProperty("pState")
	public void setpState(final PState<T> pState) {
		this.pState = pState;
	}

	/**
	 * A message sent by the server to confirm that a client message has been
	 * processed
	 *
	 */
	@JsonProperty("ack")
	public Ack getAck() {
		return this.ack;
	}

	/**
	 * A message sent by the server to confirm that a client message has been
	 * processed
	 *
	 */
	@JsonProperty("ack")
	public void setAck(final Ack ack) {
		this.ack = ack;
	}

	/**
	 * A message sent by the server to indicate that there was an error processing a
	 * client message
	 *
	 */
	@JsonProperty("err")
	public Err getErr() {
		return this.err;
	}

	/**
	 * A message sent by the server to indicate that there was an error processing a
	 * client message
	 *
	 */
	@JsonProperty("err")
	public void setErr(final Err err) {
		this.err = err;
	}

	/**
	 * A message sent by the server in response to an Ls message
	 *
	 */
	@JsonProperty("lsState")
	public LsState getLsState() {
		return this.lsState;
	}

	/**
	 * A message sent by the server in response to an Ls message
	 *
	 */
	@JsonProperty("lsState")
	public void setLsState(final LsState lsState) {
		this.lsState = lsState;
	}

	@Override
	public String toString() {
		final var sb = new StringBuilder();
		sb.append(ServerMessage.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this)))
				.append('[');
		sb.append("handshake");
		sb.append('=');
		sb.append(((this.handshake == null) ? "<null>" : this.handshake));
		sb.append(',');
		sb.append("state");
		sb.append('=');
		sb.append(((this.state == null) ? "<null>" : this.state));
		sb.append(',');
		sb.append("pState");
		sb.append('=');
		sb.append(((this.pState == null) ? "<null>" : this.pState));
		sb.append(',');
		sb.append("ack");
		sb.append('=');
		sb.append(((this.ack == null) ? "<null>" : this.ack));
		sb.append(',');
		sb.append("err");
		sb.append('=');
		sb.append(((this.err == null) ? "<null>" : this.err));
		sb.append(',');
		sb.append("lsState");
		sb.append('=');
		sb.append(((this.lsState == null) ? "<null>" : this.lsState));
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
		result = ((result * 31) + ((this.handshake == null) ? 0 : this.handshake.hashCode()));
		result = ((result * 31) + ((this.err == null) ? 0 : this.err.hashCode()));
		result = ((result * 31) + ((this.pState == null) ? 0 : this.pState.hashCode()));
		result = ((result * 31) + ((this.ack == null) ? 0 : this.ack.hashCode()));
		result = ((result * 31) + ((this.state == null) ? 0 : this.state.hashCode()));
		result = ((result * 31) + ((this.lsState == null) ? 0 : this.lsState.hashCode()));
		return result;
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if ((other instanceof ServerMessage) == false) {
			return false;
		}
		final ServerMessage<?> rhs = ((ServerMessage<?>) other);
		return (((((((this.handshake == rhs.handshake)
				|| ((this.handshake != null) && this.handshake.equals(rhs.handshake)))
				&& ((this.err == rhs.err) || ((this.err != null) && this.err.equals(rhs.err))))
				&& ((this.pState == rhs.pState) || ((this.pState != null) && this.pState.equals(rhs.pState))))
				&& ((this.ack == rhs.ack) || ((this.ack != null) && this.ack.equals(rhs.ack))))
				&& ((this.state == rhs.state) || ((this.state != null) && this.state.equals(rhs.state))))
				&& ((this.lsState == rhs.lsState) || ((this.lsState != null) && this.lsState.equals(rhs.lsState))));
	}

}
