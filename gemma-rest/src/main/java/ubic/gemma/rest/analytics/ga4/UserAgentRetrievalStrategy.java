package ubic.gemma.rest.analytics.ga4;

import javax.annotation.Nullable;

/**
 * Strategy for retrieving a user agent.
 * @author poirigui
 */
public interface UserAgentRetrievalStrategy {

    @Nullable
    String get();
}
