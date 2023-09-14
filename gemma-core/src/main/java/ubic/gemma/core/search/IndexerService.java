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
     * @param numThreads number of threads to use for loading and indexing
     */
    void index( int numThreads );

    /**
     * Index all the given classes.
     * @param classesToIndex a set of classes to index
     * @param numThreads number of threads to use for loading and indexing
     */
    void index( Set<Class<? extends Identifiable>> classesToIndex, int numThreads );
}
