package ubic.gemma.web.services.rest.providers;

import lombok.extern.apachecommons.CommonsLog;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;

import javax.ws.rs.ext.Provider;

/**
 * Log request exceptions.
 *
 * @author poirigui
 */
@Provider
@CommonsLog
public class RequestExceptionLogger implements RequestEventListener {

    @Override
    public void onEvent( RequestEvent requestEvent ) {
        log.error( requestEvent.getException().getMessage(), requestEvent.getException() );
    }
}
