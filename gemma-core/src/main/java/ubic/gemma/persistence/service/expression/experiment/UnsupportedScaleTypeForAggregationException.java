package ubic.gemma.persistence.service.expression.experiment;

import ubic.gemma.model.common.quantitationtype.ScaleType;

public class UnsupportedScaleTypeForAggregationException extends UnsupportedOperationException {

    public UnsupportedScaleTypeForAggregationException( ScaleType scale ) {
        super( "Unsupported scale type for aggregation: " + scale );
    }
}
