package ubic.gemma.core.search;

import org.hibernate.SessionFactory;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.impl.SimpleIndexingProgressMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.common.Identifiable;

import java.util.Set;

@Service
public class IndexerServiceImpl implements IndexerService {

    @Autowired
    private SessionFactory sessionFactory;

    @Override
    public void index( int numThreads ) {
        doIndex( new Class[0], numThreads );
    }

    @Override
    public void index( Set<Class<? extends Identifiable>> classesToIndex, int numThreads ) {
        if ( classesToIndex.isEmpty() ) {
            return;
        }
        doIndex( classesToIndex.toArray( new Class[0] ), numThreads );
    }

    private void doIndex( Class<?>[] classesToIndex, int numThreads ) {
        FullTextSession fullTextSession = Search.getFullTextSession( sessionFactory.openSession() );
        try {
            fullTextSession.createIndexer( classesToIndex )
                    .threadsToLoadObjects( numThreads )
                    .progressMonitor( new SimpleIndexingProgressMonitor( 10000 ) )
                    .startAndWait();
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new RuntimeException( e );
        } finally {
            fullTextSession.close();
        }
    }
}
