package ubic.gemma.persistence.service.expression.experiment;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;

import java.util.List;

/**
 * Split a single-cell expression experiment into sub-experiments by cell type.
 * @author poirigui
 */
public interface SingleCellExpressionExperimentSplitService {

    /**
     * Split single-cell vectors by the preferred cell type assignment.
     * @see #splitByCellType(ExpressionExperiment, CellTypeAssignment, ExperimentalFactor)
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    List<ExpressionExperimentSubSet> splitByCellType( ExpressionExperiment ee );

    /**
     * Split biomaterials and bioassays by the given cell type assignment and factor.
     * <p>
     * This method will:
     * <ul>
     * <li>create sub-biomaterial for each subject and cell type</li>
     * <li>create bioassays for the sub-biomaterials</li>
     * <li>attach the bioassay to the experiment</li>
     * </ul>
     *
     * @return a list of samples representing subpopulations of cells
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    List<ExpressionExperimentSubSet> splitByCellType( ExpressionExperiment ee, CellTypeAssignment cta, ExperimentalFactor cellTypeFactor );
}
