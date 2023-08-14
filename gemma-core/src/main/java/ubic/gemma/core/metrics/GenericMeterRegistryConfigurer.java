package ubic.gemma.core.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import java.util.List;

/**
 * Attach all the given {@link MeterBinder} to the registry.
 * @author poirigui
 */
public class GenericMeterRegistryConfigurer extends AbstractMeterRegistryConfigurer {

    private final List<MeterBinder> meterBinders;

    public GenericMeterRegistryConfigurer( MeterRegistry registry, List<MeterBinder> meterBinders ) {
        super( registry );
        this.meterBinders = meterBinders;
    }

    @Override
    protected void configure( MeterRegistry registry ) {
        for ( MeterBinder metrics : meterBinders ) {
            metrics.bindTo( registry );
        }
    }
}
