package ubic.gemma.core.context;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Utilities for manipulating async factory beans.
 * @see AsyncFactoryBean
 */
public class AsyncFactoryBeanUtils {

    /**
     * Resolve a future obtained from {@link AsyncFactoryBean#getObject()} silently.
     * @throws RuntimeException wrapping any exception raised by the future
     */
    public static <T> T getSilently( Future<T> future, Class<? extends AsyncFactoryBean<T>> factoryBeanClass ) throws RuntimeException {
        try {
            return future.get();
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new RuntimeException( String.format( "Current thread was interrupted while obtaining async bean from %s.", factoryBeanClass.getName() ), e );
        } catch ( ExecutionException e ) {
            throw new RuntimeException( String.format( "Failed to obtain async bean from %s.", factoryBeanClass.getName() ), e.getCause() );
        }
    }
}
