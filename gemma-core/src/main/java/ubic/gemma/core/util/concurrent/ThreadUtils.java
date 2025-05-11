package ubic.gemma.core.util.concurrent;

import ubic.gemma.core.logging.log4j.DelegatingThreadContextRunnable;

/**
 * Utilities for creating {@link Thread} objects.
 * @author poirigui
 */
public class ThreadUtils {

    public static Thread newThread( Runnable runnable ) {
        return new Thread( wrap( runnable ) );
    }

    public static Thread newThread( Runnable runnable, String name ) {
        return new Thread( wrap( runnable ), name );
    }

    private static Runnable wrap( Runnable runnable ) {
        return DelegatingThreadContextRunnable.create( runnable );
    }
}
