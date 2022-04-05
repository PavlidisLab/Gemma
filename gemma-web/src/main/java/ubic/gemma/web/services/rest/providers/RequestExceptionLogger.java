package ubic.gemma.web.services.rest.providers;

import lombok.extern.apachecommons.CommonsLog;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import ubic.gemma.web.services.rest.util.ServletUtils;

import javax.servlet.http.HttpServletRequest;
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
    private HttpServletRequest request;

    @Override
    public void onEvent( RequestEvent requestEvent ) {
        log.error( "Unhandled exception while processing request :" + ServletUtils.summarizeRequest( request ) + ".",
                requestEvent.getException() );
    }
}
