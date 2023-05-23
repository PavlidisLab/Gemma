package ubic.gemma.rest.analytics;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Interface for analytics providers.
 */
public interface AnalyticsProvider {

    /**
     * Publish an event.
     *
     * @param eventName a name for the event
     * @param date      an exact moment when the event occurred
     * @param params    additional parameters for the event
     */
    void sendEvent( String eventName, Date date, Map<String, String> params );

    /**
     * Publish an event with additional parameters expressed as a sequence of keys and values.
     * @see #sendEvent(String, Date, Map)
     */
    default void sendEvent( String eventName, Date date, String... params ) {
        if ( params.length % 2 != 0 ) {
            throw new IllegalArgumentException( "There must be an even number of params to form key-value pairs." );
        }
        Map<String, String> paramsMap = new HashMap<>();
        for ( int i = 0; i < params.length; i += 2 ) {
            paramsMap.put( params[i], params[i + 1] );
        }
        sendEvent( eventName, date, paramsMap );
    }

    /**
     * Publish an event without specifying a date.
     * @see #sendEvent(String, Date, Map)
     */
    default void sendEvent( String eventName, String... params ) {
        sendEvent( eventName, new Date(), params );
    }
}
