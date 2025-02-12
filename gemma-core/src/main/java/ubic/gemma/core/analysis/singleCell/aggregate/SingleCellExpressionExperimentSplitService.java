package ubic.gemma.core.analysis.singleCell.aggregate;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;

import java.util.List;
import java.util.Map;

/**
 * Split a single-cell expression experiment into sub-experiments by cell type.
 * @author poirigui
 */
public interface SingleCellExpressionExperimentSplitService {

    /**
     * Split a single-cell dataset by cell type.
     * <p>
     * The dataset must have a preferred {@link ubic.gemma.model.expression.bioAssayData.CellTypeAssignment} and a cell
     * type factor in its experimental design.
     * @throws IllegalStateException if there is no preferred cell type assignment, no (or multiple) cell type factor
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    List<ExpressionExperimentSubSet> splitByCellType( ExpressionExperiment ee, SplitConfig config );

    /**
     * Split biomaterials and bioassays by the given {@link CellLevelCharacteristics}.
     * <p>
     * This method will:
     * <ul>
     * <li>create sub-{@link BioMaterial}s for each subject and cell type</li>
     * <li>create corresponding {@link BioAssay}s for the sub-{@link BioMaterial}s</li>
     * <li>attach the {@link BioAssay}s to {@link ExpressionExperimentSubSet}</li>
     * </ul>
     *
     * @return a list of subsets representing subpopulations of cells
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    List<ExpressionExperimentSubSet> split( ExpressionExperiment ee, CellLevelCharacteristics clc, ExperimentalFactor factor, Map<Characteristic, FactorValue> cellTypeMapping, SplitConfig config );
}
