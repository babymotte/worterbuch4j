
package net.bbmsoft.worterbuch.client.model;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "handshakeRequest", "get", "pGet", "set", "publish", "subscribe", "pSubscribe", "unsubscribe",
		"delete", "pDelete", "ls", "subscribeLs" })
@Generated("jsonschema2pojo")
public class ClientMessage {

	/**
	 * A message sent by a client to initiate a handshake with the server
	 *
	 */
	@JsonProperty("handshakeRequest")
	@JsonPropertyDescription("A message sent by a client to initiate a handshake with the server")
	private HandshakeRequest handshakeRequest;
	/**
	 * A message sent by a client to request the value of the provided key from the
	 * server
	 *
	 */
	@JsonProperty("get")
	@JsonPropertyDescription("A message sent by a client to request the value of the provided key from the server")
	private Get get;
	/**
	 * A message sent by a client to request the values of all keys matching the
	 * provided pattern from the server
	 *
	 */
	@JsonProperty("pGet")
	@JsonPropertyDescription("A message sent by a client to request the values of all keys matching the provided pattern from the server")
	private PGet pGet;
	/**
	 * A message sent by a client to set a new value for a key
	 *
	 */
	@JsonProperty("set")
	@JsonPropertyDescription("A message sent by a client to set a new value for a key")
	private Set set;
	/**
	 * A message sent by a client to publish a new value for a key. The value will
	 * not be persisted on the server
	 *
	 */
	@JsonProperty("publish")
	@JsonPropertyDescription("A message sent by a client to publish a new value for a key. The value will not be persisted on the server")
	private Publish publish;
	/**
	 * A message sent by a client to subscribe to values of the provided key
	 *
	 */
	@JsonProperty("subscribe")
	@JsonPropertyDescription("A message sent by a client to subscribe to values of the provided key")
	private Subscribe subscribe;
	/**
	 * A message sent by a client to subscribe to values of all keys matching the
	 * provided pattern
	 *
	 */
	@JsonProperty("pSubscribe")
	@JsonPropertyDescription("A message sent by a client to subscribe to values of all keys matching the provided pattern")
	private PSubscribe pSubscribe;
	/**
	 * A message sent by a client to request the cancellation of the subscription
	 *
	 */
	@JsonProperty("unsubscribe")
	@JsonPropertyDescription("A message sent by a client to request the cancellation of the subscription")
	private Unsubscribe unsubscribe;
	/**
	 * A message sent by a client to request the deletion of the value of the
	 * provided key
	 *
	 */
	@JsonProperty("delete")
	@JsonPropertyDescription("A message sent by a client to request the deletion of the value of the provided key")
	private Delete delete;
	/**
	 * A message sent by a client to request the deletion of the values of all keys
	 * matching the provided pattern
	 *
	 */
	@JsonProperty("pDelete")
	@JsonPropertyDescription("A message sent by a client to request the deletion of the values of all keys matching the provided pattern")
	private PDelete pDelete;
	/**
	 * A message sent by a client to list all direct sub-key segments of the
	 * provided partial key
	 *
	 */
	@JsonProperty("ls")
	@JsonPropertyDescription("A message sent by a client to list all direct sub-key segments of the provided partial key")
	private Ls ls;
	/**
	 * A message sent by a client to request a subscription to all direct sub-key
	 * segments of the provided partial key
	 *
	 */
	@JsonProperty("subscribeLs")
	@JsonPropertyDescription("A message sent by a client to request a subscription to all direct sub-key segments of the provided partial key")
	private SubscribeLs subscribeLs;
	/**
	 * A message sent by a client to request the cancellation of an ls subscription
	 *
	 */
	@JsonProperty("unsubscribeLs")
	@JsonPropertyDescription("A message sent by a client to request the cancellation of an ls subscription")
	private UnsubscribeLs unsubscribeLs;

	/**
	 * A message sent by a client to initiate a handshake with the server
	 *
	 */
	@JsonProperty("handshakeRequest")
	public HandshakeRequest getHandshakeRequest() {
		return this.handshakeRequest;
	}

	/**
	 * A message sent by a client to initiate a handshake with the server
	 *
	 */
	@JsonProperty("handshakeRequest")
	public void setHandshakeRequest(final HandshakeRequest handshakeRequest) {
		this.handshakeRequest = handshakeRequest;
	}

	/**
	 * A message sent by a client to request the value of the provided key from the
	 * server
	 *
	 */
	@JsonProperty("get")
	public Get getGet() {
		return this.get;
	}

	/**
	 * A message sent by a client to request the value of the provided key from the
	 * server
	 *
	 */
	@JsonProperty("get")
	public void setGet(final Get get) {
		this.get = get;
	}

	/**
	 * A message sent by a client to request the values of all keys matching the
	 * provided pattern from the server
	 *
	 */
	@JsonProperty("pGet")
	public PGet getpGet() {
		return this.pGet;
	}

