package ubic.gemma.persistence.util;

import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.context.SmartLifecycle;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Establishes a local SSH tunnel.
 * @author poirigui
 */
@CommonsLog
@Setter
public class LocalSshTunnel implements SmartLifecycle {

    private String host;
    @Nullable
    private Integer port;

    // for the tunnel
    private int localPort;
    private String remoteHost;
    private int remotePort;

    private boolean autoStart = false;

    private Process tunnelProcess;

    @Override
    public void start() {
        Assert.isTrue( host != null && localPort > 0 && remoteHost != null && remotePort > 0 );
        try {
            String[] args = new String[] { "ssh", "-L", localPort + ":" + remoteHost + ":" + remotePort, host };
            if ( port != null ) {
                args = ArrayUtils.addAll( args, "-p", String.valueOf( port ) );
            }
            tunnelProcess = Runtime.getRuntime().exec( args );
            // quickly check if the process exited
            if ( tunnelProcess.waitFor( 100, TimeUnit.MILLISECONDS ) ) {
                throw new RuntimeException( IOUtils.toString( tunnelProcess.getErrorStream(), StandardCharsets.UTF_8 ) );
            }
        } catch ( IOException | InterruptedException e ) {
            throw new RuntimeException( e );
        }
        log.info( String.format( "Established a SSH tunnel to %s%s: %d -> %s:%d.",
                host, port != null ? ":" + port : "", localPort, remoteHost, remotePort ) );
    }

    @Override
    public void stop() {
        tunnelProcess.destroy();
    }

    @Override
    public boolean isRunning() {
        return tunnelProcess != null && tunnelProcess.isAlive();
    }

    @Override
    public boolean isAutoStartup() {
        return autoStart;
    }

    @Override
    public void stop( Runnable callback ) {
        stop();
    }

    @Override
    public int getPhase() {
        return 0;
    }
}
