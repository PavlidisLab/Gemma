package ubic.gemma.rest.providers;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jersey.server.DefaultJerseyTagsProvider;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

@Provider
public class MetricsApplicationEventListener implements ApplicationEventListener {

    private static final String METRIC_NAME = "gemmaRestServlet";

    @Context
    private ServletContext servletContext;

    private ApplicationEventListener delegate;

    @PostConstruct
    public void init() {
        WebApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext( servletContext );
        try {
            MeterRegistry registry = ctx.getBean( MeterRegistry.class );
            delegate = new io.micrometer.core.instrument.binder.jersey.server.MetricsApplicationEventListener(
                    registry, new DefaultJerseyTagsProvider(), METRIC_NAME, true );
        } catch ( NoSuchBeanDefinitionException e ) {
            delegate = new org.glassfish.jersey.server.monitoring.ApplicationEventListener() {

                @Override
                public void onEvent( ApplicationEvent event ) {

                }

                @Override
                public RequestEventListener onRequest( RequestEvent requestEvent ) {
                    return null;
                }
            };
        }
    }

    @Override
    public void onEvent( ApplicationEvent event ) {
        delegate.onEvent( event );
    }

    @Override
    public RequestEventListener onRequest( RequestEvent requestEvent ) {
        return delegate.onRequest( requestEvent );
    }
}
