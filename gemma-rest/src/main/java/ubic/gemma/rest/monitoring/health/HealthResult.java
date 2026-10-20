package ubic.gemma.rest.monitoring.health;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Outcome of a {@link HealthIndicator#check()} call: a UP/DOWN status plus an ordered map of
 * detail key-value pairs (e.g. {@code database=MySQL}, {@code freeBytes=12345678}).
 *
 * @author Phase 3 actuator wiring
 */
public final class HealthResult {

    public enum Status {
        UP,
        DOWN
    }

    private final Status status;
    private final Map<String, Object> details;

    private HealthResult( Status status, Map<String, Object> details ) {
        this.status = status;
        this.details = Collections.unmodifiableMap( details );
    }

    public static HealthResult up() {
        return new HealthResult( Status.UP, new LinkedHashMap<>() );
    }

    public static HealthResult up( Map<String, Object> details ) {
        return new HealthResult( Status.UP, new LinkedHashMap<>( details ) );
    }

    public static HealthResult down( String message ) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put( "error", message );
        return new HealthResult( Status.DOWN, d );
    }

    public static HealthResult down( Map<String, Object> details ) {
        return new HealthResult( Status.DOWN, new LinkedHashMap<>( details ) );
    }

    public Status getStatus() {
        return status;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
