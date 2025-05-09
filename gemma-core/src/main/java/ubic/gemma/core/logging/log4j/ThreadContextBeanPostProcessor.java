package ubic.gemma.core.logging.log4j;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

/**
 * Wraps Task executors in a {@link DelegatingThreadContextTaskExecutor} or {@link DelegatingThreadContextAsyncTaskExecutor}.
 *
 * @author poirigui
 */
@Component
public class ThreadContextBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization( Object bean, String beanName ) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization( Object bean, String beanName ) throws BeansException {
        if ( bean instanceof AsyncTaskExecutor ) {
            return new DelegatingThreadContextAsyncTaskExecutor( ( AsyncTaskExecutor ) bean );
        } else if ( bean instanceof TaskExecutor ) {
            return new DelegatingThreadContextTaskExecutor( ( TaskExecutor ) bean );
        } else {
            return bean;
        }
    }
}
