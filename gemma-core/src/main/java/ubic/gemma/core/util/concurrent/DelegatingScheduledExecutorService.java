package ubic.gemma.core.util.concurrent;

import java.util.concurrent.ScheduledExecutorService;

/**
 * @author poirigui
 */
public interface DelegatingScheduledExecutorService extends DelegatingExecutorService, ScheduledExecutorService {

    @Override
    ScheduledExecutorService getDelegate();
}
