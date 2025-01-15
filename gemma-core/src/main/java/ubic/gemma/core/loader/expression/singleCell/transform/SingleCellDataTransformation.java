package ubic.gemma.core.loader.expression.singleCell.transform;

import java.io.IOException;

/**
 * Represents a single-cell data transformation.
 * @author poirigui
 */
public interface SingleCellDataTransformation {

    /**
     * Perform the transformation.
     */
    void perform() throws IOException;
}
