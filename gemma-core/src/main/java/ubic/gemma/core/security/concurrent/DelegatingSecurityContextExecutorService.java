package ubic.gemma.core.security.concurrent;

import ubic.gemma.core.util.concurrent.DelegatingExecutorService;

import java.util.concurrent.ExecutorService;

/**
 * @author poirigui
 */
public class DelegatingSecurityContextExecutorService extends org.springframework.security.concurrent.DelegatingSecurityContextExecutorService implements DelegatingExecutorService {

    private final ExecutorService delegate;

    public DelegatingSecurityContextExecutorService( ExecutorService delegate ) {
        super( delegate );
        this.delegate = delegate;
    }

    @Override
    public ExecutorService getDelegate() {
        return delegate;
    }
}
