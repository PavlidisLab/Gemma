package ubic.gemma.core.logging.log4j;

import org.springframework.core.task.TaskExecutor;

/**
 * @author poirigui
 */
public class DelegatingThreadContextTaskExecutor implements TaskExecutor {

    private final TaskExecutor delegate;

    public DelegatingThreadContextTaskExecutor( TaskExecutor delegate ) {
        this.delegate = delegate;
    }

    @Override
    public void execute( Runnable task ) {
        delegate.execute( DelegatingThreadContextRunnable.create( task ) );
    }
}