	/**
	 * A message sent by a client to request the values of all keys matching the
	 * provided pattern from the server
	 *
	 */
	@JsonProperty("pGet")
	public void setpGet(final PGet pGet) {
		this.pGet = pGet;
	}

	/**
	 * A message sent by a client to set a new value for a key
	 *
	 */
	@JsonProperty("set")
	public Set getSet() {
		return this.set;
	}

	/**
	 * A message sent by a client to set a new value for a key
	 *
	 */
	@JsonProperty("set")
	public void setSet(final Set set) {
		this.set = set;
	}

	/**
	 * A message sent by a client to publish a new value for a key. The value will
	 * not be persisted on the server
	 *
	 */
	@JsonProperty("publish")
	public Publish getPublish() {
		return this.publish;
	}

	/**
	 * A message sent by a client to publish a new value for a key. The value will
	 * not be persisted on the server
	 *
	 */
	@JsonProperty("publish")
	public void setPublish(final Publish publish) {
		this.publish = publish;
	}

	/**
	 * A message sent by a client to subscribe to values of the provided key
	 *
	 */
	@JsonProperty("subscribe")
	public Subscribe getSubscribe() {
		return this.subscribe;
	}

	/**
	 * A message sent by a client to subscribe to values of the provided key
	 *
	 */
	@JsonProperty("subscribe")
	public void setSubscribe(final Subscribe subscribe) {
		this.subscribe = subscribe;
	}

	/**
	 * A message sent by a client to subscribe to values of all keys matching the
	 * provided pattern
	 *
	 */
	@JsonProperty("pSubscribe")
	public PSubscribe getpSubscribe() {
		return this.pSubscribe;
	}

	/**
	 * A message sent by a client to subscribe to values of all keys matching the
	 * provided pattern
	 *
	 */
	@JsonProperty("pSubscribe")
	public void setpSubscribe(final PSubscribe pSubscribe) {
		this.pSubscribe = pSubscribe;
	}

	/**
	 * A message sent by a client to request the cancellation of the subscription
	 *
	 */
	@JsonProperty("unsubscribe")
	public Unsubscribe getUnsubscribe() {
		return this.unsubscribe;
	}

	/**
	 * A message sent by a client to request the cancellation of the subscription
	 *
	 */
	@JsonProperty("unsubscribe")
	public void setUnsubscribe(final Unsubscribe unsubscribe) {
		this.unsubscribe = unsubscribe;
	}

	/**
	 * A message sent by a client to request the deletion of the value of the
	 * provided key
	 *
	 */
	@JsonProperty("delete")
	public Delete getDelete() {
		return this.delete;
	}

	/**
	 * A message sent by a client to request the deletion of the value of the
	 * provided key
	 *
	 */
	@JsonProperty("delete")
	public void setDelete(final Delete delete) {
		this.delete = delete;
	}

	/**
	 * A message sent by a client to request the deletion of the values of all keys
	 * matching the provided pattern
	 *
	 */
	@JsonProperty("pDelete")
	public PDelete getpDelete() {
		return this.pDelete;
	}

	/**
	 * A message sent by a client to request the deletion of the values of all keys
	 * matching the provided pattern
	 *
	 */
	@JsonProperty("pDelete")
	public void setpDelete(final PDelete pDelete) {
		this.pDelete = pDelete;
	}

	/**
	 * A message sent by a client to list all direct sub-key segments of the
	 * provided partial key
	 *
	 */
	@JsonProperty("ls")
	public Ls getLs() {
		return this.ls;
	}

	/**
	 * A message sent by a client to list all direct sub-key segments of the
	 * provided partial key
	 *
	 */
	@JsonProperty("ls")
	public void setLs(final Ls ls) {
		this.ls = ls;
	}

	/**
	 * A message sent by a client to request a subscription to all direct sub-key
	 * segments of the provided partial key
	 *
	 */
	@JsonProperty("subscribeLs")
	public SubscribeLs getSubscribeLs() {
		return this.subscribeLs;
	}

	/**
	 * A message sent by a client to request a subscription to all direct sub-key
	 * segments of the provided partial key
	 *
	 */
	@JsonProperty("subscribeLs")
	public void setSubscribeLs(final SubscribeLs subscribeLs) {
		this.subscribeLs = subscribeLs;
	}

	/**
	 * A message sent by a client to request the cancellation of the subscription
	 *
	 */
	@JsonProperty("unsubscribeLs")
	public UnsubscribeLs getUnsubscribeLs() {
		return this.unsubscribeLs;
	}

	/**
	 * A message sent by a client to request the cancellation of the subscription
	 *
	 */
	@JsonProperty("unsubscribeLs")
	public void setUnsubscribeLs(final UnsubscribeLs unsubscribeLs) {
		this.unsubscribeLs = unsubscribeLs;
	}

