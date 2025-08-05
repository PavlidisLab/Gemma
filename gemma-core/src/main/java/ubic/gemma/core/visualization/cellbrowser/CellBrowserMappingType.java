package ubic.gemma.core.visualization.cellbrowser;

import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;

public enum CellBrowserMappingType {
    /**
     * @see ExperimentalDesign#getExperimentalFactors()
     */
    FACTOR,
    /**
     * @see SingleCellDimension#getCellTypeAssignments()
     */
    CELL_TYPE_ASSIGNMENT,
    /**
     * @see SingleCellDimension#getCellLevelCharacteristics()
     */
    CELL_LEVEL_CHARACTERISTICS
}
