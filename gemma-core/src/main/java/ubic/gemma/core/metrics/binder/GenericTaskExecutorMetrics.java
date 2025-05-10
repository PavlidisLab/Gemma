package ubic.gemma.core.metrics.binder;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import ubic.gemma.core.util.concurrent.DelegatingTaskExecutor;

import javax.annotation.Nullable;

/**
 * A meter binder that delegates to the appropriate {@link TaskExecutor} implementation.
 * <p>
 * This handles {@link DelegatingTaskExecutor} by recursively binding to the delegate until it finds a supported
 * implementation.
 * @author poirigui
 */
public class GenericTaskExecutorMetrics implements MeterBinder {

    private final TaskExecutor executor;

    @Nullable
    private String poolName;

    public GenericTaskExecutorMetrics( TaskExecutor executor ) {
        this.executor = executor;
    }

    public void setPoolName( @Nullable String poolName ) {
        this.poolName = poolName;
    }

    @Override
    public void bindTo( MeterRegistry registry ) {
        bindTo( executor, registry );
    }

    private void bindTo( TaskExecutor executor, MeterRegistry registry ) {
        if ( executor instanceof ThreadPoolTaskExecutor ) {
            ThreadPoolTaskExecutorMetrics metrics = new ThreadPoolTaskExecutorMetrics( ( ThreadPoolTaskExecutor ) executor );
            metrics.setPoolName( poolName );
            metrics.bindTo( registry );
        } else if ( executor instanceof DelegatingTaskExecutor ) {
            bindTo( ( ( DelegatingTaskExecutor ) executor ).getDelegate(), registry );
        } else {
            throw new UnsupportedOperationException( "Unsupported TaskExecutor: " + executor.getClass().getName() );
        }
    }
}
