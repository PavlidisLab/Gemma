package ubic.gemma.core.util.concurrent;

import ubic.gemma.core.logging.log4j.DelegatingThreadContextExecutorService;
import ubic.gemma.core.logging.log4j.DelegatingThreadContextScheduledExecutorService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * Wrapper around {@link java.util.concurrent.Executors} to ensure that logging works as expected.
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
        return new DelegatingThreadContextScheduledExecutorService( java.util.concurrent.Executors.newSingleThreadScheduledExecutor() );
    }

    private static ExecutorService wrap( ExecutorService executorService ) {
        return new DelegatingThreadContextExecutorService( executorService );
    }
}
