package ubic.gemma.core.security.concurrent;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.SchedulingTaskExecutor;

/**
 * Post-processor that wraps {@link TaskExecutor}, {@link AsyncTaskExecutor} and {@link SchedulingTaskExecutor} with a
 * {@link DelegatingSecurityContextTaskExecutor}.
 *
 * @author poirigui
 */
public class TaskExecutorSecurityContextDelegatePostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization( Object bean, String beanName ) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization( Object bean, String beanName ) throws BeansException {
        if ( bean instanceof SchedulingTaskExecutor ) {
            return new DelegatingSecurityContextSchedulingTaskExecutor( ( SchedulingTaskExecutor ) bean );
        } else if ( bean instanceof AsyncTaskExecutor ) {
            return new DelegatingSecurityContextAsyncTaskExecutor( ( AsyncTaskExecutor ) bean );
        } else if ( bean instanceof TaskExecutor ) {
            return new DelegatingSecurityContextTaskExecutor( ( TaskExecutor ) bean );
        } else {
            return bean;
        }
    }
}
