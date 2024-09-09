package ubic.gemma.persistence.service.expression.experiment;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.core.datastructure.matrix.SingleCellExpressionDataMatrix;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SingleCellExpressionExperimentService {

    List<QuantitationType> getSingleCellQuantitationTypes(ExpressionExperiment ee);

    /**
     * Obtain a single-cell expression data matrix for the given quantitation type.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    SingleCellExpressionDataMatrix<Double> getSingleCellExpressionDataMatrix( ExpressionExperiment expressionExperiment, QuantitationType quantitationType );

    /**
     * Add single-cell data vectors.
     * @return the number of vectors that were added
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    int addSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType,
            Collection<SingleCellExpressionDataVector> vectors );

    /**
     * Replace existing single-cell data vectors for the given quantitation type.
     * @return the number of vectors that were replaced
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    int replaceSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType,
            Collection<SingleCellExpressionDataVector> vectors );

    /**
     * Remove single-cell data vectors for the given quantitation type.
     * @return the number of vectors that were removed
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    int removeSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType );

    /**
     * Obtain all the single-cell dimensions used by a given dataset.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_READ" })
    List<SingleCellDimension> getSingleCellDimensions( ExpressionExperiment ee );

    /**
     * Relabel the cell types of an existing set of single-cell vectors.
     *
     * @param newCellTypeLabels the new cell types labels, must match the number of cells
     * @param labellingProtocol the protocol used to generate the new labelling, or null if unknown
     * @return a new, preferred cell type labelling
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_READ" })
    CellTypeAssignment relabelCellTypes( ExpressionExperiment ee, SingleCellDimension dimension, List<String> newCellTypeLabels, @Nullable Protocol labellingProtocol, @Nullable String description );

    /**
     * Remove the given cell type labelling.
     *
     * If the cell type labelling is preferred and applies the the preferred vectors as per {@link #getPreferredCellTypeAssignment(ExpressionExperiment)}, the cell type factor will be removed.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_READ" })
    void removeCellTypeLabels( ExpressionExperiment ee, SingleCellDimension scd, CellTypeAssignment cellTypeAssignment );

    /**
     * Obtain all the cell type labellings from all single-cell vectors.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_READ" })
    List<CellTypeAssignment> getCellTypeAssignments( ExpressionExperiment ee );

    /**
     * Obtain the preferred cell type labelling from the preferred single-cell vectors.
     */
    @Nullable
    @Secured({ "GROUP_USER", "ACL_SECURABLE_READ" })
    CellTypeAssignment getPreferredCellTypeAssignment( ExpressionExperiment ee );

    /**
     * Obtain the cell types of a given single-cell dataset.
     * <p>
     * Only the cell types applicable to the preferred single-cell vectors and labelling are returned.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_READ" })
    List<Characteristic> getCellTypes( ExpressionExperiment ee );

    /**
     * Recreate the cell type factor based on the preferred labelling of the preferred single-cell vectors.
     * <p>
     * Analyses involving the factor are removed and samples mentioning the factor values are updated as per
     * {@link ExperimentalFactorService#remove(ExperimentalFactor)}.
     *
     * @return the created cell type factor
     * @throws IllegalStateException if the dataset does not have a preferred cell type labelling for its preferred set
     *                               of single-cell vectors
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_READ" })
    ExperimentalFactor recreateCellTypeFactor( ExpressionExperiment ee );
}
