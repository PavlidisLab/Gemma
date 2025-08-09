package ubic.gemma.core.util.r;

import lombok.extern.apachecommons.CommonsLog;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngine;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A {@link REngine} implementation that launches Rserve using a UNIX domain socket and connects to it with
 * {@link RConnection}.
 * @author poirigui
 * @see RConnection
 */
@CommonsLog
public class StandaloneRConnection extends REngine {

    private final RServe rServe;
    /**
     * Socket through which the R connection communicates with Rserve.
     */
    private final Socket rServeSocket;
    private final RConnection rConnection;

    public StandaloneRConnection( Path rExe ) throws IOException, REngineException {
        Path socketPath = Files.createTempFile( "rserve", ".sock" );
        log.info( "Launching Rserve with socket at " + socketPath + "..." );
        this.rServe = new RServe( rExe, socketPath );
        this.rServeSocket = AFUNIXSocket.connectTo( AFUNIXSocketAddress.of( socketPath ) );
        this.rConnection = new RConnection( rServeSocket );
    }

    @Override
    public REXP parse( String text, boolean resolve ) throws REngineException {
        return rConnection.parse( text, resolve );
    }

    @Override
    public REXP eval( REXP what, REXP where, boolean resolve ) throws REngineException, REXPMismatchException {
        return rConnection.eval( what, where, resolve );
    }

    @Override
    public void assign( String symbol, REXP value, REXP env ) throws REngineException, REXPMismatchException {
        rConnection.assign( symbol, value, env );
    }

    @Override
    public REXP get( String symbol, REXP env, boolean resolve ) throws REngineException, REXPMismatchException {
        return rConnection.get( symbol, env, resolve );
    }

    @Override
    public REXP resolveReference( REXP ref ) throws REngineException {
        return rConnection.resolveReference( ref );
    }

    @Override
    public REXP createReference( REXP value ) throws REngineException {
        return rConnection.createReference( value );
    }

    @Override
    public void finalizeReference( REXP ref ) throws REngineException {
        rConnection.finalizeReference( ref );
    }

    @Override
    public REXP getParentEnvironment( REXP env, boolean resolve ) throws REngineException {
        return rConnection.getParentEnvironment( env, resolve );
    }

    @Override
    public REXP newEnvironment( REXP parent, boolean resolve ) throws REngineException {
        return rConnection.newEnvironment( parent, resolve );
    }

    @Override
    public boolean close() {
        boolean ret = rConnection.close();
        try {
            rServeSocket.close();
        } catch ( IOException e ) {
            log.error( "Failed to close Rserve socket.", e );
            ret = false;
        }
        try {
            rServe.close();
        } catch ( Exception e ) {
            log.error( "Failed to close RServe." );
            ret = false;
        }
        return ret;
    }
}
