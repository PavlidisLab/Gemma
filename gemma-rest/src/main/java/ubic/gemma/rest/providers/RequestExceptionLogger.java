package ubic.gemma.rest.providers;

import lombok.extern.apachecommons.CommonsLog;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ParamException;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.ext.Provider;

@Provider
@CommonsLog
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
                if ( event.getException() instanceof ClientErrorException
                        // these should be treated as 400 errors, but they do not inherit from BadRequestException
                        || event.getException() instanceof ParamException
                        || event.getException() instanceof ServiceUnavailableException ) {
                    log.warn( m, event.getException() );
                } else {
                    log.error( m, event.getException() );
                }
            }
        };
    }
}
