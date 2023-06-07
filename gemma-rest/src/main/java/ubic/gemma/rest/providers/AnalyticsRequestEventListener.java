package ubic.gemma.rest.providers;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import ubic.gemma.rest.analytics.AnalyticsProvider;

import java.util.*;

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
                Map<String, String> params = new HashMap<>();
                params.put( "method", request.getMethod() );
                params.put( "endpoint", limitParamValue( request.getAbsolutePath().getPath() ) );
                String forwardedFor = request.getHeaderString( "X-Forwarded-For" );
                if ( StringUtils.isNotBlank( forwardedFor ) ) {
                    params.put( "ip", forwardedFor );
                }
                if ( !request.getAcceptableLanguages().isEmpty() ) {
                    params.put( "language", request.getAcceptableLanguages().iterator().next().toLanguageTag().toLowerCase() );
                }
                String userAgent = request.getHeaderString( "User-Agent" );
                if ( StringUtils.isNotBlank( userAgent ) ) {
                    params.put( "user_agent", limitParamValue( userAgent.trim() ) );
                }
                // the analytics need to be collected here because the RequestAttributes will be cleared in REQUEST_FILTERED
                analyticsProvider.sendEvent( eventName, date, params );
                break;
            case FINISHED:
                eventDates.remove( request );
                break;
        }
    }

    private String limitParamValue( String v ) {
        return v.substring( 0, Math.min( v.length(), 100 ) );
    }
}
