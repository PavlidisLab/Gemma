package ubic.gemma.core.metrics.binder;

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
        new ThreadPoolExecutorMetrics( executor.getThreadPoolExecutor(), poolName )
                .bindTo( registry );
    }

    public void setPoolName( String poolName ) {
        this.poolName = poolName;
    }
}
