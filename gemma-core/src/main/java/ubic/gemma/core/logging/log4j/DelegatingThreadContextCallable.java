package ubic.gemma.core.logging.log4j;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.ThreadContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @author poirigui
 */
public class DelegatingThreadContextCallable<T> implements Callable<T> {

    public static <T> Callable<T> create( Callable<T> delegate ) {
        Map<String, String> mdc = ThreadContext.getImmutableContext();
        List<String> ndc = ThreadContext.getImmutableStack().asList();
        return new DelegatingThreadContextCallable<>( delegate, mdc, ndc );
    }

    private final Callable<T> delegate;
    private final List<String> ndc;
    private final Map<String, String> mdc;

    private DelegatingThreadContextCallable( Callable<T> delegate, Map<String, String> mdc, List<String> ndc ) {
        this.delegate = delegate;
        this.mdc = mdc;
        this.ndc = ndc;
    }

    @Override
    public T call() throws Exception {
        try ( CloseableThreadContext.Instance ignored = CloseableThreadContext.putAll( mdc ).pushAll( ndc ) ) {
            return delegate.call();
        }
    }
}
