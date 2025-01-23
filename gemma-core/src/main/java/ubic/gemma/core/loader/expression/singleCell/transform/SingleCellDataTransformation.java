package ubic.gemma.core.loader.expression.singleCell.transform;

import java.io.IOException;

/**
 * Represents a single-cell data transformation.
 * @author poirigui
 */
public interface SingleCellDataTransformation {

    /**
     * Obtain a short description of what the transformation does.
     */
    String getDescription();

    /**
     * Perform the transformation.
     */
    void perform() throws IOException;
}
