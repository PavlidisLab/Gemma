package ubic.gemma.core.loader.expression.singleCell;

import java.nio.file.Path;

/**
 * Simple transformation with input and output files.
 * @author poirigui
 */
public interface SingleCellInputOutputFileTransformation extends SingleCellDataTransformation {

    void setInputFile( Path inputFile );

    void setInputDataType( SingleCellDataType singleCellDataType );

    void setOutputFile( Path outputFile );

    void setOutputDataType( SingleCellDataType singleCellDataType );

}