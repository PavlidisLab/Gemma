package ubic.gemma.rest.providers;

import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ParamException;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.ext.Provider;

@Provider
@Slf4j
public class RequestExceptionLogger implements ApplicationEventListener {

    @Override
    public void onEvent( ApplicationEvent event ) {

    }

    @Override
    public RequestEventListener onRequest( RequestEvent requestEvent ) {
        return event -> {
            if ( event.getType() == RequestEvent.Type.ON_EXCEPTION ) {
                ContainerRequest request = event.getContainerRequest();
                String m;
                if ( request != null ) {
                    m = String.format( "Exception was raised for %s %s", request.getMethod(), request.getRequestUri() );
                } else {
                    m = "Exception was raised, but there is no current request.";
                }
                Throwable ex = event.getException();
                // Spring-Security auth/authz exceptions are the normal 401/403 path for
                // anonymous probes of authenticated endpoints (e.g. the UI's /me check
                // on every page load). They produce stack traces identical to every
                // other "not logged in" call, so we log the message at WARN without
                // the trace to keep the loop quiet. Jersey wraps the real exception in
                // a MappableException, so walk the cause chain to find the actual type.
                Throwable rootMatch = findCause( ex, AccessDeniedException.class, AuthenticationException.class );
                if ( rootMatch != null ) {
                    log.warn( m + " (" + rootMatch.getClass().getSimpleName() + ": " + rootMatch.getMessage() + ")" );
                } else if ( ex instanceof ClientErrorException
                        // these should be treated as 400 errors, but they do not inherit from BadRequestException
                        || ex instanceof ParamException
                        // these are happening when the client closes the connection before the server can respond, in
                        // gemma-web, see ClientAbortExceptionResolver. It is a Tomcat-specific exception, so we do not
                        // have the class definition
                        || "org.apache.catalina.connector.ClientAbortException".equals( ex.getClass().getName() )
                        || ex instanceof ServiceUnavailableException ) {
                    log.warn( m, ex );
                } else {
                    log.error( m, ex );
                }
            }
        };
    }

    /**
     * Walk {@code t}'s cause chain looking for an instance of one of {@code targets}.
     * Returns the first match (cause-first preferred over wrapper) or {@code null}.
     * Bounded so self-referential cause chains can't spin.
     */
    @SafeVarargs
    private static Throwable findCause( Throwable t, Class<? extends Throwable>... targets ) {
        Throwable cur = t;
        for ( int hops = 0; cur != null && hops < 16; hops++, cur = cur.getCause() ) {
            for ( Class<? extends Throwable> target : targets ) {
                if ( target.isInstance( cur ) ) {
                    return cur;
                }
            }
            if ( cur.getCause() == cur ) {
                break;
            }
        }
        return null;
    }
}
