package ubic.gemma.web.assets;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.IOUtils;
import org.springframework.context.SmartLifecycle;
import org.springframework.util.Assert;
import ubic.gemma.core.util.concurrent.ThreadUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

/**
 * An internal static asset server that uses npm to serve static assets.
 * @author poirigui
 */
@CommonsLog
public class InternalStaticAssetServer implements StaticAssetServer, SmartLifecycle {

    private final Path npmExe;
    private final Path prefix;
    private final String baseUrl;
    private final boolean autoStartup;
    private final Path logFile;

    @Nullable
    private Process npmServeProcess;
    @Nullable
    private String errorMessage;

    public InternalStaticAssetServer( Path npmExe, Path prefix, String baseUrl, boolean autoStartup, Path logFile ) {
        this.npmExe = npmExe;
        this.prefix = prefix;
        this.baseUrl = baseUrl;
        this.autoStartup = autoStartup;
        this.logFile = logFile;
    }

    @Override
    public String getBaseUrl() {
        return baseUrl;
    }

    @Override
    public void start() {
        if ( npmServeProcess != null && npmServeProcess.isAlive() ) {
            return;
        }
        if ( npmServeProcess != null && npmServeProcess.exitValue() != 0 ) {
            log.error( "npm serve process exited with code " + npmServeProcess.exitValue() + ", it will be restarted." );
        }
        startNpmServeProcess();
    }

    private void startNpmServeProcess() {
        Assert.isTrue( Files.exists( prefix.resolve( "package.json" ) ), "There is no package.json under " + prefix + "." );
        try {
            log.info( "Launching npm serve from " + prefix + "..." );
            npmServeProcess = new ProcessBuilder( npmExe.toString(), "--prefix", prefix.toString(), "run", "serve" )
                    .redirectOutput( logFile.toFile() )
                    .redirectError( ProcessBuilder.Redirect.PIPE )
                    .start();
            // this thread will live as long as the process does, so no need to manage its lifecycle
            ThreadUtils.newThread( () -> {
                try {
                    if ( npmServeProcess.waitFor() != 0 ) {
                        errorMessage = IOUtils.toString( npmServeProcess.getErrorStream(), StandardCharsets.UTF_8 );
                        log.error( "npm serve process appeared to have crashed:\n"
                                + errorMessage );
                    }
                } catch ( IOException e ) {
                    log.error( "Error reading npm serve process output.", e );
                } catch ( InterruptedException e ) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException( e );
                }
            }, "gemma-npm-serve-monitor-thread" ).start();
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    public void stop() {
        if ( npmServeProcess != null ) {
            npmServeProcess.destroy();
        }
    }

    @Override
    public boolean isRunning() {
        return npmServeProcess != null && npmServeProcess.isAlive();
    }

    @Override
    public boolean isAlive() {
        return isRunning();
    }

    @Override
    public String getLaunchInstruction() {
        if ( errorMessage != null ) {
            return "An error occurred when launching npm serve:<br><pre>" + escapeHtml4( errorMessage ) + "</pre>";
        } else {
            return "The asset server is launched automatically by Gemma.";
        }
    }

    @Override
    public boolean isAutoStartup() {
        return autoStartup;
    }

    @Override
    public void stop( Runnable callback ) {
        stop();
        callback.run();
    }

    @Override
    public int getPhase() {
        return 0;
    }
}
