package ubic.gemma.core.security.concurrent;

import org.springframework.core.task.AsyncTaskExecutor;
import ubic.gemma.core.util.concurrent.DelegatingAsyncTaskExecutor;

public class DelegatingSecurityContextAsyncTaskExecutor extends org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor implements DelegatingAsyncTaskExecutor {

    private final AsyncTaskExecutor delegate;

    public DelegatingSecurityContextAsyncTaskExecutor( AsyncTaskExecutor delegate ) {
        super( delegate );
        this.delegate = delegate;
    }

    @Override
    public AsyncTaskExecutor getDelegate() {
        return delegate;
    }
}
