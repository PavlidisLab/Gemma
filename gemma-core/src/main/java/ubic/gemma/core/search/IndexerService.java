package ubic.gemma.core.search;

import ubic.gemma.model.common.Identifiable;

import java.util.Set;

/**
 * Indexer service.
 * @author poirigui
 */
public interface IndexerService {

    /**
     * Index all the searchable entities.
     */
    void index();

    /**
     * Index the given classes.
     * @param classesToIndex a set of classes to index
     */
    void index( Set<Class<? extends Identifiable>> classesToIndex );

    /**
     * Set the number of threads to use for indexing entities.
     */
    void setNumThreads( int numThreads );

    /**
     * Set the logging frequency for reporting progress.
     */
    void setLoggingFrequency( int loggingFrequency );
}
