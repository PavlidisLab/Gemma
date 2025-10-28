package ubic.gemma.core.loader.expression.singleCell.transform;

import lombok.Setter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static java.util.Objects.requireNonNull;

@Setter
public abstract class AbstractScriptBasedTransformation implements SingleCellDataTransformation {

    protected final Log log = LogFactory.getLog( getClass() );

    private final String scriptName;

    public AbstractScriptBasedTransformation( String scriptName ) {
        this.scriptName = scriptName;
    }

    @Override
    public void perform() throws IOException {
        String scriptPath = "/ubic/gemma/core/loader/expression/singleCell/transform/" + scriptName;
        URL scriptUrl = getClass().getResource( scriptPath );
        try ( InputStream in = requireNonNull( scriptUrl, "Could not locate script under " + scriptUrl ).openStream() ) {
            ProcessBuilder processBuilder = new ProcessBuilder( createScriptArgs() )
                    .redirectOutput( ProcessBuilder.Redirect.PIPE )
                    .redirectError( ProcessBuilder.Redirect.PIPE );
            processBuilder.environment().putAll( createEnvironmentVariables() );
            Process process = processBuilder.start();
            IOUtils.copy( in, process.getOutputStream() );
            process.getOutputStream().close();
            try ( BufferedReader reader = new BufferedReader( new InputStreamReader( process.getInputStream() ) ) ) {
                reader.lines().forEach( log::info );
            }
            if ( process.waitFor() != 0 ) {
                throw new RuntimeException( "Transformation failed:\n" + createErrorMessage( process.getErrorStream(), scriptUrl ) );
            }
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new RuntimeException( "Transformation was interrupted.", e );
        }
    }

    /**
     * Create an error message from a failing script.
     */
    protected String createErrorMessage( InputStream errorStream, URL scriptUrl ) throws IOException {
        return IOUtils.toString( errorStream, StandardCharsets.UTF_8 );
    }

    protected abstract String[] createScriptArgs();

    protected abstract Map<String, String> createEnvironmentVariables();
}
