package ubic.gemma.core.context;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * Async extension of the {@link FactoryBean} interface.
 * <p>
 * <strong>Limitation:</strong><br>
 * All beans created by this factory expose the same {@link Future} object type, so a {@link Qualifier} must be used to
 * resolve a particular one.
 * <p>
 * Attempting to inject a {@code List<Future<MyService>>} will produce unexpected types in the collections if you have
 * defined other subclasses of {@link AsyncFactoryBean} for other bean types, there is unfortunately no direct
 * replacement for this, and you will have to filter the beans by type manually by calling {@link BeanFactory#getBean(Class)}
 * with the class of {@link Future}.
 * @author poirigui
 */
public interface AsyncFactoryBean<T> extends FactoryBean<Future<T>> {

    /**
     * Factory method to create an async singleton bean with a single-thread executor.
     */
    static <T> AsyncFactoryBean<T> singleton( Supplier<T> supplier ) {
        return new AbstractAsyncFactoryBean<T>() {

            @Override
            public boolean isSingleton() {
                return true;
            }

            @Override
            protected T createObject() {
                return supplier.get();
            }
        };
    }

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
