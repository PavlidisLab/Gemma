package ubic.gemma.core.loader.expression.singleCell;

import java.io.IOException;

/**
 * Represents a single-cell data transformation.
 * @author poirigui
 */
public interface SingleCellDataTransformation {

    /**
     * Perform the transformation.
     * @throws IOException
     */
    void perform() throws IOException;
}
