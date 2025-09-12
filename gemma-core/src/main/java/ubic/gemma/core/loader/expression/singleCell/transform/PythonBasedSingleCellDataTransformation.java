package ubic.gemma.core.loader.expression.singleCell.transform;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Interface implemented by single-cell data transformation that require Python.
 * @author poirigui
 */
public interface PythonBasedSingleCellDataTransformation extends SingleCellDataTransformation {

    Path DEFAULT_PYTHON_EXECUTABLE = Paths.get( "python" );

    String REQUIREMENTS_FILE = "/ubic/gemma/core/loader/expression/singleCell/transform/requirements.txt";

    void setPythonExecutable( Path pythonExecutable );
}
