package ubic.gemma.core.logging.log4j;

import org.springframework.core.task.TaskExecutor;
import ubic.gemma.core.util.concurrent.DelegatingTaskExecutor;

/**
 * @author poirigui
 */
public class DelegatingThreadContextTaskExecutor implements DelegatingTaskExecutor {

    private final TaskExecutor delegate;

    public DelegatingThreadContextTaskExecutor( TaskExecutor delegate ) {
        this.delegate = delegate;
    }

    @Override
    public TaskExecutor getDelegate() {
        return delegate;
    }

    @Override
    public void execute( Runnable task ) {
        delegate.execute( DelegatingThreadContextRunnable.create( task ) );
    }
}