	@Override
	public String toString() {
		final var sb = new StringBuilder();
		sb.append(ClientMessage.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this)))
				.append('[');
		sb.append("handshakeRequest");
		sb.append('=');
		sb.append(((this.handshakeRequest == null) ? "<null>" : this.handshakeRequest));
		sb.append(',');
		sb.append("get");
		sb.append('=');
		sb.append(((this.get == null) ? "<null>" : this.get));
		sb.append(',');
		sb.append("pGet");
		sb.append('=');
		sb.append(((this.pGet == null) ? "<null>" : this.pGet));
		sb.append(',');
		sb.append("set");
		sb.append('=');
		sb.append(((this.set == null) ? "<null>" : this.set));
		sb.append(',');
		sb.append("publish");
		sb.append('=');
		sb.append(((this.publish == null) ? "<null>" : this.publish));
		sb.append(',');
		sb.append("subscribe");
		sb.append('=');
		sb.append(((this.subscribe == null) ? "<null>" : this.subscribe));
		sb.append(',');
		sb.append("pSubscribe");
		sb.append('=');
		sb.append(((this.pSubscribe == null) ? "<null>" : this.pSubscribe));
		sb.append(',');
		sb.append("unsubscribe");
		sb.append('=');
		sb.append(((this.unsubscribe == null) ? "<null>" : this.unsubscribe));
		sb.append(',');
		sb.append("delete");
		sb.append('=');
		sb.append(((this.delete == null) ? "<null>" : this.delete));
		sb.append(',');
		sb.append("pDelete");
		sb.append('=');
		sb.append(((this.pDelete == null) ? "<null>" : this.pDelete));
		sb.append(',');
		sb.append("ls");
		sb.append('=');
		sb.append(((this.ls == null) ? "<null>" : this.ls));
		sb.append(',');
		sb.append("subscribeLs");
		sb.append('=');
		sb.append(((this.subscribeLs == null) ? "<null>" : this.subscribeLs));
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
		result = ((result * 31) + ((this.set == null) ? 0 : this.set.hashCode()));
		result = ((result * 31) + ((this.subscribe == null) ? 0 : this.subscribe.hashCode()));
		result = ((result * 31) + ((this.ls == null) ? 0 : this.ls.hashCode()));
		result = ((result * 31) + ((this.delete == null) ? 0 : this.delete.hashCode()));
		result = ((result * 31) + ((this.pGet == null) ? 0 : this.pGet.hashCode()));
		result = ((result * 31) + ((this.unsubscribe == null) ? 0 : this.unsubscribe.hashCode()));
		result = ((result * 31) + ((this.get == null) ? 0 : this.get.hashCode()));
		result = ((result * 31) + ((this.publish == null) ? 0 : this.publish.hashCode()));
		result = ((result * 31) + ((this.pSubscribe == null) ? 0 : this.pSubscribe.hashCode()));
		result = ((result * 31) + ((this.handshakeRequest == null) ? 0 : this.handshakeRequest.hashCode()));
		result = ((result * 31) + ((this.subscribeLs == null) ? 0 : this.subscribeLs.hashCode()));
		result = ((result * 31) + ((this.pDelete == null) ? 0 : this.pDelete.hashCode()));
		return result;
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if ((other instanceof ClientMessage) == false) {
			return false;
		}
		final var rhs = ((ClientMessage) other);
		return (((((((((((((this.set == rhs.set) || ((this.set != null) && this.set.equals(rhs.set)))
				&& ((this.subscribe == rhs.subscribe)
						|| ((this.subscribe != null) && this.subscribe.equals(rhs.subscribe))))
				&& ((this.ls == rhs.ls) || ((this.ls != null) && this.ls.equals(rhs.ls))))
				&& ((this.delete == rhs.delete) || ((this.delete != null) && this.delete.equals(rhs.delete))))
				&& ((this.pGet == rhs.pGet) || ((this.pGet != null) && this.pGet.equals(rhs.pGet))))
				&& ((this.unsubscribe == rhs.unsubscribe)
						|| ((this.unsubscribe != null) && this.unsubscribe.equals(rhs.unsubscribe))))
				&& ((this.get == rhs.get) || ((this.get != null) && this.get.equals(rhs.get))))
				&& ((this.publish == rhs.publish) || ((this.publish != null) && this.publish.equals(rhs.publish))))
				&& ((this.pSubscribe == rhs.pSubscribe)
						|| ((this.pSubscribe != null) && this.pSubscribe.equals(rhs.pSubscribe))))
				&& ((this.handshakeRequest == rhs.handshakeRequest)
						|| ((this.handshakeRequest != null) && this.handshakeRequest.equals(rhs.handshakeRequest))))
				&& ((this.subscribeLs == rhs.subscribeLs)
						|| ((this.subscribeLs != null) && this.subscribeLs.equals(rhs.subscribeLs))))
				&& ((this.pDelete == rhs.pDelete) || ((this.pDelete != null) && this.pDelete.equals(rhs.pDelete))));
	}

}
