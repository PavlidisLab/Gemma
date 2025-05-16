package ubic.gemma.core.analysis.singleCell.aggregate;

import ubic.gemma.model.common.quantitationtype.ScaleType;

public class UnsupportedScaleTypeForAggregationException extends AggregationException {

    public UnsupportedScaleTypeForAggregationException( ScaleType scale ) {
        super( "Unsupported scale type for aggregation: " + scale );
    }
}
