package ubic.gemma.core.loader.entrez;

import java.util.List;

/**
 * Exception raised when an Entrez reply contains an {@code ERROR} tag.
 * @author poirigui
 */
public class EntrezException extends RuntimeException {

    private final List<String> errors;

    public EntrezException( String message, List<String> errors ) {
        super( message );
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
