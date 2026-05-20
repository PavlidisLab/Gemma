/*
 * The baseCode project
 *
 * Copyright (c) 2009 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration of ontology services and other things.
 * <p>
 * Configurations are retrieved from three locations: properties set at runtime with {@link #setString(String, String)},
 * system properties and a default properties file named {@code basecode.properties} at the root of the classpath in
 * that order.
 * <p>
 * Properties set via system properties must be prefixed with {@code basecode.} to be considered.
 * <p>
 * Properties set at runtime can be reset with {@link #reset()} and {@link #reset(String)}.
 * <p>
 * Ported in-tree from {@code ubic.basecode.util.Configuration} (baseCode 1.1.34-RENOVATIONS) as the final
 * step in retiring the baseCode Maven dependency. The system-property prefix and the
 * {@code basecode.properties} classpath resource name are preserved verbatim for backward compatibility with
 * existing configuration files and {@link BaseCodeConfigurer}.
 *
 * @author paul
 */
public class Configuration {

    private static final Logger log = LoggerFactory.getLogger( Configuration.class );

    private static final String SYSTEM_PROPERTY_PREFIX = "basecode.";
    private static final Properties defaultProps = new Properties();
    private static final Properties props = new Properties();

    static {
        try ( InputStream is = Configuration.class.getResourceAsStream( "/basecode.properties" ) ) {
            if ( is != null ) {
                defaultProps.load( is );
            } else {
                log.warn( "No basecode.properties was found in the classpath, only system and manually set properties will be considered." );
            }
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Obtain a configuration value by key.
     */
    @Nullable
    public static String getString( String key ) {
        String val = props.getProperty( key );
        if ( val == null ) {
            val = System.getProperty( SYSTEM_PROPERTY_PREFIX + key );
        }
        if ( val == null ) {
            val = defaultProps.getProperty( key );
        }
        return val;
    }

    /**
     * Obtain a boolean configuration value by key.
     *
     * @see Boolean#parseBoolean(String)
     */
    @Nullable
    public static Boolean getBoolean( String key ) {
        String val = getString( key );
        if ( val != null ) {
            return Boolean.parseBoolean( val );
        } else {
            return null;
        }
    }

    /**
     * Set a configuration by key.
     */
    public static void setString( String key, String value ) {
        props.setProperty( key, value );
    }

    /**
     * Reset all configurations set at runtime.
     */
    public static void reset() {
        props.clear();
    }

    /**
     * Reset a specific configuration by key.
     */
    public static void reset( String key ) {
        props.remove( key );
    }
}
