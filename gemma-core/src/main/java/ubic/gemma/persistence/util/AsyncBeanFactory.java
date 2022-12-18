package ubic.gemma.persistence.util;

import java.util.concurrent.Future;

/**
 * Replacement for {@link org.springframework.beans.factory.BeanFactory} when you need to instantiate a bean
 * asynchronously.
 * <p>
 * The bean must have a pre-defined {@link AsyncFactoryBean}.
 *
 * @see AsyncFactoryBean
 * @author poirigui
 */
public interface AsyncBeanFactory {

    /**
     * Create a bean via its {@link AsyncFactoryBean}.
     *
     * @param clazz a class corresponding to the type of bean to initialize
     * @return a future for the bean creation
     * @param <T> the type of bean to initialize
     */
    <T> Future<T> getBeanAsync( Class<? extends AsyncFactoryBean<T>> clazz );
}
