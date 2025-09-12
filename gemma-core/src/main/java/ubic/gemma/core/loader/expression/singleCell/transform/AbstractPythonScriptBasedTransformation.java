package ubic.gemma.core.loader.expression.singleCell.transform;

import lombok.Setter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

@Setter
public abstract class AbstractPythonScriptBasedTransformation extends AbstractScriptBasedTransformation implements PythonBasedSingleCellDataTransformation {

    private Path pythonExecutable = DEFAULT_PYTHON_EXECUTABLE;

    protected AbstractPythonScriptBasedTransformation( String scriptName ) {
        super( scriptName + ".py" );
    }

    @Override
    public void perform() throws IOException {
        log.info( "Using " + getPythonVersion() + " from " + pythonExecutable + "." );
        super.perform();
    }

    protected final String[] createScriptArgs() {
        return ArrayUtils.addAll( new String[] { pythonExecutable.toString(), "-u", "-" }, createPythonScriptArgs() );
    }

    protected abstract String[] createPythonScriptArgs();

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
