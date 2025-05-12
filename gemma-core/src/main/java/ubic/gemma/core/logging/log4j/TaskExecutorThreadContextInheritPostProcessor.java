package ubic.gemma.core.logging.log4j;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.SchedulingTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Post-process {@link TaskExecutor}, {@link AsyncTaskExecutor} and {@link SchedulingTaskExecutor} such that they
 * inherit the {@link org.apache.logging.log4j.ThreadContext} of their callers.
 * @author poirigui
 * @see DelegatingThreadContextScheduledExecutorService
 * @see DelegatingThreadContextTaskExecutor
 */
public class TaskExecutorThreadContextInheritPostProcessor implements BeanPostProcessor {

    private final boolean includeJavaExecutors;

    /**
     * @param includeJavaExecutors if true, post-process {@link Executor} from the {@code java.util.concurrent} package.
     */
    public TaskExecutorThreadContextInheritPostProcessor( boolean includeJavaExecutors ) {
        this.includeJavaExecutors = includeJavaExecutors;
    }

    @Override
    public Object postProcessBeforeInitialization( Object bean, String beanName ) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization( Object bean, String beanName ) throws BeansException {
        if ( bean instanceof SchedulingTaskExecutor ) {
            return new DelegatingThreadContextSchedulingTaskExecutor( ( SchedulingTaskExecutor ) bean );
        } else if ( bean instanceof AsyncTaskExecutor ) {
            return new DelegatingThreadContextAsyncTaskExecutor( ( AsyncTaskExecutor ) bean );
        } else if ( bean instanceof TaskExecutor ) {
            return new DelegatingThreadContextTaskExecutor( ( TaskExecutor ) bean );
        }
        if ( includeJavaExecutors ) {
            if ( bean instanceof ScheduledExecutorService ) {
                return new DelegatingThreadContextScheduledExecutorService( ( ScheduledExecutorService ) bean );
            } else if ( bean instanceof ExecutorService ) {
                return new DelegatingThreadContextExecutorService( ( ExecutorService ) bean );
            } else if ( bean instanceof Executor ) {
                return new DelegatingThreadContextExecutor( ( Executor ) bean );
            }
        }
        return bean;
    }
}
