package ubic.gemma.core.util.concurrent;

import java.util.concurrent.ExecutorService;

/**
 * An interface for {@link ExecutorService} that delegate to another {@link ExecutorService}.
 * @author poirigui
 */
public interface DelegatingExecutorService extends ExecutorService, DelegatingExecutor {

    @Override
    ExecutorService getDelegate();
}
