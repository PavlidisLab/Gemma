package ubic.gemma.core.search;

import ubic.gemma.model.common.Identifiable;

/**
 * Indexer service.
 * @author poirigui
 */
public interface IndexerService {

    /**
     * Index the given class.
     * @param classToIndex a set of classes to index
     */
    void index( Class<? extends Identifiable> classToIndex );

    /**
     * Set the number of threads to use for indexing entities.
     */
    void setNumThreads( int numThreads );

    /**
     * Set the logging frequency for reporting progress.
     */
    void setLoggingFrequency( int loggingFrequency );
}
