package ubic.gemma.rest.analytics.ga4;

import javax.annotation.Nullable;

public interface UserAgentRetrievalStrategy {

    @Nullable
    String get();
}
