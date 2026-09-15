package ubic.gemma.rest.analytics.ga4;

import org.springframework.lang.Nullable;

/**
 * Strategy for retrieving a user ID.
 * @author poirigui
 */
public interface UserIdRetrievalStrategy {

    @Nullable
    String get();
}
