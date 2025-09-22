package ubic.gemma.core.loader.expression.singleCell.transform;

import java.nio.file.Path;

/**
 * @author poirigui
 */
public interface CellRangerBasedTransformation extends SingleCellDataTransformation {

    /**
     * Set the installation prefix of Cell Ranger.
     */
    void setCellRangerPrefix( Path cellRangerPrefix );
}
