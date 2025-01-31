package ubic.gemma.core.analysis.singleCell.aggregate;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;

import java.util.List;

/**
 * Split a single-cell expression experiment into sub-experiments by cell type.
 * @author poirigui
 */
public interface SingleCellExpressionExperimentSplitService {

    /**
     * Split biomaterials and bioassays by the given cell type assignment.
     * <p>
     * This method will:
     * <ul>
     * <li>create sub-{@link BioMaterial}s for each subject and cell type</li>
     * <li>create corresponding {@link BioAssay}s for the sub-{@link BioMaterial}s</li>
     * <li>attach the {@link BioAssay}s to {@link ExpressionExperimentSubSet}</li>
     * </ul>
     * @return a list of subsets representing subpopulations of cells
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    List<ExpressionExperimentSubSet> splitByCellType( ExpressionExperiment ee, CellTypeAssignment cta, boolean allowUnmappedFactorValues );
}
