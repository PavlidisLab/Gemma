package ubic.gemma.rest.providers;

import lombok.extern.apachecommons.CommonsLog;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;

import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

/**
 * Log request exceptions.
 *
 * @author poirigui
 */
@Provider
@CommonsLog
public class RequestExceptionLogger implements RequestEventListener {

    @Context
    private ContainerRequest request;

    @Override
    public void onEvent( RequestEvent requestEvent ) {
        ContainerRequest request1 = requestEvent.getContainerRequest();
        log.error( String.format( "Unhandled exception while processing request :%s.",
                        request1.getMethod() + ' ' + request1.getRequestUri() ),
                requestEvent.getException() );
    }
}
