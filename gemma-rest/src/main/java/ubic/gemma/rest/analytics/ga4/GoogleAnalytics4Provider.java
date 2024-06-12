package ubic.gemma.rest.analytics.ga4;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.Value;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.util.Assert;
import org.springframework.validation.DirectFieldBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ubic.gemma.rest.analytics.AnalyticsProvider;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of the {@link AnalyticsProvider} interface for Google Analytics 4 collect API.
 * <p>
 * General outline:
 * <p>
 * Events are queued and ordered by date. Events that share the same date within a configured time resolution, client ID
 * and user ID are batched together. The time resolution can be configured via {@link #setResolutionMillis(long)}.
 * <p>
 * A task is run regularly to flush pending events regardless of the queue size and can be configured with
 * {@link #setPollingIntervalMillis(long)}.
 * <p>
 * If a full batch can be sent after {@link #sendEvent(String, Date, Map)} is invoked and no manual flush is currently
 * in-progress, a manual flush is scheduled. This will only flush a single batch of events.
 * <p>
 * When the bean is destroyed, all pending events are flushed.
 *
 * @author poirigui
 */
public class GoogleAnalytics4Provider implements AnalyticsProvider, InitializingBean, DisposableBean {

    private static final Log log = LogFactory.getLog( GoogleAnalytics4Provider.class );

    private static final long DEFAULT_RESOLUTION_MILLIS = 1000;

    private static final long DEFAULT_POLLING_INTERVAL_MILLIS = 1000;

    private static final EventValidator EVENT_VALIDATOR = new EventValidator();

    private static final String
            endpoint = "https://www.google-analytics.com/mp/collect?api_secret={apiSecret}&measurement_id={measurementId}",
            debugEndpoint = "https://www.google-analytics.com/debug/mp/collect?api_secret={apiSecret}&measurement_id={measurementId}";

    private final RestTemplate restTemplate;
    private final ScheduledExecutorService taskExecutor = Executors.newSingleThreadScheduledExecutor();
    private final String measurementId;
    private final String apiSecret;
    private long resolutionMillis = DEFAULT_RESOLUTION_MILLIS;
    private long pollingIntervalMillis = DEFAULT_POLLING_INTERVAL_MILLIS;
    private boolean debug;

    private ClientIdRetrievalStrategy clientIdRetrievalStrategy = () -> null;
    private UserIdRetrievalStrategy userIdRetrievalStrategy = () -> null;


    /* internal state */
    private final Queue<_Event> events = new PriorityBlockingQueue<>();
    private Future<Integer> lastManualFlush = null;

    /**
     * Create a new Google Analytics 4 provider.
     *
     * @param restTemplate  a REST template for performing requests with the collect API
     * @param measurementId a measurement ID which may be empty
     * @param apiSecret     an API secret, must be non-empty if measurementId is supplied
     */
    public GoogleAnalytics4Provider( RestTemplate restTemplate, String measurementId, String apiSecret ) {
        Assert.isTrue( StringUtils.isBlank( measurementId ) || !StringUtils.isBlank( apiSecret ) );
        this.restTemplate = restTemplate;
        this.measurementId = measurementId;
        this.apiSecret = apiSecret;
    }

    /**
     * @see #GoogleAnalytics4Provider(RestTemplate, String, String)
     */
    public GoogleAnalytics4Provider( String measurementId, String apiSecret ) {
        this( new RestTemplate(), measurementId, apiSecret );
    }


    @Override
    public void afterPropertiesSet() {
        taskExecutor.scheduleWithFixedDelay( this::flushPendingEvents, pollingIntervalMillis, pollingIntervalMillis, TimeUnit.MILLISECONDS );
    }

    @Override
    public void destroy() throws Exception {
        try {
            taskExecutor.shutdown();
        } finally {
            flushPendingEvents();
        }
    }

    /**
     * Set the strategy used to retrieve client IDs.
     */
    public void setClientIdRetrievalStrategy( ClientIdRetrievalStrategy clientIdRetrievalStrategy ) {
        this.clientIdRetrievalStrategy = clientIdRetrievalStrategy;
    }

    /**
     * Set the strategy used to retrieve user IDs.
     */
    public void setUserIdRetrievalStrategy( UserIdRetrievalStrategy userIdRetrievalStrategy ) {
        this.userIdRetrievalStrategy = userIdRetrievalStrategy;
    }

    /**
     * Enable the debug mode.
     * <p>
     * When debug mode is enabled, a testing GA4 endpoint will be used and {@link #sendEvent(String, Date, Map)} may
     * raise exception instead of simply warning.
     */
    public void setDebug( boolean debug ) {
        this.debug = debug;
        if ( debug ) {
            log.warn( "Debug mode is enabled for Google Analytics measurement protocol, no analytics will be collected." );
        }
    }

    /**
     * Time resolution for regrouping events in the same batch.
     * <p>
     * Events with the same client and user IDs are batched together if they are less than the resolution apart and
     * reported at the time the first event in the batch occurred.
     * <p>
     * Setting this to zero effectively disable batching of events unless they occur at exactly the same time. This can
     * be achieved by reusing the {@link Date} object when sending multiple events.
     */
    @SuppressWarnings("unused")
    public void setResolutionMillis( long resolutionMillis ) {
        this.resolutionMillis = resolutionMillis;
    }

    /**
     * Interval at which events are polled and flushed.
     */
    public void setPollingIntervalMillis( long pollingIntervalMillis ) {
        this.pollingIntervalMillis = pollingIntervalMillis;
    }

    @Value
    private class _Event implements Comparable<_Event> {

        String clientId;
        @Nullable
        String userId;
        String name;
        Date date;
        Map<String, String> params;

        @Override
        public int compareTo( _Event event ) {
            return date.compareTo( event.date );
        }

        /**
         * Indicate if this event can be combined in the same batch as the supplied event.
         */
        private boolean isBatchableWith( _Event firstEventOfBatch ) {
            return clientId.equals( firstEventOfBatch.clientId )
                    && Objects.equals( userId, firstEventOfBatch.userId )
                    && ( date.getTime() - firstEventOfBatch.date.getTime() ) <= resolutionMillis;
        }
    }

    @Override
    public void sendEvent( String eventName, Date date, Map<String, String> params ) {
        if ( StringUtils.isBlank( measurementId ) ) {
            log.trace( String.format( "No measurement ID is configured; the %s event will be ignored.", eventName ) );
            return;
        }
        String clientId = clientIdRetrievalStrategy.get();
        if ( clientId == null ) {
            log.trace( String.format( "No client ID is could be retrieved; the %s event will be ignored.", eventName ) );
            return;
        }
        _Event e = new _Event( clientIdRetrievalStrategy.get(), userIdRetrievalStrategy.get(), eventName, date, params );
        Errors errors = validateEvent( e );
        if ( errors.hasErrors() ) {
            if ( debug ) {
                throw new InvalidEventException( errors );
            } else {
                log.error( "Invalid event: " + errors );
            }
            return;
        }
        events.add( e );
        log.trace( e );
        flushManuallyIfNeeded();
    }

    private Errors validateEvent( _Event e ) {
        Errors errors = new DirectFieldBindingResult( e, "event" );
        EVENT_VALIDATOR.validate( e, errors );
        return errors;
    }

    /**
     * Validator for events.
     * <p>
     * Conforms to the rules described in <a href="https://developers.google.com/analytics/devguides/collection/protocol/ga4/sending-events">Sending events</a>.
     */
    private static class EventValidator implements Validator {

        private static final Set<String> RESERVED_EVENT_NAMES;
        private static final Set<String> RESERVED_PARAMETER_NAMES;
        private static final Set<String> RESERVED_PARAMETER_NAME_PREFIXES;

        static {
            RESERVED_EVENT_NAMES = readLinesFromResource( "/ubic/gemma/rest/analytics/ga4/reserved-event-names" );
            RESERVED_PARAMETER_NAMES = readLinesFromResource( "/ubic/gemma/rest/analytics/ga4/reserved-parameter-names" );
            RESERVED_PARAMETER_NAME_PREFIXES = readLinesFromResource( "/ubic/gemma/rest/analytics/ga4/reserved-parameter-name-prefixes" );
        }

        private static Set<String> readLinesFromResource( String f ) {
            try ( InputStream is = EventValidator.class.getResourceAsStream( f ) ) {
                return new HashSet<>( IOUtils.readLines( requireNonNull( is ), StandardCharsets.UTF_8 ) );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
        }

        @Override
        public boolean supports( Class<?> clazz ) {
            return _Event.class.isAssignableFrom( clazz );
        }

        @Override
        public void validate( Object target, Errors errors ) {
            _Event e = ( _Event ) target;
            if ( !isValidClientId( e.clientId ) ) {
                errors.rejectValue( "clientId", "invalid", "Invalid GA4 client ID" );
            }
            if ( e.userId != null && e.userId.length() > 255 ) {
                errors.rejectValue( "userId", "size", new Object[] { 255 }, "User ID must not contain more than 255 characters." );
            }
            if ( e.name.length() > 40 ) {
                errors.rejectValue( "name", "size", new Object[] { 40 }, "Event name must not contain more than 40 characters." );
            }
            if ( !e.name.matches( "[a-zA-Z][a-zA-Z0-9_]*" ) ) {
                errors.rejectValue( "name", "onlyAlpha", "Event name can only contain alpha-numeric characters or underscores." );
            }
            if ( RESERVED_EVENT_NAMES.contains( e.name ) ) {
                errors.rejectValue( "name", "reservedEventName", "Event name is reserved." );
            }
            if ( e.date.after( new Date() ) ) {
                errors.rejectValue( "date", "dateMustBeInPast", "Event date must be in the past." );
            }
            if ( e.params.size() > 10 ) {
                errors.rejectValue( "params", "size", new Object[] { 10 }, "Events can have at most 10 parameters." );
            }
            for ( Map.Entry<String, String> entry : e.params.entrySet() ) {
                errors.pushNestedPath( "params" );
                if ( entry.getKey().length() > 40 ) {
                    errors.rejectValue( null, "sizeOfKey", new Object[] { entry.getKey(), 40 }, String.format( "Parameter name for %s must not contain more than 40 characters.", entry.getKey() ) );
                }
                if ( RESERVED_PARAMETER_NAMES.contains( entry.getKey() ) ) {
                    errors.rejectValue( null, "reservedParameterName", new Object[] { entry.getKey() }, String.format( "Parameter name for %s is reserved.", entry.getKey() ) );
                }
                if ( RESERVED_PARAMETER_NAME_PREFIXES.stream().anyMatch( p -> entry.getKey().startsWith( p ) ) ) {
                    errors.rejectValue( null, "reservedParameterNamePrefix", new Object[] { entry.getKey() }, String.format( "Parameter name for %s starts with a reserved prefix.", entry.getKey() ) );
                }
                if ( entry.getValue().length() > 100 ) {
                    errors.rejectValue( null, "sizeOfValue", new Object[] { entry.getKey(), 100 }, String.format( "Parameter value for %s must not contain more than 100 characters.", entry.getKey() ) );
                }
                errors.popNestedPath();
            }
        }
    }

    /**
     * There is no explicit requirements for this, but this is the format UA and GA4 use.
     */
    static boolean isValidClientId( String clientId ) {
        return clientId.matches( "^[0-9]{10}\\.[0-9]{10}$" );
    }

    @Value
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    private static class Payload {
        String clientId;
        @Nullable
        String userId;
        @Nullable
        Long timestampMicros;
        List<Event> events = new ArrayList<>( 25 );
    }

    @Value
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    private static class Event {
        String name;
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, String> params;
    }

    /**
     * Perform a manual flush.
     */
    private void flushManuallyIfNeeded() {
        if ( lastManualFlush != null && !lastManualFlush.isDone() )
            return;
        if ( events.size() < 25 ) {
            return;
        }
        _Event firstEvent = null;
        int nextBatchSize = 0;
        for ( _Event e : events ) {
            if ( firstEvent == null ) {
                firstEvent = e;
            }
            if ( ( e.date.getTime() - firstEvent.date.getTime() ) > resolutionMillis ) {
                break;
            }
            if ( e.isBatchableWith( firstEvent ) ) {
                if ( ++nextBatchSize == 25 ) {
                    log.debug( "Flushing manually..." );
                    lastManualFlush = taskExecutor.submit( this::flush );
                    break;
                }
            }
        }
    }

    /**
     * Flush pending events.
     * <p>
     * New events added while this method executes might not be flushed.
     */
    private void flushPendingEvents() {
        int size = events.size();
        log.debug( String.format( "Flushing at least %d pending events...", size ) );
        int flushed = 0;
        while ( !events.isEmpty() && flushed < size ) {
            flushed += flush();
        }
        log.debug( String.format( String.format( "Flushed %d events in total.", flushed ) ) );
    }

    /**
     * Flush a single batch of events.
     */
    private int flush() {
        _Event firstEvent = null;
        List<_Event> batch = new ArrayList<>( 25 );
        Iterator<_Event> it = events.iterator();
        while ( it.hasNext() && batch.size() < 25 ) {
            _Event e = it.next();
            if ( firstEvent == null ) {
                firstEvent = e;
                batch = new ArrayList<>( 25 );
            }
            // events are ordered by time, so don't need to check every single one of them
            if ( ( e.date.getTime() - firstEvent.date.getTime() ) > resolutionMillis ) {
                break;
            }
            if ( e.isBatchableWith( firstEvent ) ) {
                it.remove();
                batch.add( e );
            }
        }
        if ( batch.isEmpty() )
            return 0;
        Payload payload = new Payload( firstEvent.clientId, firstEvent.userId, firstEvent.date.getTime() * 1000L );
        for ( _Event e : batch ) {
            payload.events.add( new Event( e.name, e.params ) );
        }
        try {
            log.trace( String.format( "Flushing a batch of %d events, %d events pending...", batch.size(), events.size() ) );
            if ( debug ) {
                ValidationResult v = restTemplate.postForObject( debugEndpoint, payload, ValidationResult.class, apiSecret, measurementId );
                if ( !v.validationMessages.isEmpty() ) {
                    throw new IllegalArgumentException( v.toString() );
                }
            } else {
                restTemplate.postForLocation( endpoint, payload, apiSecret, measurementId );
            }
            return batch.size();
        } catch ( RestClientException e ) {
            if ( debug ) {
                throw e;
            } else if ( e instanceof ClientHttpRequestExecution ) {
                log.error( String.format( "Failed to publish %d analytics events due to a client-side exception; the events will be discarded.", batch.size() ), e );
                // we have to lie here because events will not be resent
                return batch.size();
            } else {
                // for server and I/O errors, requeue the events and resend them later
                log.warn( String.format( "Failed to publish %d analytics events due to a server-side or I/O error; the events will be resent later.", batch.size() ), e );
                events.addAll( batch );
                return 0;
            }
        }
    }

    @Data
    private static class ValidationResult {
        public List<ValidationMessage> validationMessages;

        @Data
        private static class ValidationMessage {
            String fieldPath;
            String description;
            String validationCode;
        }

        @Override
        public String toString() {
            if ( validationMessages.isEmpty() ) {
                return "Payload is valid.";
            } else {
                return "Payload is invalid:\n" + validationMessages.stream()
                        .map( v -> String.format( String.format( "%s: %s [%s]", v.fieldPath, v.description, v.validationCode ) ) )
                        .collect( Collectors.joining( "\n- " ) );
            }
        }
    }
}
