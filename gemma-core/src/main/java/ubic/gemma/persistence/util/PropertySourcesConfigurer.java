package ubic.gemma.persistence.util;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;

import java.util.List;

/**
 * Add a given list of {@link PropertySource} to the {@link Environment}.
 * @author poirigui
 */
public class PropertySourcesConfigurer implements EnvironmentAware, InitializingBean {

    private final List<PropertySource<?>> propertySources;
    private ConfigurableEnvironment env;

    public PropertySourcesConfigurer( List<PropertySource<?>> propertySources ) {
        this.propertySources = propertySources;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull( env, "A ConfigurableEnvironment was not set, property sources cannot be added." );
        for ( int i = propertySources.size() - 1; i >= 0; i-- ) {
            env.getPropertySources().addFirst( propertySources.get( i ) );
        }
    }

    @Override
    public void setEnvironment( Environment environment ) {
        if ( environment instanceof ConfigurableEnvironment ) {
            this.env = ( ConfigurableEnvironment ) environment;
        }
    }
}