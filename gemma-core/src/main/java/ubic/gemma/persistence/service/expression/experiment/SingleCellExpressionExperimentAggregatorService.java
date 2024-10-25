package ubic.gemma.persistence.service.expression.experiment;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.List;

/**
 * Aggregate single-cell vectors.
 */
public interface SingleCellExpressionExperimentAggregatorService {

    /**
     * Aggregate preferred single-cell data vectors by the preferred cell type assignment.
     * @param qt            single-cell quantitation type to aggregate
     * @param cellBAs       samples to aggregate vectors for
     * @param makePreferred make the resulting QT preferred
     * @return the quantitation type of the newly created vectors
     * @throws UnsupportedScaleTypeForAggregationException if data of the given scale type cannot be aggregated
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    QuantitationType aggregateVectors( ExpressionExperiment ee, QuantitationType qt, List<BioAssay> cellBAs, boolean makePreferred ) throws UnsupportedScaleTypeForAggregationException;
}
