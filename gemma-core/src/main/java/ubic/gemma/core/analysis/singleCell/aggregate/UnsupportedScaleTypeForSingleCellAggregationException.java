package ubic.gemma.core.analysis.singleCell.aggregate;

import ubic.gemma.model.common.quantitationtype.ScaleType;

/**
 * Exception raised when a {@link ScaleType} is not suitable for aggregating.
 *
 * @author poirigui
 */
public class UnsupportedScaleTypeForSingleCellAggregationException extends SingleCellAggregationException {

    public UnsupportedScaleTypeForSingleCellAggregationException( ScaleType scale ) {
        super( "Unsupported scale type for aggregation: " + scale );
    }
}
