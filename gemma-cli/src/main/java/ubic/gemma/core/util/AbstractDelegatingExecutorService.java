package ubic.gemma.core.util;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * A delegating executor service inspired by {@link org.springframework.security.concurrent.DelegatingSecurityContextExecutorService}.
 * @author poirigui
 */
@ParametersAreNonnullByDefault
abstract class AbstractDelegatingExecutorService implements ExecutorService {

    protected abstract Runnable wrap( Runnable runnable );

    protected abstract <T> Callable<T> wrap( Callable<T> callable );

    private <T> Collection<? extends Callable<T>> wrap( Collection<? extends Callable<T>> callables ) {
        return callables.stream().map( this::wrap ).collect( Collectors.toList() );
    }

    private final ExecutorService delegate;

    protected AbstractDelegatingExecutorService( ExecutorService delegate ) {
        this.delegate = delegate;
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Nonnull
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
    public boolean awaitTermination( long l, TimeUnit timeUnit ) throws InterruptedException {
        return delegate.awaitTermination( l, timeUnit );
    }

    @Nonnull
    @Override
    public <T> Future<T> submit( Callable<T> callable ) {
        return delegate.submit( wrap( callable ) );
    }

    @Nonnull
    @Override
    public <T> Future<T> submit( Runnable runnable, T t ) {
        return delegate.submit( wrap( runnable ), t );
    }

    @Nonnull
    @Override
    public Future<?> submit( Runnable runnable ) {
        return delegate.submit( wrap( runnable ) );
    }

    @Nonnull
    @Override
    public <T> List<Future<T>> invokeAll( Collection<? extends Callable<T>> collection ) throws InterruptedException {
        return delegate.invokeAll( wrap( collection ) );
    }

    @Nonnull
    @Override
    public <T> List<Future<T>> invokeAll( Collection<? extends Callable<T>> collection, long l, TimeUnit timeUnit ) throws InterruptedException {
        return delegate.invokeAll( wrap( collection ), l, timeUnit );
    }

    @Nonnull
    @Override
    public <T> T invokeAny( Collection<? extends Callable<T>> collection ) throws InterruptedException, ExecutionException {
        return delegate.invokeAny( wrap( collection ) );
    }

    @Override
    public <T> T invokeAny( Collection<? extends Callable<T>> collection, long l, TimeUnit timeUnit ) throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny( wrap( collection ), l, timeUnit );
    }

    @Override
    public void execute( Runnable runnable ) {
        delegate.execute( wrap( runnable ) );
    }
}
