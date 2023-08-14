package ubic.gemma.web.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import ubic.gemma.core.metrics.AbstractMeterRegistryConfigurer;

/**
 * Configured the {@code environment='web'} tag to the provided registry.
 * @author poirigui
 */
public class MeterRegistryWebConfigurer extends AbstractMeterRegistryConfigurer {

    public MeterRegistryWebConfigurer( MeterRegistry meterRegistry ) {
        super( meterRegistry );
    }

    @Override
    protected void configure( MeterRegistry meterRegistry ) {
        meterRegistry.config()
                .commonTags( "environment", "web" );
    }
}
