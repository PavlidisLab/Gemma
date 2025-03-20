package ubic.gemma.core.loader.expression.singleCell.transform;

import java.nio.file.Path;

/**
 * Interface implemented by single-cell data transformation that require Python.
 * @author poirigui
 */
public interface PythonBasedSingleCellDataTransformation extends SingleCellDataTransformation {

    void setPythonExecutable( Path pythonExecutable );
}
