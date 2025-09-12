package ubic.gemma.core.loader.expression.singleCell.transform;

import org.springframework.util.Assert;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataType;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

public abstract class AbstractPythonScriptBasedAnnDataTransformation extends AbstractPythonScriptBasedTransformation implements SingleCellInputOutputFileTransformation {

    // input/output for the transformation
    private Path inputFile, outputFile;

    protected AbstractPythonScriptBasedAnnDataTransformation( String scriptName ) {
        super( scriptName + "-anndata" );
    }

    @Override
    public void setInputFile( Path inputFile, SingleCellDataType singleCellDataType ) {
        Assert.isTrue( singleCellDataType == SingleCellDataType.ANNDATA,
                "Only AnnData is supported as input for this transformation." );
        this.inputFile = inputFile;
    }

    @Override
    public void setOutputFile( Path outputFile, SingleCellDataType singleCellDataType ) {
        Assert.isTrue( singleCellDataType == SingleCellDataType.ANNDATA,
                "Only AnnData is supported as output for this transformation." );
        this.outputFile = outputFile;
    }

    protected String[] createPythonScriptArgs() {
        Assert.notNull( inputFile, "An input file must be provided for the transformation." );
        Assert.notNull( outputFile, "An output file must be provided for the transformation." );
        return new String[] { inputFile.toString(), outputFile.toString() };
    }

    @Override
    protected Map<String, String> createEnvironmentVariables() {
        return Collections.emptyMap();
    }
}
