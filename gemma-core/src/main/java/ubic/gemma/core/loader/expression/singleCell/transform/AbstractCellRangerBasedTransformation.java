package ubic.gemma.core.loader.expression.singleCell.transform;

import lombok.Setter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.util.Assert;
import ubic.gemma.core.util.runtime.ExtendedRuntime;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author poirigui
 */
public abstract class AbstractCellRangerBasedTransformation extends AbstractScriptBasedTransformation implements CellRangerBasedTransformation {


    /**
     * Check if the current CPU supports AVX instruction set required by Cell Ranger.
     */
    public boolean isCpuSupported() {
        try {
            return Arrays.stream( ExtendedRuntime.getRuntime().getCpuInfo() )
                    .allMatch( cpu -> ArrayUtils.contains( cpu.getFlags(), "avx" ) );
        } catch ( IOException e ) {
            log.warn( "Failed to detect if CPU supports AVX instruction set.", e );
            return false;
        }
    }

    /**
     * Installation prefix of Cell Ranger.
     */
    @Setter
    private Path cellRangerPrefix;

    protected AbstractCellRangerBasedTransformation( String scriptName ) {
        super( scriptName );
    }

    /**
     * Replicate as much as possible the environment from {@code sourceme.bash}.
     */
    @Override
    protected Map<String, String> createEnvironmentVariables() {
        Assert.notNull( cellRangerPrefix, "Cell Ranger prefix must be specified." );
        Map<String, String> env = new HashMap<>();
        env.put( "PATH", String.join( ":",
                cellRangerPrefix.resolve( "bin" ).toString(),
                cellRangerPrefix.resolve( "bin/tenkit" ).toString(),
                cellRangerPrefix.resolve( "external/anaconda/bin" ).toString(),
                cellRangerPrefix.resolve( "external/martial/bin" ).toString(),
                cellRangerPrefix.resolve( "lib/bin" ).toString() ) );
        env.put( "PYTHONPATH", String.join( ":",
                cellRangerPrefix.resolve( "external/martian/adapters/python" ).toString(),
                cellRangerPrefix.resolve( "lib/python" ).toString() ) );
        env.put( "MROPATH", cellRangerPrefix.resolve( "mro" ).toString() );
        env.put( "HDF5_USE_FILE_LOCKING", "FALSE" );
        env.put( "LANG", "C" );
        env.put( "LC_CTYPE", "en_US.UTF-8" );
        env.put( "MKL_CBWR", "COMPATIBLE" );
        env.put( "NPY_DISABLE_CPU_FEATURES", "AVX512F" );
        env.put( "PYTHONNOUSERSITE", "1" );
        env.put( "RUST_BACKTRACE", "1" );
        return env;
    }

    @Override
    public void perform() throws IOException {
        if ( !isCpuSupported() ) {
            throw new RuntimeException( "The current CPU does not support AVX instructions. Those are required for running Cell Ranger." );
        }
        log.info( "Using Cell Ranger " + getCellRangerVersion() + " from " + cellRangerPrefix + "." );
        super.perform();
    }

    private String getCellRangerVersion() {
        ProcessBuilder builder = new ProcessBuilder( getCellRangerExecutable().toString(), "--version" );
        builder.environment().putAll( createEnvironmentVariables() );
        try {
            Process process = builder
                    .redirectOutput( ProcessBuilder.Redirect.PIPE )
                    .redirectError( ProcessBuilder.Redirect.PIPE )
                    .start();
            if ( process.waitFor() != 0 ) {
                throw new RuntimeException( "Obtaining Cell Ranger version failed:\n" + IOUtils.toString( process.getErrorStream(), StandardCharsets.UTF_8 ) );
            }
            return StringUtils.strip( IOUtils.toString( process.getInputStream(), StandardCharsets.UTF_8 ) )
                    .replaceFirst( "^cellranger cellranger-", "" );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new RuntimeException( e );
        }
    }

    protected Path getCellRangerExecutable() {
        return cellRangerPrefix.resolve( "bin/cellranger" );
    }

    /**
     * Obtain a path to the Python executable from the Cell Ranger Anaconda environment.
     */
    protected Path getPythonExecutable() {
        return cellRangerPrefix.resolve( "external/anaconda/bin/python" );
    }

    /**
     * Obtain a list of supported chemistry identifiers.
     */
    protected Set<String> getChemistryIdentifiers() {
        try ( Reader reader = Files.newBufferedReader( cellRangerPrefix.resolve( "lib/python/cellranger/chemistry_defs.json" ) ) ) {
            return new JSONObject( new JSONTokener( reader ) ).keySet();
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }
}
