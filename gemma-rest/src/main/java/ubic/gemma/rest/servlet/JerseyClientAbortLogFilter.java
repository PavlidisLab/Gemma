package ubic.gemma.rest.servlet;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Suppresses noisy SEVERE log records that Jersey emits when a client disconnects mid-response.
 *
 * <p>The symptom: {@code org.glassfish.jersey.server.ServerRuntime$Responder} logs
 * "Error while closing the output stream in order to commit response" at SEVERE whenever the
 * client socket goes away before Jersey finishes writing the response — typically because the
 * caller hit Ctrl-C, navigated away, or its HTTP client timed out. The root cause is always a
 * {@code org.apache.catalina.connector.ClientAbortException} wrapping a {@code Broken pipe} or
 * {@code Connection reset} {@code IOException}. There is nothing actionable the server can do —
 * the response has already been sent past the point of repair — and the noise drowns out real
 * errors during normal usage (browser preview probes, health checks, abandoned curl calls).
 *
 * <p>This filter installs a {@link java.util.logging.Filter} on the Jersey {@code Responder}
 * logger that drops only records whose thrown cause chain ends in a broken-pipe / connection-reset
 * IOException. Everything else passes through; real Jersey response-writing errors still surface.
 *
 * <p>Registered in {@code web.xml} after {@link JdbcDriverDeregistrationListener}.
 */
public class JerseyClientAbortLogFilter implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger( JerseyClientAbortLogFilter.class );

    private static final String JERSEY_RESPONDER_LOGGER = "org.glassfish.jersey.server.ServerRuntime$Responder";
    private static final String CLIENT_ABORT_FQN = "org.apache.catalina.connector.ClientAbortException";

    private java.util.logging.Logger targetLogger;
    private Filter previousFilter;
    private boolean installed;

    @Override
    public void contextInitialized( ServletContextEvent sce ) {
        targetLogger = java.util.logging.Logger.getLogger( JERSEY_RESPONDER_LOGGER );
        previousFilter = targetLogger.getFilter();
        targetLogger.setFilter( new ClientAbortFilter( previousFilter ) );
        installed = true;
        log.info( "Installed Jersey client-abort log filter on {}", JERSEY_RESPONDER_LOGGER );
    }

    @Override
    public void contextDestroyed( ServletContextEvent sce ) {
        if ( installed && targetLogger != null ) {
            targetLogger.setFilter( previousFilter );
            log.info( "Restored prior log filter on {}", JERSEY_RESPONDER_LOGGER );
        }
    }

    /** Drops only the client-abort flavour of {@code Responder} SEVERE records. */
    private static final class ClientAbortFilter implements Filter {
        private final Filter delegate;

        ClientAbortFilter( Filter delegate ) {
            this.delegate = delegate;
        }

        @Override
        public boolean isLoggable( LogRecord record ) {
            if ( record.getLevel().intValue() >= Level.SEVERE.intValue() && isClientAbort( record.getThrown() ) ) {
                return false;
            }
            return delegate == null || delegate.isLoggable( record );
        }

        private static boolean isClientAbort( Throwable t ) {
            // Walk the cause chain; bound to 16 hops to avoid pathological cycles.
            for ( int i = 0; t != null && i < 16; i++, t = t.getCause() ) {
                if ( CLIENT_ABORT_FQN.equals( t.getClass().getName() ) ) {
                    return true;
                }
                if ( t instanceof IOException ) {
                    String m = t.getMessage();
                    if ( m != null && ( m.contains( "Broken pipe" ) || m.contains( "Connection reset" ) ) ) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
