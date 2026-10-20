package ubic.gemma.core.metrics;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

import java.util.List;

/**
 * Static factories for Micrometer registries that need a wiring step Spring XML cannot express
 * directly (because the constructors take Clock or PrometheusConfig statics that are public
 * fields, not factory methods).
 *
 * @author Phase 3 actuator wiring
 */
public final class CompositeMeterRegistryFactory {

    private CompositeMeterRegistryFactory() {
    }

    /**
     * Build a composite registry that forwards every recording to each backing registry.
     */
    public static CompositeMeterRegistry of( List<MeterRegistry> backingRegistries ) {
        CompositeMeterRegistry composite = new CompositeMeterRegistry( Clock.SYSTEM );
        for ( MeterRegistry backing : backingRegistries ) {
            composite.add( backing );
        }
        return composite;
    }

    /**
     * Build a default {@link PrometheusMeterRegistry} using {@link PrometheusConfig#DEFAULT}.
     */
    public static PrometheusMeterRegistry prometheus() {
        return new PrometheusMeterRegistry( PrometheusConfig.DEFAULT );
    }
}
