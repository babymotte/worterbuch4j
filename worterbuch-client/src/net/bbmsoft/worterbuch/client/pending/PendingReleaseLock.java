package net.bbmsoft.worterbuch.client.pending;

import java.util.concurrent.CompletableFuture;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bbmsoft.worterbuch.client.error.Result;
import net.bbmsoft.worterbuch.client.model.ClientMessage;

@SuppressFBWarnings({ "EI_EXPOSE_REP", "EI_EXPOSE_REP2" })
public record PendingReleaseLock(ClientMessage request, CompletableFuture<Result<Void>> callback) {

}
