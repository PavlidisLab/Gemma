package ubic.gemma.core.util.concurrent;

import org.springframework.core.task.AsyncTaskExecutor;

/**
 * @author poirigui
 */
public interface DelegatingAsyncTaskExecutor extends AsyncTaskExecutor, DelegatingTaskExecutor {

    /**
     * The task executor this is delegating for.
     */
    @Override
    AsyncTaskExecutor getDelegate();
}
