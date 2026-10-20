package ubic.gemma.core.search.source;

import ubic.gemma.core.search.SearchException;

/**
 * Wraps a Hibernate Search runtime exception in a checked {@link SearchException}.
 *
 * <p>HS 7 throws {@link org.hibernate.search.util.common.SearchException} (moved
 * from {@code org.hibernate.search.exception.SearchException} that HS 5 used).</p>
 */
public class HibernateSearchException extends SearchException {

    private final org.hibernate.search.util.common.SearchException cause;

    public HibernateSearchException( String message, org.hibernate.search.util.common.SearchException cause ) {
        super( message, cause );
        this.cause = cause;
    }

    @Override
    public synchronized org.hibernate.search.util.common.SearchException getCause() {
        return cause;
    }
}
