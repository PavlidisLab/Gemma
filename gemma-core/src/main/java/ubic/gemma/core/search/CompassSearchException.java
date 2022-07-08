package ubic.gemma.core.search;

import org.compass.core.CompassException;

/**
 * Search exception for Compass-specific exceptions.
 * @author poirigui
 */
public class CompassSearchException extends SearchException {

    private final CompassException cause;

    public CompassSearchException( String message, CompassException cause ) {
        super( message, cause );
        this.cause = cause;
    }

    @Override
    public CompassException getCause() {
        return cause;
    }
}
