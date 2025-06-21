package ubic.gemma.core.logging.log4j;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.SchedulingTaskExecutor;

/**
 * Post-process {@link TaskExecutor}, {@link AsyncTaskExecutor} and {@link SchedulingTaskExecutor} such that they
 * inherit the {@link org.apache.logging.log4j.ThreadContext} of their callers.
 *
 * @author poirigui
 * @see DelegatingThreadContextScheduledExecutorService
 * @see DelegatingThreadContextTaskExecutor
 */
public class TaskExecutorThreadContextInheritPostProcessor implements BeanPostProcessor {

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
        } else {
            return bean;
        }
    }
}
