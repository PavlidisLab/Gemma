package ubic.gemma.core.loader.expression.geo.service;

import java.io.IOException;

public class MinimlDocumentTooLargeException extends IOException {
    public MinimlDocumentTooLargeException( String message ) {
        super( message );
    }
}
