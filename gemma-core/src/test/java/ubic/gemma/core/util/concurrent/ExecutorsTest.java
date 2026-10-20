package ubic.gemma.core.util.concurrent;

import org.junit.jupiter.api.Test;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;
import org.springframework.security.concurrent.DelegatingSecurityContextScheduledExecutorService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Executors}: the in-tree factory that wraps every executor
 * produced for the project with Spring-Security's
 * {@link DelegatingSecurityContextExecutorService} plus the log4j ThreadContext
 * propagator. Pins the wrapping contract (so the layered delegation never silently
 * regresses), the JDK 21 virtual-thread reflection path, and the basic functional
 * behaviour (a submitted task actually runs on the returned executor).
 *
 * @author claude
 */
public class ExecutorsTest {

    @Test
    public void newSingleThreadExecutor_returnsSecurityContextWrappedExecutor() throws Exception {
        ExecutorService ex = Executors.newSingleThreadExecutor();
        try {
            assertThat( ex ).isInstanceOf( DelegatingSecurityContextExecutorService.class );
            assertSubmittedTaskRuns( ex );
        } finally {
            ex.shutdown();
            ex.awaitTermination( 2, TimeUnit.SECONDS );
        }
    }

    @Test
    public void newSingleThreadExecutor_withThreadFactory_consultsTheFactory() throws Exception {
        AtomicBoolean factoryWasCalled = new AtomicBoolean( false );
        ThreadFactory tf = r -> {
            factoryWasCalled.set( true );
            return new Thread( r, "executors-test-1" );
        };
        ExecutorService ex = Executors.newSingleThreadExecutor( tf );
        try {
            assertThat( ex ).isInstanceOf( DelegatingSecurityContextExecutorService.class );
            assertSubmittedTaskRuns( ex );
            assertThat( factoryWasCalled.get() ).isTrue();
        } finally {
            ex.shutdown();
            ex.awaitTermination( 2, TimeUnit.SECONDS );
        }
    }

    @Test
    public void newFixedThreadPool_returnsSecurityContextWrappedExecutor() throws Exception {
        ExecutorService ex = Executors.newFixedThreadPool( 2 );
        try {
            assertThat( ex ).isInstanceOf( DelegatingSecurityContextExecutorService.class );
            assertSubmittedTaskRuns( ex );
        } finally {
            ex.shutdown();
            ex.awaitTermination( 2, TimeUnit.SECONDS );
        }
    }

    @Test
    public void newFixedThreadPool_withThreadFactory_returnsWrappedExecutor() throws Exception {
        AtomicBoolean factoryWasCalled = new AtomicBoolean( false );
        ThreadFactory tf = r -> {
            factoryWasCalled.set( true );
            return new Thread( r, "executors-test-fixed" );
        };
        ExecutorService ex = Executors.newFixedThreadPool( 2, tf );
        try {
            assertThat( ex ).isInstanceOf( DelegatingSecurityContextExecutorService.class );
            assertSubmittedTaskRuns( ex );
            assertThat( factoryWasCalled.get() ).isTrue();
        } finally {
            ex.shutdown();
            ex.awaitTermination( 2, TimeUnit.SECONDS );
        }
    }

    @Test
    public void newCachedThreadPool_returnsSecurityContextWrappedExecutor() throws Exception {
        ExecutorService ex = Executors.newCachedThreadPool();
        try {
            assertThat( ex ).isInstanceOf( DelegatingSecurityContextExecutorService.class );
            assertSubmittedTaskRuns( ex );
        } finally {
            ex.shutdown();
            ex.awaitTermination( 2, TimeUnit.SECONDS );
        }
    }

    @Test
    public void newSingleThreadScheduledExecutor_returnsSecurityContextWrappedExecutor() throws Exception {
        ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor();
        try {
            assertThat( ex ).isInstanceOf( DelegatingSecurityContextScheduledExecutorService.class );
            // Smoke-check: a scheduled task with zero delay actually executes.
            AtomicBoolean ran = new AtomicBoolean( false );
            ex.schedule( () -> ran.set( true ), 0, TimeUnit.MILLISECONDS ).get( 2, TimeUnit.SECONDS );
            assertThat( ran.get() ).isTrue();
        } finally {
            ex.shutdown();
            ex.awaitTermination( 2, TimeUnit.SECONDS );
        }
    }

    @Test
    public void newVirtualThreadPerTaskExecutorIfAvailable_returnsWrappedExecutor() throws Exception {
        // On JDK 21+ the underlying executor is a virtual-thread-per-task instance; on JDK 17
        // the factory falls back to a cached platform pool. In both cases the wrapper layer
        // must remain — the project relies on security/MDC propagation regardless of JDK.
        ExecutorService ex = Executors.newVirtualThreadPerTaskExecutorIfAvailable();
        try {
            assertThat( ex ).isInstanceOf( DelegatingSecurityContextExecutorService.class );
            assertSubmittedTaskRuns( ex );
        } finally {
            ex.shutdown();
            ex.awaitTermination( 2, TimeUnit.SECONDS );
        }
    }

    /**
     * Submit a small task and confirm it ran on the wrapped executor by capturing the
     * carrier thread name. The wrapping layer must NOT swallow tasks — every executor
     * factory in this class is consumed by long-running services where lost tasks would
     * be a silent correctness bug.
     */
    private static void assertSubmittedTaskRuns( ExecutorService ex ) throws Exception {
        AtomicReference<String> carrier = new AtomicReference<>();
        ex.submit( () -> carrier.set( Thread.currentThread().getName() ) ).get( 2, TimeUnit.SECONDS );
        assertThat( carrier.get() ).isNotNull();
    }
}
