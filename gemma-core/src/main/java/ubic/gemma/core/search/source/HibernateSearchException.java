package ubic.gemma.core.search.source;

import ubic.gemma.core.search.SearchException;

public class HibernateSearchException extends SearchException {

    private final org.hibernate.search.SearchException cause;

    public HibernateSearchException( String message, org.hibernate.search.SearchException cause ) {
        super( message, cause );
        this.cause = cause;
    }

    @Override
    public synchronized org.hibernate.search.SearchException getCause() {
        return cause;
    }
}
