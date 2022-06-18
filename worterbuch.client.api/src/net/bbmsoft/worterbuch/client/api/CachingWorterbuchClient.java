package net.bbmsoft.worterbuch.client.api;

import org.osgi.annotation.versioning.ProviderType;

/**
 * A client for a Wörterbuch server. Once connected it can be used to get, set
 * and subscribe to values. Values will automatically be cached and the cache
 * will be kept in sync with the server, so any subsequent get requests to the
 * same value will not cause any network traffic.
 *
 * @since 1.0
 */
@ProviderType
public interface CachingWorterbuchClient {

}
