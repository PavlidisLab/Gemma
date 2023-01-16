package ubic.gemma.core.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.InitializingBean;

public abstract class AbstractMeterRegistryConfigurer implements InitializingBean {

    private final MeterRegistry registry;

    protected AbstractMeterRegistryConfigurer( MeterRegistry registry ) {
        this.registry = registry;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        configure( registry );
    }

    protected abstract void configure( MeterRegistry registry );
}
