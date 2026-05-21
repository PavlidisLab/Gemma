package ubic.gemma.core.util.concurrent;

import org.springframework.security.concurrent.DelegatingSecurityContextScheduledExecutorService;
import ubic.gemma.core.logging.log4j.DelegatingThreadContextExecutorService;
import ubic.gemma.core.logging.log4j.DelegatingThreadContextScheduledExecutorService;
import ubic.gemma.core.security.concurrent.DelegatingSecurityContextExecutorService;

import java.lang.reflect.Method;
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

    /**
     * Returns a virtual-thread-per-task executor when running on a JVM that supports it (JDK 21+),
     * or a cached thread pool fallback on older JVMs (JDK 17).
     * <p>
     * This is a forward-prep factory: callsites that are I/O-bound (network fetchers, FTP/HTTP
     * downloaders, REST clients, ontology updaters, parser readers) can opt in to virtual threads
     * one at a time without each callsite needing to know about the running JDK version. CPU-bound
     * workloads (ComBat, differential-expression analysis, math-heavy stats) should keep using
     * {@link #newFixedThreadPool(int)} or {@link #newCachedThreadPool()} — virtual threads provide
     * no throughput benefit there and add overhead.
     * <p>
     * The returned executor is wrapped with the same security context + log4j ThreadContext
     * delegation as every other factory in this class, so MDC and SecurityContext propagation
     * work identically whether the underlying carrier is virtual or platform.
     * <p>
     * Detection is via reflection on {@code java.util.concurrent.Executors#newVirtualThreadPerTaskExecutor},
     * which compiles cleanly on JDK 17 because the symbol is resolved at runtime, not compile time.
     * <p>
     * Active callsites (JDK 21 migration): TaskRunningServiceImpl background-tasks executor,
     * ServiceBeansConfig#expressionDataFileTaskExecutor, OntologyConfig#ontologyTaskExecutor,
     * plus the earlier I/O-bound sites already on this factory. Further migration should
     * continue per-callsite after auditing for {@code synchronized} blocks that hold the
     * carrier thread (a known virtual-thread pitfall; prefer
     * {@code java.util.concurrent.locks.ReentrantLock} in long-pinned regions).
     *
     * @return a {@link ExecutorService} backed by virtual threads on JDK 21+, otherwise by a cached
     *         platform thread pool; in both cases wrapped for security + thread context propagation.
     */
    public static ExecutorService newVirtualThreadPerTaskExecutorIfAvailable() {
        try {
            Method m = java.util.concurrent.Executors.class.getMethod( "newVirtualThreadPerTaskExecutor" );
            return wrap( ( ExecutorService ) m.invoke( null ) );
        } catch ( NoSuchMethodException e ) {
            // JDK 17 or older: virtual threads not available; fall back to a cached platform pool.
            return wrap( java.util.concurrent.Executors.newCachedThreadPool() );
        } catch ( ReflectiveOperationException e ) {
            throw new IllegalStateException( "failed to create virtual-thread-per-task executor", e );
        }
    }

    private static ExecutorService wrap( ExecutorService executorService ) {
        return new DelegatingSecurityContextExecutorService( new DelegatingThreadContextExecutorService( executorService ) );
    }

    private static ScheduledExecutorService wrap( ScheduledExecutorService scheduledExecutorService ) {
        return new DelegatingSecurityContextScheduledExecutorService( new DelegatingThreadContextScheduledExecutorService( scheduledExecutorService ) );
    }
}
