package ubic.gemma.core.metrics.binder;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import ubic.gemma.core.util.concurrent.DelegatingExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * A meter binder that delegates to the appropriate {@link Executor} implementation.
 * <p>
 * This handles {@link DelegatingExecutor} by recursively binding to the delegate until it finds a supported
 * implementation.
 * @author poirigui
 */
public class GenericExecutorMetrics implements MeterBinder {

    private final Executor executor;
    private final String poolName;

    public GenericExecutorMetrics( Executor executor, String poolName ) {
        this.executor = executor;
        this.poolName = poolName;
    }

    @Override
    public void bindTo( MeterRegistry registry ) {
        bindTo( executor, registry );
    }

    private void bindTo( Executor executor, MeterRegistry registry ) {
        if ( executor instanceof ThreadPoolExecutor ) {
            new ThreadPoolExecutorMetrics( ( ThreadPoolExecutor ) executor, poolName )
                    .bindTo( registry );
        } else if ( executor instanceof DelegatingExecutor ) {
            bindTo( ( ( DelegatingExecutor ) executor ).getDelegate(), registry );
        } else {
            throw new UnsupportedOperationException( "Unsupported executor: " + executor.getClass().getName() );
        }
    }
}
