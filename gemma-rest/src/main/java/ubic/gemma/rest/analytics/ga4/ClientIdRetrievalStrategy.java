package ubic.gemma.rest.analytics.ga4;

import ubic.gemma.core.lang.Nullable;

/**
 * Strategy for retrieving a client ID.
 * <p>
 * The client ID uniquely identifies a user for analytics purposes.
 *
 * @author poirigui
 */
public interface ClientIdRetrievalStrategy {

    @Nullable
    String get();
}
