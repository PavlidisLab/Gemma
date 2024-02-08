package ubic.gemma.persistence.service.expression.experiment;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Collection;
import java.util.List;

public interface SingleCellExpressionExperimentService {

    /**
     * Add single-cell data vectors.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void addSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType,
            Collection<SingleCellExpressionDataVector> vectors );

    /**
     * Replace existing single-cell data vectors for the given quantitation type.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void replaceSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType,
            Collection<SingleCellExpressionDataVector> vectors );

    /**
     * Remove single-cell data vectors for the given quantitation type.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void removeSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType );

    /**
     * Obtain all the single-cell dimensions used by a given dataset.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_READ" })
    List<SingleCellDimension> getSingleCellDimensions( ExpressionExperiment ee );

    /**
     * Relabel the cell types of an existing set of single-cell vectors.
     * @return a new dimension with the relabeled cell types, the original one is deleted
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_READ" })
    SingleCellDimension relabelCellTypes( ExpressionExperiment ee, SingleCellDimension dimension, List<String> newCellTypeLabels );
}
