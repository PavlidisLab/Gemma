package ubic.gemma.core.logging.log4j;

import ubic.gemma.core.util.concurrent.AbstractDelegatingExecutorService;
import ubic.gemma.core.util.concurrent.DelegatingScheduledExecutorService;

import org.springframework.lang.NonNull;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author poirigui
 */
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

    @NonNull
    @Override
    public ScheduledFuture<?> schedule( @NonNull Runnable command, long delay, @NonNull TimeUnit unit ) {
        return delegate.schedule( command, delay, unit );
    }

    @NonNull
    @Override
    public <V> ScheduledFuture<V> schedule( @NonNull Callable<V> callable, long delay, @NonNull TimeUnit unit ) {
        return delegate.schedule( callable, delay, unit );
    }

    @NonNull
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate( @NonNull Runnable command, long initialDelay, long period, @NonNull TimeUnit unit ) {
        return delegate.scheduleAtFixedRate( command, initialDelay, period, unit );
    }

    @NonNull
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay( @NonNull Runnable command, long initialDelay, long delay, @NonNull TimeUnit unit ) {
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
