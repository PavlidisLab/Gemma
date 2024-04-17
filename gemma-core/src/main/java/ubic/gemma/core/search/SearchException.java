package ubic.gemma.core.search;

/**
 * Exception raised by the {@link SearchService} when the search could not be performed.
 * @author poirigui
 */
public class SearchException extends Exception {

    public SearchException( String message, Throwable cause ) {
        super( message, cause );
    }

    public SearchException( Throwable cause ) {
        super( cause );
    }
}
