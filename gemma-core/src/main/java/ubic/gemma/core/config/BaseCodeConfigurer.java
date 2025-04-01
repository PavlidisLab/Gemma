package ubic.gemma.core.config;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.util.Assert;
import ubic.basecode.util.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Properties;

/**
 * Configure the baseCode library from a given property sources.
 * <p>
 * The preferred way is using the {@code basecode.} prefix in the properties.
 * <p>
 * For backward-compatibility, it will also detect properties declared in {@code basecode.properties} and warn
 * accordingly.
 * @see Configuration
 * @author poirigui
 */
@CommonsLog
public class BaseCodeConfigurer implements BeanFactoryPostProcessor {

    private static final String BASECODE_PROPERTY_PREFIX = "basecode.";

    private PropertySources propertySources;

    public void setPropertySources( PropertySources propertySources ) {
        this.propertySources = propertySources;
    }

    @Override
    public void postProcessBeanFactory( ConfigurableListableBeanFactory beanFactory ) throws BeansException {
        Assert.notNull( propertySources, "Property sources must be set." );
        // check if some of the baseCode properties are used literally, they should always be prefixed with 'basecode.'
        Properties basecodeProps = new Properties();
        try ( InputStream s = getClass().getResourceAsStream( "/basecode.properties" ) ) {
            basecodeProps.load( s );
        } catch ( IOException e ) {
            log.warn( "Could not locate basecode.properties in classpath.", e );
        }
        // preserve order in which props were seen
        LinkedHashSet<String> props = new LinkedHashSet<>();
        for ( PropertySource<?> ps : propertySources ) {
            if ( ps instanceof EnumerablePropertySource ) {
                for ( String prop : ( ( EnumerablePropertySource<?> ) ps ).getPropertyNames() ) {
                    if ( prop.startsWith( BASECODE_PROPERTY_PREFIX ) ) {
                        props.add( prop );
                    } else if ( basecodeProps.containsKey( prop ) ) {
                        log.warn( "Property " + prop + " in " + ps.getName() + " matches a baseCode property, but is not prefixed with 'basecode.'. It will be ignored." );
                    }
                }
            } else {
                log.warn( String.format( "%s does not support enumeration of properties, cannot read 'basecode.'-prefixed properties from it.", ps ) );
            }
        }
        PropertySourcesPropertyResolver resolver = new PropertySourcesPropertyResolver( propertySources );
        for ( String prop : props ) {
            String val = resolver.getRequiredProperty( prop );
            String baseCodeProp = prop.substring( BASECODE_PROPERTY_PREFIX.length() );
            log.debug( String.format( "Setting baseCode configuration %s to %s.", baseCodeProp, val ) );
            Configuration.setString( baseCodeProp, val );
        }
    }
}
