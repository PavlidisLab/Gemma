package ubic.gemma.rest.analytics.ga4;

import ubic.gemma.core.lang.Nullable;

import java.util.List;

/**
 * Composite strategy for retrieving client ID.
 * @author poirigui
 */
public class CompositeClientIdRetrievalStrategy implements ClientIdRetrievalStrategy {

    private final List<ClientIdRetrievalStrategy> strategies;

    public CompositeClientIdRetrievalStrategy( List<ClientIdRetrievalStrategy> strategies ) {
        this.strategies = strategies;
    }

    @Nullable
    @Override
    public String get() {
        String clientId = null;
        for ( ClientIdRetrievalStrategy strategy : strategies ) {
            clientId = strategy.get();
            if ( clientId != null ) {
                break;
            }
        }
        return clientId;
    }
}
