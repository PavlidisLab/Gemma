package ubic.gemma.rest.monitoring.health;

/**
 * Single-method contract for a component health probe used by the {@code /rest/v2/health} endpoint.
 * Each implementation reports the status of one slice of the running process (database, cache,
 * disk space, ...). The aggregator in {@code HealthWebService} collects every bean of this type
 * and returns DOWN as soon as any component reports DOWN.
 * <p>
 * Modelled after Spring Boot Actuator's {@code HealthIndicator} so the response JSON can match
 * the Boot shape, but kept in the Gemma package tree to avoid dragging Boot autoconfig.
 *
 * @author Phase 3 actuator wiring
 */
public interface HealthIndicator {

    /**
     * Stable identifier for this component in the aggregated response (e.g. {@code "db"},
     * {@code "cache"}, {@code "diskSpace"}).
     */
    String getName();

    /**
     * Run the probe and report the result. Implementations must be quick (target less than 2
     * seconds) and must not throw; any failure should be reported as a DOWN result with the
     * error message in the detail map.
     */
    HealthResult check();
}
