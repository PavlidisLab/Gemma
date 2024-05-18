package ubic.gemma.rest.providers;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jersey.server.DefaultJerseyTagsProvider;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.ws.rs.ext.Provider;

@Provider
@Component
public class MetricsApplicationEventListener implements ApplicationEventListener {

    private static final String METRIC_NAME = "gemmaRestServlet";

    @Autowired(required = false)
    private MeterRegistry registry;

    private ApplicationEventListener delegate;

    @PostConstruct
    public void init() {
        if ( registry != null ) {
            delegate = new io.micrometer.core.instrument.binder.jersey.server.MetricsApplicationEventListener(
                    registry, new DefaultJerseyTagsProvider(), METRIC_NAME, true );
        } else {
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
