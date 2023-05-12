package ubic.gemma.rest.analytics.ga4;

import javax.annotation.Nullable;

public interface UserIdRetrievalStrategy {

    @Nullable
    String get();
}
