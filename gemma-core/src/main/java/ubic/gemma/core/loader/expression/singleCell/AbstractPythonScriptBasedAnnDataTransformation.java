package ubic.gemma.core.loader.expression.singleCell;

import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.util.Assert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

@Setter
@CommonsLog
public class AbstractPythonScriptBasedAnnDataTransformation implements SingleCellInputOutputFileTransformation {

    public static final String DEFAULT_PYTHON_EXECUTABLE = "python";

    private final String scriptName;

    /**
     * Location of the Python executable.
     */
    private String pythonExecutable = DEFAULT_PYTHON_EXECUTABLE;

    // input/output for the transformation
    private Path inputFile, outputFile;
    private SingleCellDataType inputDataType, outputDataType;

    protected AbstractPythonScriptBasedAnnDataTransformation( String scriptName ) {
        this.scriptName = scriptName;
    }

    /**
     * Check if a Python package is installed.
     */
    public boolean isPackageInstalled( String packageName ) throws IOException {
        Process process = Runtime.getRuntime().exec( new String[] { pythonExecutable, "-c", "import " + packageName } );
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
        try ( InputStream in = requireNonNull( getClass().getResourceAsStream( "/ubic/gemma/core/loader/expression/singleCell/" + scriptName + "-anndata.py" ) ) ) {
            Process process = Runtime.getRuntime().exec( ArrayUtils.addAll( new String[] { pythonExecutable, "-" }, createScriptArgs() ) );
            IOUtils.copy( in, process.getOutputStream() );
            process.getOutputStream().close();
            try ( BufferedReader reader = new BufferedReader( new InputStreamReader( process.getInputStream() ) ) ) {
                reader.lines().forEach( log::info );
            }
            if ( process.waitFor() != 0 ) {
                throw new RuntimeException( "Transposition of " + inputFile + " failed:\n" + IOUtils.toString( process.getErrorStream(), StandardCharsets.UTF_8 ) );
            }
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new RuntimeException( String.format( "Transposition of %s was interrupted.", inputFile ), e );
        }
    }

    protected String[] createScriptArgs() {
        return new String[] { inputFile.toString(), outputFile.toString() };
    }
}
