package ubic.gemma.core.loader.expression.singleCell.transform;

import ubic.gemma.core.loader.expression.singleCell.SingleCellDataType;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class SingleCellDataTransformationPipeline implements SingleCellDataTransformation, SingleCellInputOutputFileTransformation, PythonBasedSingleCellDataTransformation {

    private final List<SingleCellInputOutputFileTransformation> transformations;

    private Path inputFile;
    private SingleCellDataType inputDataType;
    private Path outputFile;
    private SingleCellDataType outputDataType;
    @Nullable
    private Path scratchDir;

    public SingleCellDataTransformationPipeline( List<SingleCellInputOutputFileTransformation> transformations ) {
        this.transformations = transformations;
    }

    @Override
    public void setPythonExecutable( Path pythonExecutable ) {
        transformations.forEach( t -> {
            if ( t instanceof PythonBasedSingleCellDataTransformation ) {
                ( ( PythonBasedSingleCellDataTransformation ) t ).setPythonExecutable( pythonExecutable );
            }
        } );
    }

    /**
     * Set the scratch directory where temporary files will be created.
     */
    public void setScratchDir( Path scratchDir ) {
        this.scratchDir = scratchDir;
    }

    @Override
    public void setInputFile( Path inputFile, SingleCellDataType singleCellDataType ) {
        this.inputFile = inputFile;
        this.inputDataType = singleCellDataType;
    }

    @Override
    public void setOutputFile( Path outputFile, SingleCellDataType singleCellDataType ) {
        this.outputFile = outputFile;
        this.outputDataType = singleCellDataType;
    }

    @Override
    public String getDescription() {
        return transformations.stream()
                .map( singleCellInputOutputFileTransformation -> {
                    if ( singleCellInputOutputFileTransformation instanceof SingleCellDataTransformationPipeline ) {
                        return "(" + singleCellInputOutputFileTransformation.getDescription() + ")";
                    } else {
                        return singleCellInputOutputFileTransformation.getDescription();
                    }
                } )
                .collect( Collectors.joining( " → " ) );
    }

    @Override
    public void perform() throws IOException {
        Path tempInputFile;
        if ( scratchDir != null ) {
            tempInputFile = Files.createTempFile( scratchDir, "gemma-single-cell-data-transform-", "h5ad" );
        } else {
            tempInputFile = Files.createTempFile( "gemma-single-cell-data-transform-", "h5ad" );
        }
        Path tempOutputFile;
        if ( scratchDir != null ) {
            tempOutputFile = Files.createTempFile( scratchDir, "gemma-single-cell-data-transform-", "h5ad" );
        } else {
            tempOutputFile = Files.createTempFile( "gemma-single-cell-data-transform-", "h5ad" );
        }
        SingleCellDataType tempDataType = SingleCellDataType.ANNDATA;
        try {
            for ( int i = 0; i < transformations.size(); i++ ) {
                SingleCellInputOutputFileTransformation transformation = transformations.get( i );
                if ( i == 0 ) {
                    // first step
                    transformation.setInputFile( inputFile, inputDataType );
                } else {
                    transformation.setInputFile( tempInputFile, tempDataType );
                }
                if ( i == transformations.size() - 1 ) {
                    // last step
                    transformation.setOutputFile( outputFile, outputDataType );
                } else {
                    transformation.setOutputFile( tempOutputFile, outputDataType );
                }
                transformation.perform();
                // swap temp input/output
                Path temp = tempInputFile;
                tempInputFile = tempOutputFile;
                tempOutputFile = temp;
            }
        } finally {
            Files.delete( tempInputFile );
            Files.delete( tempOutputFile );
        }
    }
}
