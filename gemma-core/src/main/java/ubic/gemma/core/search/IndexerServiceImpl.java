package ubic.gemma.core.search;

import org.hibernate.SessionFactory;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.impl.SimpleIndexingProgressMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import ubic.gemma.model.common.Identifiable;

import java.util.Set;

@Service
public class IndexerServiceImpl implements IndexerService {

    @Autowired
    private SessionFactory sessionFactory;

    private int numThreads = 4;
    private int loggingFrequency = 10000;

    @Override
    public void index() {
        doIndex( new Class[0] );
    }

    @Override
    public void index( Set<Class<? extends Identifiable>> classesToIndex ) {
        if ( classesToIndex.isEmpty() ) {
            return;
        }
        doIndex( classesToIndex.toArray( new Class[0] ) );
    }

    @Override
    public void setNumThreads( int numThreads ) {
        Assert.isTrue( numThreads > 0, "The number of threads must be strictly positive." );
        this.numThreads = numThreads;
    }

    @Override
    public void setLoggingFrequency( int loggingFrequency ) {
        Assert.isTrue( loggingFrequency > 0, "The logging frequency must be strictly positive." );
        this.loggingFrequency = loggingFrequency;
    }

    private void doIndex( Class<?>[] classesToIndex ) {
        FullTextSession fullTextSession = Search.getFullTextSession( sessionFactory.openSession() );
        try {
            fullTextSession.createIndexer( classesToIndex )
                    .threadsToLoadObjects( numThreads )
                    .progressMonitor( new SimpleIndexingProgressMonitor( loggingFrequency ) )
                    .startAndWait();
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new RuntimeException( e );
        } finally {
            fullTextSession.close();
        }
    }
}
