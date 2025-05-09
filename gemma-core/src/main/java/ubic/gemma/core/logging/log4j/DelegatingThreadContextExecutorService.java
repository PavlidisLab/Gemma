package ubic.gemma.core.logging.log4j;

import ubic.gemma.core.util.AbstractDelegatingExecutorService;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * @author poirigui
 */
public class DelegatingThreadContextExecutorService extends AbstractDelegatingExecutorService {

    public DelegatingThreadContextExecutorService( ExecutorService delegate ) {
        super( delegate );
    }

    @Override
    protected Runnable wrap( Runnable runnable ) {
        return DelegatingThreadContextRunnable.create( runnable );
    }

    @Override
    protected <T> Callable<T> wrap( Callable<T> callable ) {
        return DelegatingThreadContextCallable.create( callable );
    }
}
