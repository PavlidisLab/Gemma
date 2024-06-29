package ubic.gemma.core.metrics.binder;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import ubic.gemma.core.lang.NonNullApi;

import java.util.concurrent.ThreadPoolExecutor;

@NonNullApi
public class ThreadPoolExecutorMetrics implements MeterBinder {

    private final ThreadPoolExecutor executor;
    private final String poolName;

    public ThreadPoolExecutorMetrics( ThreadPoolExecutor executor, String poolName ) {
        this.executor = executor;
        this.poolName = poolName;
    }

    @Override
    public void bindTo( MeterRegistry registry ) {
        Gauge.builder( "threadPool.corePoolSize", executor, ThreadPoolExecutor::getCorePoolSize )
                .description( "Core pool size" )
                .tags( "pool", poolName )
                .register( registry );
        Gauge.builder( "threadPool.maxPoolSize", executor, e -> e.getMaximumPoolSize() == Integer.MAX_VALUE ? Double.POSITIVE_INFINITY : e.getMaximumPoolSize() )
                .description( "Maximum pool size" )
                .tags( "pool", poolName )
                .register( registry );
        Gauge.builder( "threadPool.poolSize", executor, ThreadPoolExecutor::getPoolSize )
                .description( "Pool size" )
                .tags( "pool", poolName )
                .register( registry );
        Gauge.builder( "threadPool.activeCount", executor, ThreadPoolExecutor::getActiveCount )
                .description( "Number of active threads" )
                .tags( "pool", poolName )
                .register( registry );
        Gauge.builder( "threadPool.queueSize", executor, e -> e.getQueue().size() )
                .description( "Queue size" )
                .tags( "pool", poolName )
                .register( registry );
    }
}
