package ubic.gemma.persistence.util;

import org.springframework.beans.factory.FactoryBean;

import java.util.concurrent.Future;

/**
 * Async extension of the {@link FactoryBean} interface.
 * @author poirigui
 */
public interface AsyncFactoryBean<T> extends FactoryBean<T> {

    /**
     * Obtain a bean asynchronously.
     * @return a future that completes when the bean is ready
     */
    Future<T> getObjectAsync();

    /**
     * Indicate if this bean has been initialized or if its initialization is pending.
     * <p>
     * This is only meaningful if {@link #isSingleton()} is true as otherwise a new, yet to be initialized bean will be
     * returned everytime and this method will always return false.
     */
    boolean isInitialized();
}
