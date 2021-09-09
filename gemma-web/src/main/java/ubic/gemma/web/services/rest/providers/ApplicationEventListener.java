package ubic.gemma.web.services.rest.providers;

import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;

import javax.ws.rs.ext.Provider;

/**
 * Listen and dispatch {@link ApplicationEvent}.
 * @author poirigui
 */
@Provider
public class ApplicationEventListener implements org.glassfish.jersey.server.monitoring.ApplicationEventListener {

    @Override
    public void onEvent( ApplicationEvent applicationEvent ) {
    }

    @Override
    public RequestEventListener onRequest( RequestEvent requestEvent ) {
        if ( requestEvent.getType() == RequestEvent.Type.ON_EXCEPTION ) {
            return new RequestExceptionLogger();
        } else {
            return null;
        }
    }
}
