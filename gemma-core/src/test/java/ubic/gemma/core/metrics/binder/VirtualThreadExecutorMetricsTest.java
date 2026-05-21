package ubic.gemma.core.metrics.binder;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link VirtualThreadExecutorMetrics} correctly increments its counters/gauges
 * when wrapped tasks are submitted, complete, or throw. Uses a cached thread pool as the
 * delegate (not the VT executor itself) so the test runs identically on JDK 17 and 21 — the
 * wrapper's instrumentation is independent of the underlying ExecutorService implementation.
 */
public class VirtualThreadExecutorMetricsTest {

    private SimpleMeterRegistry registry;
    private VirtualThreadExecutorMetrics metrics;
    private ExecutorService delegate;
    private ExecutorService wrapped;

    @BeforeEach
    public void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new VirtualThreadExecutorMetrics( "test" );
        metrics.bindTo( registry );
        delegate = Executors.newCachedThreadPool();
        wrapped = metrics.wrap( delegate );
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        wrapped.shutdown();
        wrapped.awaitTermination( 5, TimeUnit.SECONDS );
    }

    @Test
    public void submitRunnable_incrementsSubmittedAndCompleted() throws Exception {
        CountDownLatch latch = new CountDownLatch( 1 );
        Future<?> f = wrapped.submit( latch::countDown );
        f.get( 2, TimeUnit.SECONDS );
        assertThat( latch.await( 2, TimeUnit.SECONDS ) ).isTrue();

        assertThat( counter( "gemma.executor.vt.submitted" ) ).isEqualTo( 1.0 );
        assertThat( counter( "gemma.executor.vt.completed" ) ).isEqualTo( 1.0 );
        assertThat( counter( "gemma.executor.vt.failed" ) ).isEqualTo( 0.0 );
        Timer timer = registry.find( "gemma.executor.vt.task.duration" ).tag( "name", "test" ).timer();
        assertThat( timer ).isNotNull();
        assertThat( timer.count() ).isEqualTo( 1L );
    }

    @Test
    public void executeRunnableThatThrows_incrementsFailed() throws Exception {
        CountDownLatch done = new CountDownLatch( 1 );
        wrapped.execute( () -> {
            try {
                throw new IllegalStateException( "boom" );
            } finally {
                done.countDown();
            }
        } );
        assertThat( done.await( 2, TimeUnit.SECONDS ) ).isTrue();
        // give the finally block in instrument(...) a moment to run after the throw propagates
        Thread.sleep( 50 );

        assertThat( counter( "gemma.executor.vt.submitted" ) ).isEqualTo( 1.0 );
        assertThat( counter( "gemma.executor.vt.completed" ) ).isEqualTo( 1.0 );
        assertThat( counter( "gemma.executor.vt.failed" ) ).isEqualTo( 1.0 );
    }

    @Test
    public void submitCallableThatThrows_incrementsFailed() {
        Future<String> f = wrapped.submit( () -> {
            throw new IllegalStateException( "boom" );
        } );
        assertThat( f ).failsWithin( 2, TimeUnit.SECONDS )
                .withThrowableOfType( ExecutionException.class );

        assertThat( counter( "gemma.executor.vt.submitted" ) ).isEqualTo( 1.0 );
        assertThat( counter( "gemma.executor.vt.completed" ) ).isEqualTo( 1.0 );
        assertThat( counter( "gemma.executor.vt.failed" ) ).isEqualTo( 1.0 );
    }

    @Test
    public void activeGauge_tracksInFlightTasks() throws Exception {
        CountDownLatch started = new CountDownLatch( 1 );
        CountDownLatch release = new CountDownLatch( 1 );
        Future<?> f = wrapped.submit( () -> {
            started.countDown();
            try {
                release.await();
            } catch ( InterruptedException e ) {
                Thread.currentThread().interrupt();
            }
        } );
        assertThat( started.await( 2, TimeUnit.SECONDS ) ).isTrue();
        assertThat( gauge( "gemma.executor.vt.active" ) ).isEqualTo( 1.0 );

        release.countDown();
        f.get( 2, TimeUnit.SECONDS );
        // brief settle for the finally block
        Thread.sleep( 50 );
        assertThat( gauge( "gemma.executor.vt.active" ) ).isEqualTo( 0.0 );
    }

    @Test
    public void tagsArePresentOnAllMeters() {
        // sanity: every meter we documented carries the name tag
        for ( String meter : new String[] {
                "gemma.executor.vt.submitted",
                "gemma.executor.vt.completed",
                "gemma.executor.vt.failed",
                "gemma.executor.vt.active",
                "gemma.executor.vt.queued" } ) {
            assertThat( registry.find( meter ).tag( "name", "test" ).meter() )
                    .as( "meter %s tagged name=test", meter )
                    .isNotNull();
        }
        assertThat( registry.find( "gemma.executor.vt.task.duration" ).tag( "name", "test" ).timer() )
                .isNotNull();
    }

    private double counter( String name ) {
        FunctionCounter c = registry.find( name ).tag( "name", "test" ).functionCounter();
        assertThat( c ).as( "function counter %s tagged name=test", name ).isNotNull();
        return c.count();
    }

    private double gauge( String name ) {
        Gauge g = registry.find( name ).tag( "name", "test" ).gauge();
        assertThat( g ).as( "gauge %s tagged name=test", name ).isNotNull();
        return g.value();
    }
}
