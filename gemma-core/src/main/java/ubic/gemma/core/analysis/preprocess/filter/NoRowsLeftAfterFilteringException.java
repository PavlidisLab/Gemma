package ubic.gemma.core.analysis.preprocess.filter;

/**
 * This is a special kind of preprocessing exception that occurs when filtering the expression data matrix result in no
 * rows left.
 * @author poirigui
 */
public class NoRowsLeftAfterFilteringException extends FilteringException {

    public NoRowsLeftAfterFilteringException( String message ) {
        super( message );
    }
}
