package ubic.gemma.core.util.concurrent;

import java.util.concurrent.Executor;

/**
 * An interface for {@link Executor} that delegate to another {@link Executor}.
 * @author poirigui
 */
public interface DelegatingExecutor extends Executor {

    /**
     * The executor this is delegating for.
     */
    Executor getDelegate();
}
