package ubic.gemma.core.context;

import org.springframework.beans.factory.FactoryBean;

import java.util.concurrent.Future;

/**
 * Async extension of the {@link FactoryBean} interface.
 * <p>
 * <strong>Limitation:</strong><br>
 * Attempting to inject a {@code List<Future<MyService>>} will produce unexpected types in the collections if you have
 * defined other subclasses of {@link AsyncFactoryBean} for other bean types, there is unfortunately no direct
 * replacement for this, and you will have to filter the beans by type manually by calling {@link org.springframework.beans.factory.BeanFactory#getBean(Class)}
 * with the class of {@link Future}.
 * @author poirigui
 */
public interface AsyncFactoryBean<T> extends FactoryBean<Future<T>> {

    /**
     * Obtain a bean asynchronously.
     * @return a future that completes when the bean is ready
     */
    @Override
    Future<T> getObject();

    /**
     * Indicate if this bean has been initialized or if its initialization is pending.
     * <p>
     * This is only meaningful if {@link #isSingleton()} is true as otherwise a new, yet to be initialized bean will be
     * returned everytime and this method will always return false.
     */
    boolean isInitialized();
}
