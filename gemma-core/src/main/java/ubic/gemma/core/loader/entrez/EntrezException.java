package ubic.gemma.core.loader.entrez;

import java.util.List;

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
