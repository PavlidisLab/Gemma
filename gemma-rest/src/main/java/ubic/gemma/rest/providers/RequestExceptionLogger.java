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
import ubic.gemma.rest.util.OntologyTermValidationException;

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
                // Triage policy: this logger's job is to surface SERVER-side problems.
                // Client-side outcomes (4xx) are already captured in the access log; a
                // duplicate app-log line with a stack trace adds no diagnostic value and
                // produces gigabytes of noise when a misbehaving client polls in a tight
                // loop (the 2026-05-26 /me firehose). So:
                //   - AccessDeniedException / AuthenticationException → DEBUG (anonymous
                //     probes of authenticated endpoints are normal traffic; absent any
                //     diagnostic interest, don't log).
                //   - Other 4xx-mapped exceptions → WARN with message only (no stack).
                //   - Everything else → ERROR with full stack (the actual server faults).
                // Jersey wraps in MappableException; walk the cause chain to find the
                // actual type so the branches match.
                Throwable authMatch = findCause( ex, AccessDeniedException.class, AuthenticationException.class );
                // Exceptions that map to a 4xx but are wrapped by Jersey (MappableException) so a top-level
                // instanceof won't see them. OntologyTermValidationException is a validation 400 whose (enriched)
                // message names every failing term — log it at WARN without a stack, not as a server fault.
                Throwable clientMatch = findCause( ex, OntologyTermValidationException.class );
                if ( authMatch != null ) {
                    log.debug( "{} ({}: {})", m, authMatch.getClass().getSimpleName(), authMatch.getMessage() );
                } else if ( clientMatch != null
                        || ex instanceof ClientErrorException
                        // these should be treated as 400 errors, but they do not inherit from BadRequestException
                        || ex instanceof ParamException
                        // these are happening when the client closes the connection before the server can respond, in
                        // gemma-web, see ClientAbortExceptionResolver. It is a Tomcat-specific exception, so we do not
                        // have the class definition
                        || "org.apache.catalina.connector.ClientAbortException".equals( ex.getClass().getName() )
                        || ex instanceof ServiceUnavailableException ) {
                    Throwable c = clientMatch != null ? clientMatch : ex;
                    log.warn( "{} ({}: {})", m, c.getClass().getSimpleName(), c.getMessage() );
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
