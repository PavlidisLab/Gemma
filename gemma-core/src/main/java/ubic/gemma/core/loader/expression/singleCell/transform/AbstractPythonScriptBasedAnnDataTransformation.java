package ubic.gemma.core.loader.expression.singleCell.transform;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.Objects.requireNonNull;

@CommonsLog
public abstract class AbstractPythonScriptBasedAnnDataTransformation implements PythonBasedSingleCellDataTransformation, SingleCellInputOutputFileTransformation {

    public static final Path DEFAULT_PYTHON_EXECUTABLE = Paths.get( "python" );

    private final String scriptName;

    /**
     * Location of the Python executable.
     */
    private Path pythonExecutable = DEFAULT_PYTHON_EXECUTABLE;

    // input/output for the transformation
    private Path inputFile, outputFile;
    private SingleCellDataType inputDataType, outputDataType;

    protected AbstractPythonScriptBasedAnnDataTransformation( String scriptName ) {
        this.scriptName = scriptName;
    }

    @Override
    public void setPythonExecutable( Path pythonExecutable ) {
        this.pythonExecutable = pythonExecutable;
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

    /**
     * Check if a Python package is installed.
     */
    public boolean isPackageInstalled( String packageName ) throws IOException {
        Process process = Runtime.getRuntime().exec( new String[] { pythonExecutable.toString(), "-c", "import " + packageName } );
        try {
            if ( process.waitFor() != 0 ) {
                log.warn( "import " + packageName + " failed:\n" + IOUtils.toString( process.getErrorStream(), StandardCharsets.UTF_8 ) );
                return false;
            }
            return true;
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new RuntimeException( e );
        }
    }

    @Override
    public void perform() throws IOException {
        Assert.notNull( inputFile );
        Assert.notNull( outputFile );
        Assert.isTrue( inputDataType == SingleCellDataType.ANNDATA, "Only AnnData is supported as input for this transformation." );
        Assert.isTrue( outputDataType == SingleCellDataType.ANNDATA, "Only AnnData is supported as output for this transformation." );
        log.info( "Using " + getPythonVersion() + " from " + pythonExecutable + "." );
        try ( InputStream in = requireNonNull( getClass().getResourceAsStream( "/ubic/gemma/core/loader/expression/singleCell/" + scriptName + "-anndata.py" ) ) ) {
            Process process = Runtime.getRuntime().exec( ArrayUtils.addAll( new String[] { pythonExecutable.toString(), "-" }, createScriptArgs() ) );
            IOUtils.copy( in, process.getOutputStream() );
            process.getOutputStream().close();
            try ( BufferedReader reader = new BufferedReader( new InputStreamReader( process.getInputStream() ) ) ) {
                reader.lines().forEach( log::info );
            }
            if ( process.waitFor() != 0 ) {
                throw new RuntimeException( "Transformation of " + inputFile + " failed:\n" + IOUtils.toString( process.getErrorStream(), StandardCharsets.UTF_8 ) );
            }
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new RuntimeException( String.format( "Transformation of %s was interrupted.", inputFile ), e );
        }
    }

    protected String[] createScriptArgs() {
        return new String[] { inputFile.toString(), outputFile.toString() };
    }

    private String getPythonVersion() throws IOException {
        Process process = Runtime.getRuntime().exec( new String[] { pythonExecutable.toString(), "--version" } );
        try {
            if ( process.waitFor() == 0 ) {
                return StringUtils.strip( IOUtils.toString( process.getInputStream(), StandardCharsets.UTF_8 ) );
            } else {
                throw new RuntimeException( "Checking Python version failed:\n" + IOUtils.toString( process.getErrorStream(), StandardCharsets.UTF_8 ) );
            }
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new RuntimeException( e );
        }
    }
}
