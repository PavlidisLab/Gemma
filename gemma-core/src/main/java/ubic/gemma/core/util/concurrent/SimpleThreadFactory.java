package ubic.gemma.core.util.concurrent;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A simple thread factory based on a preferably unique thread name prefix.
 * @author poirigui
 */
@ParametersAreNonnullByDefault
public class SimpleThreadFactory implements ThreadFactory {

    private final String threadNamePrefix;
    private final AtomicInteger threadId = new AtomicInteger( 0 );

    public SimpleThreadFactory( String threadNamePrefix ) {
        this.threadNamePrefix = threadNamePrefix;
    }

    @Override
    public Thread newThread( Runnable runnable ) {
        return new Thread( runnable, threadNamePrefix + threadId.incrementAndGet() );
    }
}
