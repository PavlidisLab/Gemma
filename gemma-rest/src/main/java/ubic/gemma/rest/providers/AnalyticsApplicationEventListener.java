package ubic.gemma.rest.providers;

import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.rest.analytics.AnalyticsProvider;

import javax.ws.rs.ext.Provider;
import java.util.Date;

/**
 * Register {@link AnalyticsRequestEventListener}
 * @author poirigui
 */
@Provider
@Component
public class AnalyticsApplicationEventListener implements ApplicationEventListener {

    @Autowired
    private AnalyticsProvider analyticsProvider;

    @Override
    public void onEvent( ApplicationEvent event ) {
    }

    @Override
    public RequestEventListener onRequest( RequestEvent requestEvent ) {
        return new AnalyticsRequestEventListener( "gemma_rest_api_access", new Date(), analyticsProvider );
    }
}
