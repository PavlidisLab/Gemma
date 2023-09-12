package ubic.gemma.rest.analytics.ga4;

import org.springframework.validation.Errors;

import java.util.Date;
import java.util.Map;

/**
 * Raised when an invalid event is passed to {@link GoogleAnalytics4Provider#sendEvent(String, Date, Map)}.
 * <p>
 * This is package-private and only meant for test purposes. This exception is only raised when the debug mode is
 * enabled via {@link GoogleAnalytics4Provider#setDebug(boolean)} and should be treated as an {@link IllegalArgumentException}.
 * @author poirigui
 */
class InvalidEventException extends IllegalArgumentException {

    private final Errors errors;

    public InvalidEventException( Errors errors ) {
        super( errors.toString() );
        this.errors = errors;
    }

    public Errors getErrors() {
        return errors;
    }
}
