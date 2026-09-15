package ubic.gemma.core.logging.log4j;

import ubic.gemma.core.util.concurrent.DelegatingExecutor;

import org.springframework.lang.NonNull;
import java.util.concurrent.Executor;

/**
 * @author poirigui
 */
public class DelegatingThreadContextExecutor implements DelegatingExecutor {

    private final Executor delegate;

    public DelegatingThreadContextExecutor( Executor delegate ) {
        this.delegate = delegate;
    }

    @Override
    public Executor getDelegate() {
        return delegate;
    }

    @Override
    public void execute( @NonNull Runnable command ) {
        delegate.execute( DelegatingThreadContextRunnable.create( command ) );
    }
}
