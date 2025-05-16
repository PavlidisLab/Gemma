package ubic.gemma.core.analysis.singleCell.aggregate;

/**
 * Exception raised when aggregation fails.
 * @author poirigui
 */
public class AggregationException extends RuntimeException {

    public AggregationException( String message ) {
        super( message );
    }
}
