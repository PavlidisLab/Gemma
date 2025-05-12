package ubic.gemma.core.util.concurrent;

import org.springframework.scheduling.SchedulingTaskExecutor;

/**
 * @author poirigui
 */
public interface DelegatingSchedulingTaskExecutor extends SchedulingTaskExecutor, DelegatingAsyncTaskExecutor {

    @Override
    SchedulingTaskExecutor getDelegate();
}
