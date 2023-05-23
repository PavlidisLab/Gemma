package ubic.gemma.rest.providers;

import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import ubic.gemma.rest.analytics.AnalyticsProvider;

import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Request event listener that publishes an event when a request is finished.
 * @author poirigui
 */
public class AnalyticsRequestEventListener implements RequestEventListener {

    private final String eventName;
    private final Date date;
    private final AnalyticsProvider analyticsProvider;

    private final Map<ContainerRequest, Date> eventDates = Collections.synchronizedMap( new IdentityHashMap<>() );

    public AnalyticsRequestEventListener( String eventName, Date date, AnalyticsProvider analyticsProvider ) {
        this.eventName = eventName;
        this.date = date;
        this.analyticsProvider = analyticsProvider;
    }

    @Override
    public void onEvent( RequestEvent event ) {
        ContainerRequest request = event.getContainerRequest();
        switch ( event.getType() ) {
            case START:
                eventDates.put( request, new Date() );
                break;
            case RESOURCE_METHOD_FINISHED:
                // the analytics need to be collected here because the RequestAttributes will be cleared in REQUEST_FILTERED
                analyticsProvider.sendEvent( eventName, date,
                        "method", request.getMethod(),
                        "endpoint", request.getAbsolutePath().getPath() );
                break;
            case FINISHED:
                eventDates.remove( request );
                break;
        }
    }
}
