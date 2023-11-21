package ubic.gemma.core.loader.expression.geo.service;

/**
 * Exception raised when an empty MINiML document is encountered.
 */
public class EmptyMinimlDocumentException extends RuntimeException {
    public EmptyMinimlDocumentException( Throwable cause ) {
        super( "The MINiML document was empty.", cause );
    }
}
