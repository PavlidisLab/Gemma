package ubic.gemma.core.util.concurrent;

import org.springframework.security.concurrent.DelegatingSecurityContextScheduledExecutorService;
import ubic.gemma.core.logging.log4j.DelegatingThreadContextExecutorService;
import ubic.gemma.core.logging.log4j.DelegatingThreadContextScheduledExecutorService;
import ubic.gemma.core.security.concurrent.DelegatingSecurityContextExecutorService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * Extends {@link java.util.concurrent.Executors} to ensure that security context logging works as expected.
 * <p>
 * All executors produced by this class will be wrapped with the following:
 * <ul>
 * <li>{@link DelegatingSecurityContextExecutorService}</li>
 * <li>{@link DelegatingThreadContextExecutorService}</li>
 * </ul>
 *
 * @author poirigui
 */
public class Executors {

    public static ExecutorService newSingleThreadExecutor() {
        return wrap( java.util.concurrent.Executors.newSingleThreadExecutor() );
    }

    public static ExecutorService newSingleThreadExecutor( ThreadFactory threadFactory ) {
        return wrap( java.util.concurrent.Executors.newSingleThreadExecutor( threadFactory ) );
    }

    public static ExecutorService newFixedThreadPool( int numThreads ) {
        return wrap( java.util.concurrent.Executors.newFixedThreadPool( numThreads ) );
    }

    public static ExecutorService newFixedThreadPool( int numThreads, ThreadFactory threadFactory ) {
        return wrap( java.util.concurrent.Executors.newFixedThreadPool( numThreads, threadFactory ) );
    }

    public static ExecutorService newCachedThreadPool() {
        return wrap( java.util.concurrent.Executors.newCachedThreadPool() );
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor() {
        return wrap( java.util.concurrent.Executors.newSingleThreadScheduledExecutor() );
    }

    private static ExecutorService wrap( ExecutorService executorService ) {
        return new DelegatingSecurityContextExecutorService( new DelegatingThreadContextExecutorService( executorService ) );
    }

    private static ScheduledExecutorService wrap( ScheduledExecutorService scheduledExecutorService ) {
        return new DelegatingSecurityContextScheduledExecutorService( new DelegatingThreadContextScheduledExecutorService( scheduledExecutorService ) );
    }
}
