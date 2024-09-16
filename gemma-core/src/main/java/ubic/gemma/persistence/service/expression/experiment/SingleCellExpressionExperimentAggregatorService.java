package ubic.gemma.persistence.service.expression.experiment;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.List;

/**
 * Aggregate single-cell vectors.
 */
public interface SingleCellExpressionExperimentAggregatorService {

    /**
     * @param cellBAs samples to aggregate vectors for
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    QuantitationType aggregateVectors( ExpressionExperiment ee, List<BioAssay> cellBAs );

    /**
     * Aggregate single-cell data vectors into raw expression data vectors.
     * @param cellTypeAssignment cell type assignment to use for creating aggregates
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    QuantitationType aggregateVectors( ExpressionExperiment ee, List<BioAssay> cellBAs, QuantitationType quantitationType, CellTypeAssignment cellTypeAssignment );
}
