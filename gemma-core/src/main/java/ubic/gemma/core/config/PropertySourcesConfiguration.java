package ubic.gemma.core.config;

import org.apache.commons.configuration2.AbstractConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.springframework.core.env.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.StreamSupport;

/**
 * A {@link PropertySources}-based Apache Configuration implementation.
 * @author poirigui
 * @deprecated This has been replaced with Spring-based configuration {@link SettingsConfig} and usage of {@link org.springframework.beans.factory.annotation.Value}
 *             to inject configurations. You can use {@code @Value("${property}")} as replacement.
 */
@Deprecated
public class PropertySourcesConfiguration extends AbstractConfiguration implements Configuration {

    private final PropertySources propertySources;

    private final ConcurrentMap<String, Object> overrides = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, Object> cachedProperties = new ConcurrentHashMap<>();

    public PropertySourcesConfiguration( PropertySources propertySources, DefaultListDelimiterHandler listDelimiterHandler ) {
        MutablePropertySources ps = new MutablePropertySources( propertySources );
        ps.addFirst( new MapPropertySource( "overrides", overrides ) );
        ps.addFirst( new MapPropertySource( "cache", cachedProperties ) );
        this.propertySources = ps;
    }

    @Override
    protected void addPropertyDirect( String key, Object value ) {
        if ( overrides.put( key, value ) != value ) {
            cachedProperties.remove( key );
        }
    }

    @Override
    protected void clearPropertyDirect( String key ) {
        if ( overrides.remove( key ) != null ) {
            cachedProperties.remove( key );
        }
    }

    @Override
    protected Iterator<String> getKeysInternal() {
        return StreamSupport.stream( propertySources.spliterator(), false )
                .filter( ps -> ps instanceof EnumerablePropertySource )
                .map( ps -> ( ( EnumerablePropertySource<?> ) ps ).getPropertyNames() )
                .flatMap( Arrays::stream )
                .distinct() // only return the first key found
                .iterator();
    }

    @Override
    protected Object getPropertyInternal( String key ) {
        Object val = null;
        for ( PropertySource<?> ps : propertySources ) {
            val = ps.getProperty( key );
            // handle cases where the property value is actually 'null'
            if ( val != null || ps.containsProperty( key ) ) {
                break;
            }
        }
        if ( val != null ) {
            // FIXME: cache null values too
            cachedProperties.put( key, val );
        }
        return val;
    }

    @Override
    protected boolean isEmptyInternal() {
        return false;
    }

    @Override
    protected boolean containsKeyInternal( String key ) {
        for ( PropertySource<?> ps : propertySources ) {
            if ( ps.containsProperty( key ) ) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean containsValueInternal( Object o ) {
        return false;
    }
}
