package ubic.gemma.core.search;

import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.SessionFactory;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.common.Identifiable;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

@Service("indexerService")
@CommonsLog
public class IndexerServiceImpl implements IndexerService {

    private static final int LOGGING_FREQUENCY_MILLIS = 5000;
    private static final int NUM_THREADS = 4;

    @Autowired
    private SessionFactory sessionFactory;

    @Override
    public void index( Class<? extends Identifiable> classToIndex ) {
        index( classToIndex, NUM_THREADS );
    }

    @Override
    public void index( Class<? extends Identifiable> classToIndex, int numThreads ) {
        FullTextSession fullTextSession = Search.getFullTextSession( sessionFactory.openSession() );
        try {
            MassIndexerProgressMonitorImpl loggingProgressMonitor = new MassIndexerProgressMonitorImpl();
            Future<?> future = fullTextSession.createIndexer( classToIndex )
                    .threadsToLoadObjects( 4 )
                    .progressMonitor( loggingProgressMonitor )
                    .start();
            log.info( "Indexing of " + classToIndex.getName() + " has started..." );
            while ( !future.isDone() ) {
                try {
                    future.get( LOGGING_FREQUENCY_MILLIS, TimeUnit.MILLISECONDS );
                } catch ( ExecutionException e ) {
                    throw new RuntimeException( e );
                } catch ( TimeoutException e ) {
                    log.info( "Indexed " + loggingProgressMonitor.documentsAdded.get() + "/" + loggingProgressMonitor.totalCount.get() + " " + classToIndex.getName() + "." );
                }
            }
            log.info( "Done indexing " + loggingProgressMonitor.totalCount.get() + " " + classToIndex.getName() + "." );
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new RuntimeException( e );
        } finally {
            fullTextSession.close();
        }
    }

    private static class MassIndexerProgressMonitorImpl implements MassIndexerProgressMonitor {

        private final AtomicLong documentsBuilt = new AtomicLong();
        private final AtomicLong entitiesLoaded = new AtomicLong();
        private final AtomicLong documentsAdded = new AtomicLong();
        private final AtomicLong totalCount = new AtomicLong();

        @Override
        public void documentsBuilt( int i ) {
            documentsBuilt.addAndGet( i );
        }

        @Override
        public void entitiesLoaded( int i ) {
            entitiesLoaded.addAndGet( i );
        }

        @Override
        public void addToTotalCount( long l ) {
            totalCount.addAndGet( l );
        }

        @Override
        public void indexingCompleted() {
        }

        @Override
        public void documentsAdded( long l ) {
            documentsAdded.addAndGet( l );
        }
    }
}
