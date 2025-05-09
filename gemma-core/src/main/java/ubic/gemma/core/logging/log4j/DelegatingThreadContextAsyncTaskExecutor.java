package ubic.gemma.core.logging.log4j;

import org.springframework.core.task.AsyncTaskExecutor;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * @author poirigui
 */
public class DelegatingThreadContextAsyncTaskExecutor implements AsyncTaskExecutor {

    private final AsyncTaskExecutor delegate;

    public DelegatingThreadContextAsyncTaskExecutor( AsyncTaskExecutor delegate ) {
        this.delegate = delegate;
    }

    @Override
    public void execute( Runnable task, long startTimeout ) {
        delegate.execute( DelegatingThreadContextRunnable.create( task ), startTimeout );
    }

    @Override
    public Future<?> submit( Runnable task ) {
        return delegate.submit( DelegatingThreadContextRunnable.create( task ) );
    }

    @Override
    public <T> Future<T> submit( Callable<T> task ) {
        return delegate.submit( DelegatingThreadContextCallable.create( task ) );
    }

    @Override
    public void execute( Runnable task ) {
        delegate.execute( DelegatingThreadContextRunnable.create( task ) );
    }
}
