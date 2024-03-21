package ubic.gemma.core.metrics.binder;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ThreadPoolTaskExecutorMetrics implements MeterBinder {

    private final ThreadPoolTaskExecutor executor;

    @Nullable
    private String poolName;

    public ThreadPoolTaskExecutorMetrics( ThreadPoolTaskExecutor executor ) {
        this.executor = executor;
    }

    @Override
    public void bindTo( MeterRegistry registry ) {
        String poolName = this.poolName != null ? this.poolName : executor.getThreadNamePrefix();
        Gauge.builder( "threadPool.corePoolSize", executor, ThreadPoolTaskExecutor::getCorePoolSize )
                .description( "Core pool size" )
                .tags( "pool", poolName )
                .register( registry );
        Gauge.builder( "threadPool.maxPoolSize", executor, e -> e.getMaxPoolSize() == Integer.MAX_VALUE ? Double.POSITIVE_INFINITY : e.getMaxPoolSize() )
                .description( "Maximum pool size" )
                .tags( "pool", poolName )
                .register( registry );
        Gauge.builder( "threadPool.poolSize", executor, ThreadPoolTaskExecutor::getPoolSize )
                .description( "Pool size" )
                .tags( "pool", poolName )
                .register( registry );
        Gauge.builder( "threadPool.activeCount", executor, ThreadPoolTaskExecutor::getActiveCount )
                .description( "Number of active threads" )
                .tags( "pool", poolName )
                .register( registry );
        Gauge.builder( "threadPool.queueSize", executor, e -> e.getThreadPoolExecutor().getQueue().size() )
                .description( "Queue size" )
                .tags( "pool", poolName )
                .register( registry );
    }

    public void setPoolName( String poolName ) {
        this.poolName = poolName;
    }
}
