package net.bbmsoft.worterbuch.client.impl;

import java.util.Optional;

import net.bbmsoft.worterbuch.client.model.AuthorizationRequest;
import net.bbmsoft.worterbuch.client.model.CGet;
import net.bbmsoft.worterbuch.client.model.CSet;
import net.bbmsoft.worterbuch.client.model.ClientMessage;
import net.bbmsoft.worterbuch.client.model.Delete;
import net.bbmsoft.worterbuch.client.model.Get;
import net.bbmsoft.worterbuch.client.model.Lock;
import net.bbmsoft.worterbuch.client.model.Ls;
import net.bbmsoft.worterbuch.client.model.PDelete;
import net.bbmsoft.worterbuch.client.model.PGet;
import net.bbmsoft.worterbuch.client.model.PLs;
import net.bbmsoft.worterbuch.client.model.PSubscribe;
import net.bbmsoft.worterbuch.client.model.ProtocolSwitchRequest;
import net.bbmsoft.worterbuch.client.model.Publish;
import net.bbmsoft.worterbuch.client.model.ReleaseLock;
import net.bbmsoft.worterbuch.client.model.SPub;
import net.bbmsoft.worterbuch.client.model.SPubInit;
import net.bbmsoft.worterbuch.client.model.Set;
import net.bbmsoft.worterbuch.client.model.Subscribe;
import net.bbmsoft.worterbuch.client.model.SubscribeLs;
import net.bbmsoft.worterbuch.client.model.Unsubscribe;
import net.bbmsoft.worterbuch.client.model.UnsubscribeLs;

public class MessageBuilder {

	public static <T> ClientMessage setMessage(final long tid, final String key, final T value) {
		final var msg = new ClientMessage();
		msg.setSet(new Set(tid, key, value));
		return msg;
	}

	public static <T> ClientMessage publishMessage(final long tid, final String key, final T value) {
		final var msg = new ClientMessage();
		msg.setPublish(new Publish(tid, key, value));
		return msg;
	}

	public static ClientMessage sPubInitMessage(final long tid, final String key) {
		final var msg = new ClientMessage();
		msg.setsPubInit(new SPubInit(tid, key));
		return msg;
	}

	public static <T> ClientMessage sPubMessage(final long transactionId, final T value) {
		final var msg = new ClientMessage();
		msg.setsPub(new SPub(transactionId, value));
		return msg;
	}

	public static ClientMessage getMessage(final long tid, final String key) {
		final var msg = new ClientMessage();
		msg.setGet(new Get(tid, key));
		return msg;
	}

	public static ClientMessage pGetMessage(final long tid, final String pattern) {
		final var msg = new ClientMessage();
		msg.setpGet(new PGet(tid, pattern));
		return msg;
	}

	public static ClientMessage deleteMessage(final long tid, final String key) {
		final var msg = new ClientMessage();
		msg.setDelete(new Delete(tid, key));
		return msg;
	}

	public static ClientMessage pDeleteMessage(final long tid, final String pattern, final boolean b) {
		final var msg = new ClientMessage();
		msg.setpDelete(new PDelete(tid, pattern, b));
		return msg;
	}

	public static ClientMessage lsMessage(final long tid, final String parent) {
		final var msg = new ClientMessage();
		msg.setLs(new Ls(tid, parent));
		return msg;
	}

	public static ClientMessage pLsMessage(final long tid, final String parentPattern) {
		final var msg = new ClientMessage();
		msg.setpLs(new PLs(tid, parentPattern));
		return msg;
	}

	public static ClientMessage subscribeMessage(final long tid, final String key, final boolean unique,
			final boolean liveOnly) {
		final var msg = new ClientMessage();
		msg.setSubscribe(new Subscribe(tid, key, unique, liveOnly));
		return msg;
	}

	public static ClientMessage pSubscribeMessage(final long tid, final String pattern, final boolean unique,
			final boolean liveOnly, final Optional<Long> aggregateEvents) {
		final var msg = new ClientMessage();
		msg.setpSubscribe(new PSubscribe(tid, pattern, unique, liveOnly, tid));
		return msg;
	}

	public static ClientMessage unsubscribeMessage(final long tid) {
		final var msg = new ClientMessage();
		msg.setUnsubscribe(new Unsubscribe(tid));
		return msg;
	}

	public static ClientMessage subscribeLsMessage(final long tid, final String parent) {
		final var msg = new ClientMessage();
		msg.setSubscribeLs(new SubscribeLs(tid, parent));
		return msg;
	}

	public static ClientMessage unsubscribeLsMessage(final long tid) {
		final var msg = new ClientMessage();
		msg.setUnsubscribeLs(new UnsubscribeLs(tid));
		return msg;
	}

	public static <T> ClientMessage cSetMessage(final long tid, final String key, final T value, final long version) {
		final var msg = new ClientMessage();
		msg.setcSet(new CSet(tid, key, value, version));
		return msg;
	}

	public static ClientMessage cGetMessage(final long tid, final String key) {
		final var msg = new ClientMessage();
		msg.setcGet(new CGet(tid, key));
		return msg;
	}

	public static ClientMessage lockMessage(final long tid, final String key) {
		final var msg = new ClientMessage();
		msg.setLock(new Lock(tid, key));
		return msg;
	}

	public static ClientMessage releaseLockMessage(final long tid, final String key) {
		final var msg = new ClientMessage();
		msg.setReleaseLock(new ReleaseLock(tid, key));
		return msg;
	}

	public static ClientMessage switchProtocolMessage(final int protoVersion) {
		final var msg = new ClientMessage();
		msg.setProtocolSwitchRequest(new ProtocolSwitchRequest(protoVersion));
		return msg;
	}

	public static ClientMessage authorizationMessage(final String authToken) {
		final var msg = new ClientMessage();
		msg.setAuthorizationRequest(new AuthorizationRequest(authToken));
		return msg;
	}

}
