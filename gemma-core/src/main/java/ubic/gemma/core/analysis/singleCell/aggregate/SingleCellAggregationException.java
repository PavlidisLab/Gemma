package ubic.gemma.core.analysis.singleCell.aggregate;

/**
 * Exception raised when aggregation fails.
 * @author poirigui
 */
public class SingleCellAggregationException extends RuntimeException {

    public SingleCellAggregationException( String message ) {
        super( message );
    }
}
