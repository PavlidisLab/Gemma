package ubic.gemma.core.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;

/**
 * Configure the {@link MeterRegistry} configured for this application.
 * <p>
 * This attaches basic JVM, Hibernate and Ehcache monitoring.
 *
 * @author poirigui
 */
public class MeterRegistryConfigurer implements InitializingBean {

    private final MeterRegistry registry;
    private final List<MeterBinder> meterBinders;

    public MeterRegistryConfigurer( MeterRegistry registry, List<MeterBinder> meterBinders ) {
        this.registry = registry;
        this.meterBinders = meterBinders;
    }

    @Override
    public void afterPropertiesSet() {
        for ( MeterBinder metrics : meterBinders ) {
            metrics.bindTo( registry );
        }
    }
}
