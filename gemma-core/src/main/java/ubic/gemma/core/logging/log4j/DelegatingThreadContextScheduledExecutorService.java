package ubic.gemma.core.logging.log4j;

import ubic.gemma.core.util.concurrent.AbstractDelegatingExecutorService;
import ubic.gemma.core.util.concurrent.DelegatingScheduledExecutorService;

import javax.annotation.Nonnull;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DelegatingThreadContextScheduledExecutorService extends AbstractDelegatingExecutorService implements DelegatingScheduledExecutorService {

    private final ScheduledExecutorService delegate;

    public DelegatingThreadContextScheduledExecutorService( ScheduledExecutorService delegate ) {
        super( delegate );
        this.delegate = delegate;
    }

    @Override
    public ScheduledExecutorService getDelegate() {
        return delegate;
    }

    @Nonnull
    @Override
    public ScheduledFuture<?> schedule( @Nonnull Runnable command, long delay, @Nonnull TimeUnit unit ) {
        return delegate.schedule( command, delay, unit );
    }

    @Nonnull
    @Override
    public <V> ScheduledFuture<V> schedule( @Nonnull Callable<V> callable, long delay, @Nonnull TimeUnit unit ) {
        return delegate.schedule( callable, delay, unit );
    }

    @Nonnull
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate( @Nonnull Runnable command, long initialDelay, long period, @Nonnull TimeUnit unit ) {
        return delegate.scheduleAtFixedRate( command, initialDelay, period, unit );
    }

    @Nonnull
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay( @Nonnull Runnable command, long initialDelay, long delay, @Nonnull TimeUnit unit ) {
        return delegate.scheduleWithFixedDelay( command, initialDelay, delay, unit );
    }

    @Override
    protected Runnable wrap( Runnable runnable ) {
        return DelegatingThreadContextRunnable.create( runnable );
    }

    @Override
    protected <T> Callable<T> wrap( Callable<T> callable ) {
        return DelegatingThreadContextCallable.create( callable );
    }
}
