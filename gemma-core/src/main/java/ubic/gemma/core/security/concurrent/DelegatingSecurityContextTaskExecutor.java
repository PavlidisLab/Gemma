package ubic.gemma.core.security.concurrent;

import org.springframework.core.task.TaskExecutor;
import ubic.gemma.core.util.concurrent.DelegatingTaskExecutor;

public class DelegatingSecurityContextTaskExecutor extends org.springframework.security.task.DelegatingSecurityContextTaskExecutor implements DelegatingTaskExecutor {

    private final TaskExecutor delegate;

    public DelegatingSecurityContextTaskExecutor( TaskExecutor delegate ) {
        super( delegate );
        this.delegate = delegate;
    }

    @Override
    public TaskExecutor getDelegate() {
        return delegate;
    }
}
