package ubic.gemma.core.metrics.binder;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.MeterBinder;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Micrometer binder for virtual-thread-per-task executors (JDK 21+).
 * <p>
 * Mirrors the {@link ThreadPoolExecutorMetrics} shape (pool-tagged counters and gauges) but works
 * for any {@link ExecutorService} — in particular {@code Executors.newVirtualThreadPerTaskExecutor()},
 * which is not a {@link java.util.concurrent.ThreadPoolExecutor} and therefore cannot be bound via
 * {@link GenericExecutorMetrics}.
 * <p>
 * Usage: construct with a pool name, call {@link #wrap(ExecutorService)} to obtain an instrumented
 * executor (delegating; submission and execution counters increment around {@link ExecutorService#execute}
 * and {@code submit*}), then register this binder with a {@link MeterRegistry} (typically by adding
 * it to the binder list in {@code MetricsConfig}).
 * <p>
 * Metrics emitted, all tagged {@code pool=<name>}:
 * <ul>
 *     <li>{@code gemma.executor.vt.submitted} — total tasks submitted via {@code execute} / {@code submit*}</li>
 *     <li>{@code gemma.executor.vt.active} — tasks currently executing</li>
 *     <li>{@code gemma.executor.vt.completed} — total tasks that finished (success or failure)</li>
 *     <li>{@code gemma.executor.vt.failed} — tasks that completed by throwing</li>
 *     <li>{@code gemma.executor.vt.queued} — tasks submitted but not yet started (typically near zero
 *         for VT-per-task since each submission spawns a thread immediately; published for symmetry
 *         with platform-pool dashboards)</li>
 *     <li>{@code gemma.executor.vt.task.duration} — distribution of task execution time</li>
 * </ul>
 *
 * @author claude
 */
@ParametersAreNonnullByDefault
public class VirtualThreadExecutorMetrics implements MeterBinder {

    private final String poolName;
    private final AtomicLong submitted = new AtomicLong();
    private final AtomicLong active = new AtomicLong();
    private final AtomicLong completed = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private final AtomicLong queued = new AtomicLong();

    /** Bound lazily in {@link #bindTo(MeterRegistry)}; may stay {@code null} until then. */
    private volatile Timer durationTimer;

    public VirtualThreadExecutorMetrics( String name ) {
        this.poolName = name;
    }

    @Override
    public void bindTo( MeterRegistry registry ) {
        Gauge.builder( "gemma.executor.vt.active", active, AtomicLong::get )
                .description( "Tasks currently executing on the virtual-thread executor" )
                .tags( "name", poolName )
                .register( registry );
        Gauge.builder( "gemma.executor.vt.queued", queued, AtomicLong::get )
                .description( "Tasks submitted but not yet started on the virtual-thread executor" )
                .tags( "name", poolName )
                .register( registry );
        FunctionCounter.builder( "gemma.executor.vt.submitted", submitted, AtomicLong::get )
                .description( "Total tasks submitted to the virtual-thread executor" )
                .tags( "name", poolName )
                .register( registry );
        FunctionCounter.builder( "gemma.executor.vt.completed", completed, AtomicLong::get )
                .description( "Total tasks that completed on the virtual-thread executor (success or failure)" )
                .tags( "name", poolName )
                .register( registry );
        FunctionCounter.builder( "gemma.executor.vt.failed", failed, AtomicLong::get )
                .description( "Tasks that completed with an exception on the virtual-thread executor" )
                .tags( "name", poolName )
                .register( registry );
        durationTimer = Timer.builder( "gemma.executor.vt.task.duration" )
                .description( "Execution time of tasks on the virtual-thread executor" )
                .tags( "name", poolName )
                .register( registry );
    }

    /**
     * Wrap a virtual-thread executor so that {@code execute} and {@code submit*} increment the
     * counters bound by {@link #bindTo(MeterRegistry)}. All other {@link ExecutorService} methods
     * (lifecycle, invokeAll/Any) pass through to the delegate; tasks scheduled through
     * {@code invokeAll}/{@code invokeAny} are instrumented at the {@code submit*} layer because
     * those methods are implemented in terms of submit on the JDK VT executor — to keep the
     * accounting unambiguous, this wrapper instruments at the boundary callers actually use
     * ({@code execute} and {@code submit*}).
     */
    public ExecutorService wrap( ExecutorService delegate ) {
        return new InstrumentedExecutorService( delegate );
    }

    private Runnable instrument( Runnable command ) {
        submitted.incrementAndGet();
        queued.incrementAndGet();
        return () -> {
            queued.decrementAndGet();
            active.incrementAndGet();
            long start = System.nanoTime();
            boolean threw = false;
            try {
                command.run();
            } catch ( RuntimeException | Error e ) {
                threw = true;
                throw e;
            } finally {
                long elapsed = System.nanoTime() - start;
                active.decrementAndGet();
                completed.incrementAndGet();
                if ( threw ) {
                    failed.incrementAndGet();
                }
                Timer t = durationTimer;
                if ( t != null ) {
                    t.record( elapsed, TimeUnit.NANOSECONDS );
                }
            }
        };
    }

    private <V> Callable<V> instrument( Callable<V> command ) {
        submitted.incrementAndGet();
        queued.incrementAndGet();
        return () -> {
            queued.decrementAndGet();
            active.incrementAndGet();
            long start = System.nanoTime();
            boolean threw = false;
            try {
                return command.call();
            } catch ( Exception | Error e ) {
                threw = true;
                throw e;
            } finally {
                long elapsed = System.nanoTime() - start;
                active.decrementAndGet();
                completed.incrementAndGet();
                if ( threw ) {
                    failed.incrementAndGet();
                }
                Timer t = durationTimer;
                if ( t != null ) {
                    t.record( elapsed, TimeUnit.NANOSECONDS );
                }
            }
        };
    }

    private class InstrumentedExecutorService implements ExecutorService {

        private final ExecutorService delegate;

        InstrumentedExecutorService( ExecutorService delegate ) {
            this.delegate = delegate;
        }

        @Override
        public void execute( Runnable command ) {
            delegate.execute( instrument( command ) );
        }

        @Override
        public Future<?> submit( Runnable task ) {
            return delegate.submit( instrument( task ) );
        }

        @Override
        public <T> Future<T> submit( Runnable task, T result ) {
            return delegate.submit( instrument( task ), result );
        }

        @Override
        public <T> Future<T> submit( Callable<T> task ) {
            return delegate.submit( instrument( task ) );
        }

        @Override
        public <T> List<Future<T>> invokeAll( Collection<? extends Callable<T>> tasks ) throws InterruptedException {
            return delegate.invokeAll( instrumentAll( tasks ) );
        }

        @Override
        public <T> List<Future<T>> invokeAll( Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit ) throws InterruptedException {
            return delegate.invokeAll( instrumentAll( tasks ), timeout, unit );
        }

        @Override
        public <T> T invokeAny( Collection<? extends Callable<T>> tasks ) throws InterruptedException, ExecutionException {
            return delegate.invokeAny( instrumentAll( tasks ) );
        }

        @Override
        public <T> T invokeAny( Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit ) throws InterruptedException, ExecutionException, TimeoutException {
            return delegate.invokeAny( instrumentAll( tasks ), timeout, unit );
        }

        private <T> List<Callable<T>> instrumentAll( Collection<? extends Callable<T>> tasks ) {
            List<Callable<T>> wrapped = new java.util.ArrayList<>( tasks.size() );
            for ( Callable<T> t : tasks ) {
                wrapped.add( instrument( t ) );
            }
            return wrapped;
        }

        @Override
        public void shutdown() {
            delegate.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return delegate.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return delegate.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return delegate.isTerminated();
        }

        @Override
        public boolean awaitTermination( long timeout, TimeUnit unit ) throws InterruptedException {
            return delegate.awaitTermination( timeout, unit );
        }
    }
}
