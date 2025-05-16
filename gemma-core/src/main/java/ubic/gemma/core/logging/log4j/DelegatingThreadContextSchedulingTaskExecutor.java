package ubic.gemma.core.logging.log4j;

import org.springframework.scheduling.SchedulingTaskExecutor;
import ubic.gemma.core.util.concurrent.DelegatingSchedulingTaskExecutor;

/**
 * @author poirigui
 */
public class DelegatingThreadContextSchedulingTaskExecutor extends DelegatingThreadContextAsyncTaskExecutor implements DelegatingSchedulingTaskExecutor {

    private final SchedulingTaskExecutor delegate;

    public DelegatingThreadContextSchedulingTaskExecutor( SchedulingTaskExecutor delegate ) {
        super( delegate );
        this.delegate = delegate;
    }

    @Override
    public SchedulingTaskExecutor getDelegate() {
        return delegate;
    }

    @Override
    public boolean prefersShortLivedTasks() {
        return delegate.prefersShortLivedTasks();
    }
}
