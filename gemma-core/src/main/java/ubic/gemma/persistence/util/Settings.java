/*
 * The Gemma project
 *
 * Copyright (c) 2006-2008 University of British Columbia
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
package ubic.gemma.persistence.util;

import org.apache.commons.configuration2.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import ubic.basecode.util.ConfigUtils;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.util.*;

/**
 * Convenience class to access Gemma properties defined in a resource. Methods will look in Gemma.properties,
 * project.properties, build.properties and in the system properties.
 *
 * @author pavlidis
 * @see org.apache.commons.configuration2.CompositeConfiguration
 * @deprecated This has been replaced with Spring-based configuration {@link SettingsConfig} and usage of {@link org.springframework.beans.factory.annotation.Value}
 *             to inject configurations. You can use {@code @Value("${property}")} as replacement.
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
@Deprecated
public class Settings {

    /**
     * For web application, the key for the tracker ID in your configuration file. Tracker id for Google is something
     * like 'UA-12441-1'. In your Gemma.properties file add a line like:
     * <pre>
     * ga.tracker = UA_123456_1
     * </pre>
     */
    private static final String ANALYTICS_TRACKER_PROPERTY = "ga.tracker";

    /**
     * Name of the resource that is used to configure Gemma internally.
     */
    private static final String BUILTIN_CONFIGURATION = "project.properties";
    /**
     * Name of the resource containing defaults that the user can override (classpath)
     */
    private static final String DEFAULT_CONFIGURATION = "default.properties";
    /**
     * The name of the file users can use to configure Gemma.
     */
    private static final String USER_CONFIGURATION = "Gemma.properties";
    private static final CompositeConfiguration config;
    private static final Log log = LogFactory.getLog( Settings.class.getName() );

    static {
        config = new CompositeConfiguration();

        Settings.config.addConfiguration( new SystemConfiguration() );

        /*
         * the order matters - first come, first serve. Items added later do not overwrite items defined earlier. Thus
         * the user configuration has to be listed first. org.apache.commons.configuration.CompositeConfiguration
         * javadoc: "If you add Configuration1, and then Configuration2, any properties shared will mean that the value
         * defined by Configuration1 will be returned. If Configuration1 doesn't have the property, then Configuration2
         * will be checked"
         */

        boolean userConfigLoaded = false;

        String gemmaConfig = System.getProperty( "gemma.config" );
        if ( gemmaConfig != null ) {
            File f = Paths.get( gemmaConfig ).toFile();
            try {
                log.debug( "Loading user configuration from " + f.getAbsolutePath() + " since -Dgemma.config is defined." );
                Settings.config.addConfiguration( ConfigUtils.loadConfig( f ) );
                userConfigLoaded = true;
            } catch ( ConfigurationException e ) {
                throw new RuntimeException( f.getAbsolutePath() + " could not be loaded.", e );
            }
        }

        String catalinaBase;
        if ( !userConfigLoaded && ( catalinaBase = System.getenv( "CATALINA_BASE" ) ) != null ) {
            File f = Paths.get( catalinaBase, Settings.USER_CONFIGURATION ).toFile();
            if ( f.exists() ) {
                try {
                    log.debug( "Loading user configuration from " + f.getAbsolutePath() + " since $CATALINA_BASE is defined." );
                    Settings.config.addConfiguration( ConfigUtils.loadConfig( f ) );
                    userConfigLoaded = true;
                } catch ( ConfigurationException e ) {
                    throw new RuntimeException( f.getAbsolutePath() + " could not be loaded.", e );
                }
            }
        }

        File f = Paths.get( System.getProperty( "user.home" ), Settings.USER_CONFIGURATION ).toFile();
        if ( !userConfigLoaded && f.exists() ) {
            try {
                log.debug( "Loading user configuration from " + f.getAbsolutePath() + "." );
                Settings.config.addConfiguration( ConfigUtils.loadConfig( f ) );
                userConfigLoaded = true;
            } catch ( ConfigurationException e ) {
                throw new RuntimeException( f.getAbsolutePath() + " could not be loaded.", e );
            }
        }

        if ( !userConfigLoaded ) {
            throw new RuntimeException( Settings.USER_CONFIGURATION + " could not be loaded and no other user configuration were supplied." );
        }

        log.debug( "Loading default configurations from classpath." );

        try {
            // Default comes first.
            Settings.config.addConfiguration( ConfigUtils.loadClasspathConfig( Settings.DEFAULT_CONFIGURATION ) );
        } catch ( ConfigurationException e ) {
            throw new RuntimeException( "Default configuration could not be loaded.", e );
        }

        try {
            Settings.config.addConfiguration( ConfigUtils.loadClasspathConfig( Settings.BUILTIN_CONFIGURATION ) );
        } catch ( ConfigurationException e ) {
            throw new RuntimeException( "Extra built-in configuration could not be loaded.", e );
        }

        try {
            PropertiesConfiguration pc = ConfigUtils.loadClasspathConfig( "ubic/gemma/version.properties" );
            Settings.config.addConfiguration( pc );
        } catch ( ConfigurationException e ) {
            log.warn( "The ubic/gemma/version.properties resource was not found; run `mvn generate-resources -pl gemma-core` to generate it.", e );
        }

        // step through the result and do a final round of variable substitution.
        for ( Iterator<String> it = Settings.config.getKeys(); it.hasNext(); ) {
            String key = it.next();
            String property = Settings.config.getString( key );
            // This isn't doing anything if the variable is like "${foo}/bar"
            if ( property != null && property.startsWith( "${" ) && property.endsWith( "}" ) ) {
                String keyToSubstitute = property.substring( 2, property.length() - 1 );
                String valueToSubstitute = Settings.config.getString( keyToSubstitute );
                // log.debug( key + "=" + property + " -> " + valueToSubstitute );
                Settings.config.setProperty( key, valueToSubstitute );
            }
        }

        if ( Settings.log.isTraceEnabled() ) {
            Settings.log.trace( "********** Configuration details ***********" );
            ConfigurationUtils.dump( Settings.config, System.err );
            Settings.log.trace( "********** End of configuration details ***********" );
        }

    }

    /**
     * @return The local directory where files generated by analyses are stored. It will end in a file separator ("/" on
     * unix).
     */
    public static String getAnalysisStoragePath() {
        String val = Settings.getString( "gemma.analysis.dir" );
        assert val != null;
        if ( val.endsWith( File.separator ) )
            return val;
        return val + File.separator;
    }

    public static String getAnalyticsKey() {
        return Settings.getString( Settings.ANALYTICS_TRACKER_PROPERTY );
    }

    public static BigDecimal getBigDecimal( String key ) {
        return Settings.config.getBigDecimal( key );
    }

    public static BigDecimal getBigDecimal( String key, BigDecimal defaultValue ) {
        return Settings.config.getBigDecimal( key, defaultValue );
    }

    public static BigInteger getBigInteger( String key ) {
        return Settings.config.getBigInteger( key );
    }

    public static BigInteger getBigInteger( String key, BigInteger defaultValue ) {
        return Settings.config.getBigInteger( key, defaultValue );
    }

    public static boolean getBoolean( String key ) {
        try {
            return Settings.config.getBoolean( key );
        } catch ( NoSuchElementException nsee ) {
            Settings.log.info( key + " is not configured, returning default value of false" );
            return false;
        }
    }

    public static boolean getBoolean( String key, boolean defaultValue ) {
        return Settings.config.getBoolean( key, defaultValue );
    }

    public static Boolean getBoolean( String key, Boolean defaultValue ) {
        return Settings.config.getBoolean( key, defaultValue );
    }

    public static byte getByte( String key ) {
        try {
            return Settings.config.getByte( key );
        } catch ( NoSuchElementException nsee ) {
            Settings.log.info( key + " is not configured, returning default value of 1" );
            return 1;
        }
    }

    public static byte getByte( String key, byte defaultValue ) {
        return Settings.config.getByte( key, defaultValue );
    }

    public static Byte getByte( String key, Byte defaultValue ) {
        return Settings.config.getByte( key, defaultValue );
    }

    public static Configuration getConfiguration( int index ) {
        return Settings.config.getConfiguration( index );
    }

    public static String getDefaultSearchOperator() {
        return Settings.getString( "gemma.search.defaultOperator", "AND" );
    }

    public static double getDouble( String key ) {
        try {
            return Settings.config.getDouble( key );
        } catch ( NoSuchElementException nsee ) {
            Settings.log.info( key + " is not configured, returning default value of 1" );
            return 1;
        }
    }

    public static double getDouble( String key, double defaultValue ) {
        return Settings.config.getDouble( key, defaultValue );
    }

    public static Double getDouble( String key, Double defaultValue ) {
        return Settings.config.getDouble( key, defaultValue );
    }

    /**
     * @return The local directory where files downloaded/uploaded are stored. It will end in a file separator ("/" on
     * unix).
     */
    public static String getDownloadPath() {
        String val = Settings.getString( "gemma.download.path" );
        if ( val.endsWith( File.separator ) )
            return val;
        return val + File.separatorChar;
    }

    public static float getFloat( String key ) {
        try {
            return Settings.config.getFloat( key );
        } catch ( NoSuchElementException nsee ) {
            Settings.log.info( key + " is not configured, returning default value of 1" );
            return 1;
        }
    }

    public static float getFloat( String key, float defaultValue ) {
        return Settings.config.getFloat( key, defaultValue );
    }

    public static Float getFloat( String key, Float defaultValue ) {
        return Settings.config.getFloat( key, defaultValue );
    }

    /**
     * @return host url e.g. http://gemma.msl.ubc.ca
     */
    public static String getHostUrl() {
        String host = Settings.getString( "gemma.hosturl", "https://gemma.msl.ubc.ca" );
        if ( host.length() > 1 && host.endsWith( "/" ) ) {
            return host.substring( 0, host.length() - 1 );
        }
        return host;
    }

    public static Configuration getInMemoryConfiguration() {
        return Settings.config.getInMemoryConfiguration();
    }

    public static int getInt( String key ) {
        try {
            return Settings.config.getInt( key );
        } catch ( NoSuchElementException nsee ) {
            Settings.log.info( key + " is not configured, returning default value of 1" );
            return 1;
        }
    }

    public static int getInt( String key, int defaultValue ) {
        return Settings.config.getInt( key, defaultValue );
    }

    public static Integer getInteger( String key, Integer defaultValue ) {
        return Settings.config.getInteger( key, defaultValue );
    }

    public static Iterator<String> getKeys() {
        return Settings.config.getKeys();
    }

    public static Iterator<String> getKeys( String key ) {
        return Settings.config.getKeys( key );
    }

    public static List<?> getList( String key ) {

        try {
            return Settings.config.getList( key );

        } catch ( NoSuchElementException nsee ) {
            Settings.log.info( key + " is not configured, returning empty arrayList" );
            return new ArrayList<Object>();
        }
    }

    public static List<?> getList( String key, List<Object> defaultValue ) {
        return Settings.config.getList( key, defaultValue );
    }

    public static long getLong( String key ) {
        try {
            return Settings.config.getLong( key );
        } catch ( NoSuchElementException nsee ) {
            Settings.log.info( key + " is not configured, returning default value of 1" );
            return 1;
        }
    }

    public static long getLong( String key, long defaultValue ) {
        return Settings.config.getLong( key, defaultValue );
    }

    public static Long getLong( String key, Long defaultValue ) {
        return Settings.config.getLong( key, defaultValue );
    }

    public static int getNumberOfConfigurations() {
        return Settings.config.getNumberOfConfigurations();
    }

    public static Properties getProperties( String key ) {
        return Settings.config.getProperties( key );
    }

    public static Properties getProperties( String key, Properties defaults ) {
        return Settings.config.getProperties( key, defaults );
    }

    public static Object getProperty( String key ) {
        return Settings.config.getProperty( key );
    }

    public static short getShort( String key ) {
        try {
            return Settings.config.getShort( key );

        } catch ( NoSuchElementException nsee ) {
            Settings.log.info( key + " is not configured, returning default value of 1" );
            return 1;
        }
    }

    public static short getShort( String key, short defaultValue ) {
        return Settings.config.getShort( key, defaultValue );
    }

    public static Short getShort( String key, Short defaultValue ) {
        return Settings.config.getShort( key, defaultValue );
    }

    public static String getString( String key ) {
        try {
            return StringUtils.strip( Settings.config.getString( key ), "\"\'" );
        } catch ( NoSuchElementException nsee ) {
            Settings.log.info( key + " is not configured, returning empty string" );
            return "";
        }
    }

    public static String getString( String key, String defaultValue ) {
        return Settings.config.getString( key, defaultValue );
    }

    public static String[] getStringArray( String key ) {
        try {
            return Settings.config.getStringArray( key );
        } catch ( NoSuchElementException nsee ) {
            Settings.log.info( key + " is not configured, returning default value of null" );
            return null;
        }
    }

    /**
     * Set an environment/application variable programatically.
     *
     * @param key   key
     * @param value value
     */
    public static void setProperty( String key, Object value ) {
        Settings.config.setProperty( key, value );
    }

    public static Resource getResource( String key ) {
        String val = getString( key );
        if ( val == null ) {
            return null;
        } else if ( val.startsWith( "classpath:" ) ) {
            return new ClassPathResource( getString( key ) );
        } else if ( val.startsWith( "file:" ) || val.startsWith( "http:" ) || val.startsWith( "https:" ) || val.startsWith( "ftp:" ) ) {
            try {
                return new UrlResource( val );
            } catch ( MalformedURLException e ) {
                throw new RuntimeException( e );
            }
        } else {
            return new FileSystemResource( val );
        }
    }
}