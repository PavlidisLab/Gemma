package ubic.gemma.core.security.concurrent;

import org.springframework.scheduling.SchedulingTaskExecutor;
import ubic.gemma.core.util.concurrent.DelegatingSchedulingTaskExecutor;

public class DelegatingSecurityContextSchedulingTaskExecutor extends org.springframework.security.scheduling.DelegatingSecurityContextSchedulingTaskExecutor implements DelegatingSchedulingTaskExecutor {

    private final SchedulingTaskExecutor delegate;

    public DelegatingSecurityContextSchedulingTaskExecutor( SchedulingTaskExecutor delegate ) {
        super( delegate );
        this.delegate = delegate;
    }

    @Override
    public SchedulingTaskExecutor getDelegate() {
        return delegate;
    }
}
