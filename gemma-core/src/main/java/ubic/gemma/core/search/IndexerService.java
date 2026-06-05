package ubic.gemma.core.search;

import ubic.gemma.model.common.Identifiable;

/**
 * Indexer service.
 *
 * @author poirigui
 */
public interface IndexerService {

    /**
     * Index the given class.
     */
    void index( Class<? extends Identifiable> classToIndex );

    void index( Class<? extends Identifiable> classToIndex, int numThreads );
}
