package net.bbmsoft.worterbuch.client.pending;

import java.util.concurrent.CompletableFuture;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings({ "EI_EXPOSE_REP", "EI_EXPOSE_REP2" })
public record PendingReleaseLock(CompletableFuture<Boolean> callback) {

}
