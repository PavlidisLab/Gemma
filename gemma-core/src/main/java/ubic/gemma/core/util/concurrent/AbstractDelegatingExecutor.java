package ubic.gemma.core.util.concurrent;

import java.util.concurrent.Executor;

public abstract class AbstractDelegatingExecutor implements DelegatingExecutor {

    private final Executor delegate;

    public AbstractDelegatingExecutor( Executor delegate ) {
        this.delegate = delegate;
    }

    protected abstract Runnable wrap( Runnable runnable );

    @Override
    public void execute( Runnable runnable ) {
        delegate.execute( wrap( runnable ) );
    }
}
