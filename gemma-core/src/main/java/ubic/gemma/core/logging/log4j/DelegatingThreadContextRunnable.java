package ubic.gemma.core.logging.log4j;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.ThreadContext;

import java.util.List;
import java.util.Map;

/**
 * @author poirigui
 */
public class DelegatingThreadContextRunnable implements Runnable {

    public static Runnable create( Runnable delegate ) {
        Map<String, String> mdc = ThreadContext.getImmutableContext();
        List<String> ndc = ThreadContext.getImmutableStack().asList();
        return new DelegatingThreadContextRunnable( delegate, mdc, ndc );
    }

    private final Runnable delegate;
    private final List<String> ndc;
    private final Map<String, String> mdc;

    private DelegatingThreadContextRunnable( Runnable runnable, Map<String, String> mdc, List<String> ndc ) {
        this.delegate = runnable;
        this.mdc = mdc;
        this.ndc = ndc;
    }

    @Override
    public void run() {
        try ( CloseableThreadContext.Instance ignored = CloseableThreadContext.putAll( mdc ).pushAll( ndc ) ) {
            delegate.run();
        }
    }
}
