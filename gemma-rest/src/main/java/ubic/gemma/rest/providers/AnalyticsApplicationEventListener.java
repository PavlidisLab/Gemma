package ubic.gemma.rest.providers;

import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.springframework.web.context.support.WebApplicationContextUtils;
import ubic.gemma.rest.analytics.AnalyticsProvider;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import java.util.Date;

/**
 * Register {@link AnalyticsRequestEventListener}
 * @author poirigui
 */
@Provider
public class AnalyticsApplicationEventListener implements ApplicationEventListener {

    @Context
    private ServletContext servletContext;

    private AnalyticsProvider analyticsProvider;

    @PostConstruct
    public void init() {
        analyticsProvider = WebApplicationContextUtils
                .getRequiredWebApplicationContext( servletContext )
                .getBean( AnalyticsProvider.class );
    }

    @Override
    public void onEvent( ApplicationEvent event ) {
    }

    @Override
    public RequestEventListener onRequest( RequestEvent requestEvent ) {
        return new AnalyticsRequestEventListener( "gemma_rest_api_access", new Date(), analyticsProvider );
    }
}
