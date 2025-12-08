package ubic.gemma.core.analysis.preprocess.filter;

public class NoSamplesException extends InsufficientSamplesException {
    public NoSamplesException( String message ) {
        super( message );
    }
}
