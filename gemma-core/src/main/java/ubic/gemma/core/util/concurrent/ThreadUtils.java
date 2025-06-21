package ubic.gemma.core.util.concurrent;

import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import ubic.gemma.core.logging.log4j.DelegatingThreadContextRunnable;

/**
 * Utilities for creating {@link Thread} objects.
 * <p>
 * All threads created will see their runnable wrapped with:
 * <ul>
 * <li>{@link DelegatingSecurityContextRunnable}</li>
 * <li>{@link DelegatingThreadContextRunnable}</li>
 * </ul>
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
        return DelegatingSecurityContextRunnable.create( DelegatingThreadContextRunnable.create( runnable ), null );
    }
}
