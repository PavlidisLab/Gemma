package ubic.gemma.core.util;

import org.springframework.core.task.AsyncTaskExecutor;

public interface DelegatingAsyncTaskExecutor extends AsyncTaskExecutor, DelegatingTaskExecutor {

    /**
     * The task executor this is delegating for.
     */
    @Override
    AsyncTaskExecutor getDelegate();
}
