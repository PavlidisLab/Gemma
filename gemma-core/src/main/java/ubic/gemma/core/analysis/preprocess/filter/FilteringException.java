package ubic.gemma.core.analysis.preprocess.filter;

/**
 * Exception raised when there is an issue during the filtering process of expression data.
 * @author poirigui
 */
public class FilteringException extends Exception {

    public FilteringException( String message ) {
        super( message );
    }
}
