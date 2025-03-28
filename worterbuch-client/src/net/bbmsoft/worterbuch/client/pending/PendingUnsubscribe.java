package net.bbmsoft.worterbuch.client.pending;

import java.util.concurrent.CompletableFuture;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bbmsoft.worterbuch.client.model.ClientMessage;
import net.bbmsoft.worterbuch.client.response.Response;

@SuppressFBWarnings({ "EI_EXPOSE_REP", "EI_EXPOSE_REP2" })
public record PendingUnsubscribe(ClientMessage request, CompletableFuture<Response<Void>> callback) {

}
