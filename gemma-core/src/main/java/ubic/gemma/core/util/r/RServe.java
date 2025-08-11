package ubic.gemma.core.util.r;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@CommonsLog
class RServe implements AutoCloseable {

    @Nullable
    private final Path socketPath;

    private final Process rserveProcess;

    public RServe( Path rExe, Path socketPath ) throws IOException {
        this.socketPath = socketPath;
        this.rserveProcess = createRServeProcess( rExe, "socket=" + quoteRString( socketPath.toString() ) );
    }

    /**
     * Create an RServe instance that listens on the given port.
     */
    public RServe( Path rExe, int port ) throws IOException {
        this.socketPath = null;
        this.rserveProcess = createRServeProcess( rExe, "port=" + port );
    }

    private static Process createRServeProcess( Path rExe, String... args ) throws IOException {
        Process proc = new ProcessBuilder( rExe.toString(), "-e", "library(Rserve); run.Rserve(" + String.join( ", ", args ) + ")" )
                .redirectOutput( ProcessBuilder.Redirect.appendTo( new File( "/dev/null" ) ) )
                .redirectError( ProcessBuilder.Redirect.PIPE )
                .start();
        try {
            log.debug( "Waiting for RServe to start..." );
            if ( proc.waitFor( 2000, TimeUnit.MILLISECONDS ) ) {
                throw new RuntimeException( String.format( "RServe process exited unexpectedly with code: %d. %s",
                        proc.exitValue(),
                        IOUtils.toString( proc.getErrorStream(), StandardCharsets.UTF_8 ) ) );
            }
            log.debug( "RServe appears to be running." );
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new RuntimeException( e );
        }
        return proc;
    }

    @Override
    public void close() throws IOException {
        log.debug( "Closing RServe process..." );
        rserveProcess.destroy();
        try {
            if ( rserveProcess.waitFor() == 0 ) {
                log.debug( "RServe closed successfully." );
            } else {
                log.error( String.format( "RServe process exited with code: %d. %s", rserveProcess.exitValue(),
                        IOUtils.toString( rserveProcess.getErrorStream(), StandardCharsets.UTF_8 ) ) );
            }
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new RuntimeException( e );
        }
        if ( socketPath != null ) {
            Files.deleteIfExists( socketPath );
        }
    }

    private String quoteRString( String s ) {
        return "\"" + s.replace( "\"", "\\\"" ) + "\"";
    }
}
