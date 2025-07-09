package net.bbmsoft.worterbuch.client.error;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ConnectionFailed extends WorterbuchException {

	private static final long serialVersionUID = -2917821092985087116L;

	public final Map<URI, Throwable> causes;

	public ConnectionFailed(final String msg, final Map<URI, Throwable> causes) {
		super(msg);
		this.causes = Collections.unmodifiableMap(new LinkedHashMap<>(causes));
	}

	@Override
	public String toString() {
		final var sb = new StringBuilder();
		this.causes.forEach((u, th) -> sb.append(u).append(":\t").append(th.getMessage()).append("\n"));
		return sb.toString().trim();
	}

}
