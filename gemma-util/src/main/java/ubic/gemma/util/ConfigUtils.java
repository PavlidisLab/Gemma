/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.util;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Convenience class to access Gemma properties defined in a resource. Methods will look in Gemma.properties,
 * project.properties, build.properties and in the system properties.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ConfigUtils {

    private static Log log = LogFactory.getLog( ConfigUtils.class.getName() );

    /**
     * The name of the file users can use to configure Gemma.
     */
    private static final String USER_CONFIGURATION = "Gemma.properties";

    /**
     * Name of the resource containing defaults that the user can override (classpath)
     */
    private static final String DEFAULT_CONFIGURATION = "default.properties";

    /**
     * Name of the resource that is used to configure Gemma internally.
     */
    private static final String BUILTIN_CONFIGURATION = "project.properties";

    private static CompositeConfiguration config;

    static {

        config = new CompositeConfiguration();
        config.addConfiguration( new SystemConfiguration() );

        // the order matters - first come, first serve.
        try {
            // Default comes first.
            config.addConfiguration( new PropertiesConfiguration( DEFAULT_CONFIGURATION ) );
        } catch ( ConfigurationException e ) {
            // hmm, this is pretty much required.
            log.warn( DEFAULT_CONFIGURATION + " not found" );
        }

        try {
            config.addConfiguration( new PropertiesConfiguration( USER_CONFIGURATION ) );
        } catch ( ConfigurationException e ) {
            // hmm, this is pretty much required.
            log.warn( USER_CONFIGURATION + " not found" );
        }

        try {
            config.addConfiguration( new PropertiesConfiguration( BUILTIN_CONFIGURATION ) );
        } catch ( ConfigurationException e ) {
            // that's okay, but warn
            log.warn( BUILTIN_CONFIGURATION + " not found" );
        }

        try {
            String gemmaAppDataHome = config.getString( "gemma.appdata.home" );
            if ( StringUtils.isNotBlank( gemmaAppDataHome ) ) {
                config.addConfiguration( new PropertiesConfiguration( gemmaAppDataHome + File.separatorChar
                        + "local.properties" ) );
            }
        } catch ( ConfigurationException e ) {
            // that's okay
            // log.warn( "local.properties not found" );
        }

        try {
            config.addConfiguration( new PropertiesConfiguration( "version.properties" ) );
        } catch ( ConfigurationException e ) {
            // that's okay too.
            log.warn( "version.properties not found" );
        }

        // step through the result and do a final round of variable substitution
        for ( Iterator<String> it = config.getKeys(); it.hasNext(); ) {
            String key = it.next();
            String property = config.getString( key );
            if ( property != null && property.startsWith( "${" ) && property.endsWith( "}" ) ) {
                String keyToSubstitute = property.substring( 2, property.length() - 1 );
                String valueToSubstitute = config.getString( keyToSubstitute );
                log.debug( key + "=" + property + " -> " + valueToSubstitute );
                config.setProperty( key, valueToSubstitute );
            }
        }

        if ( log.isDebugEnabled() ) {
            log.debug( "********** Configuration details ***********" );
            for ( Iterator it = config.getKeys(); it.hasNext(); ) {
                String key = ( String ) it.next();
                Object prop = config.getProperty( key );
                log.debug( key + " = " + prop );
            }
            log.debug( "********** End of configuration details ***********" );
        }

    }

    /**
     * Attempt to get the version information about the application.
     * 
     * @return
     */
    public static String getAppVersion() {
        return getString( "gemma.version" );
    }

    /**
     * @return The local directory where files downloaded/uploaded are stored. It will end in a file separator ("/" on
     *         unix).
     */
    public static String getDownloadPath() {
        String val = getString( "gemma.download.path" );
        if ( val.endsWith( File.separator ) ) return val;
        return val + File.separatorChar;
    }

    /**
     * @return The local directory where files generated by analyses are stored. It will end in a file separator ("/" on
     *         unix).
     */
    public static String getAnalysisStoragePath() {
        String val = getString( "analysis.dir" );
        if ( val.endsWith( File.separator ) ) return val;
        return val + File.separatorChar;
    }

    /**
     * @param key
     * @param defaultValue
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getBigDecimal(java.lang.String, java.math.BigDecimal)
     */
    public static BigDecimal getBigDecimal( String key, BigDecimal defaultValue ) {
        return config.getBigDecimal( key, defaultValue );
    }

    /**
     * @param key
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getBigDecimal(java.lang.String)
     */
    public static BigDecimal getBigDecimal( String key ) {
        return config.getBigDecimal( key );
    }

    /**
     * @param key
     * @param defaultValue
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getBigInteger(java.lang.String, java.math.BigInteger)
     */
    public static BigInteger getBigInteger( String key, BigInteger defaultValue ) {
        return config.getBigInteger( key, defaultValue );
    }

    /**
     * @param key
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getBigInteger(java.lang.String)
     */
    public static BigInteger getBigInteger( String key ) {
        return config.getBigInteger( key );
    }

    /**
     * @param key
     * @param defaultValue
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getBoolean(java.lang.String, boolean)
     */
    public static boolean getBoolean( String key, boolean defaultValue ) {
        return config.getBoolean( key, defaultValue );
    }

    /**
     * @param key
     * @param defaultValue
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getBoolean(java.lang.String, java.lang.Boolean)
     */
    public static Boolean getBoolean( String key, Boolean defaultValue ) {
        return config.getBoolean( key, defaultValue );
    }

    /**
     * @param key
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getBoolean(java.lang.String)
     */
    public static boolean getBoolean( String key ) {
        try {
            return config.getBoolean( key );
        } catch ( NoSuchElementException nsee ) {
            log.info( key + " is not configured, returning default value of false" );
            return false;
        }

    }

    /**
     * @param key
     * @param defaultValue
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getByte(java.lang.String, byte)
     */
    public static byte getByte( String key, byte defaultValue ) {
        return config.getByte( key, defaultValue );
    }

    /**
     * @param key
     * @param defaultValue
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getByte(java.lang.String, java.lang.Byte)
     */
    public static Byte getByte( String key, Byte defaultValue ) {
        return config.getByte( key, defaultValue );
    }

    /**
     * @param key
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getByte(java.lang.String)
     */
    public static byte getByte( String key ) {
        try {
            return config.getByte( key );
        } catch ( NoSuchElementException nsee ) {
            log.info( key + " is not configured, returning default value of 1" );
            return 1;
        }
    }

    /**
     * @param index
     * @return
     * @see org.apache.commons.configuration.CompositeConfiguration#getConfiguration(int)
     */
    public static Configuration getConfiguration( int index ) {
        return config.getConfiguration( index );
    }

    /**
     * @param key
     * @param defaultValue
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getDouble(java.lang.String, double)
     */
    public static double getDouble( String key, double defaultValue ) {
        return config.getDouble( key, defaultValue );
    }

    /**
     * @param key
     * @param defaultValue
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getDouble(java.lang.String, java.lang.Double)
     */
    public static Double getDouble( String key, Double defaultValue ) {
        return config.getDouble( key, defaultValue );
    }

    /**
     * @param key
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getDouble(java.lang.String)
     */
    public static double getDouble( String key ) {
        try {
            return config.getDouble( key );
        } catch ( NoSuchElementException nsee ) {
            log.info( key + " is not configured, returning default value of 1" );
            return 1;
        }
    }

    /**
     * @param key
     * @param defaultValue
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getFloat(java.lang.String, float)
     */
    public static float getFloat( String key, float defaultValue ) {
        return config.getFloat( key, defaultValue );
    }

    /**
     * @param key
     * @param defaultValue
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getFloat(java.lang.String, java.lang.Float)
     */
    public static Float getFloat( String key, Float defaultValue ) {
        return config.getFloat( key, defaultValue );
    }

    /**
     * @param key
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getFloat(java.lang.String)
     */
    public static float getFloat( String key ) {
        try {
            return config.getFloat( key );
        } catch ( NoSuchElementException nsee ) {
            log.info( key + " is not configured, returning default value of 1" );
            return 1;
        }
    }

    /**
     * @return
     * @see org.apache.commons.configuration.CompositeConfiguration#getInMemoryConfiguration()
     */
    public static Configuration getInMemoryConfiguration() {
        return config.getInMemoryConfiguration();
    }

    /**
     * @param key
     * @param defaultValue
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getInt(java.lang.String, int)
     */
    public static int getInt( String key, int defaultValue ) {
        return config.getInt( key, defaultValue );
    }

    /**
     * @param key
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getInt(java.lang.String)
     */
    public static int getInt( String key ) {
        try {
            return config.getInt( key );
        } catch ( NoSuchElementException nsee ) {
            log.info( key + " is not configured, returning default value of 1" );
            return 1;
        }
    }

    /**
     * @param key
     * @param defaultValue
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getInteger(java.lang.String, java.lang.Integer)
     */
    public static Integer getInteger( String key, Integer defaultValue ) {
        return config.getInteger( key, defaultValue );
    }

    /**
     * @return
     * @see org.apache.commons.configuration.CompositeConfiguration#getKeys()
     */
    public static Iterator getKeys() {
        return config.getKeys();
    }

    /**
     * @param key
     * @return
     * @see org.apache.commons.configuration.CompositeConfiguration#getKeys(java.lang.String)
     */
    public static Iterator getKeys( String key ) {
        return config.getKeys( key );
    }

    /**
     * @param key
     * @param defaultValue
     * @return
     * @see org.apache.commons.configuration.CompositeConfiguration#getList(java.lang.String, java.util.List)
     */
    public static List getList( String key, List defaultValue ) {
        return config.getList( key, defaultValue );
    }

    /**
     * @param key
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getList(java.lang.String)
     */
    public static List getList( String key ) {

        try {
            return config.getList( key );

        } catch ( NoSuchElementException nsee ) {
            log.info( key + " is not configured, returning empty arrayList" );
            return new ArrayList();
        }
    }

    /**
     * @param key
     * @param defaultValue
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getLong(java.lang.String, long)
     */
    public static long getLong( String key, long defaultValue ) {
        return config.getLong( key, defaultValue );
    }

    /**
     * @param key
     * @param defaultValue
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getLong(java.lang.String, java.lang.Long)
     */
    public static Long getLong( String key, Long defaultValue ) {
        return config.getLong( key, defaultValue );
    }

    /**
     * @param key
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getLong(java.lang.String)
     */
    public static long getLong( String key ) {
        try {
            return config.getLong( key );
        } catch ( NoSuchElementException nsee ) {
            log.info( key + " is not configured, returning default value of 1" );
            return 1;
        }
    }

    /**
     * @return
     * @see org.apache.commons.configuration.CompositeConfiguration#getNumberOfConfigurations()
     */
    public static int getNumberOfConfigurations() {
        return config.getNumberOfConfigurations();
    }

    /**
     * @param key
     * @param defaults
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getProperties(java.lang.String, java.util.Properties)
     */
    public static Properties getProperties( String key, Properties defaults ) {
        return config.getProperties( key, defaults );
    }

    /**
     * @param key
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getProperties(java.lang.String)
     */
    public static Properties getProperties( String key ) {
        return config.getProperties( key );
    }

    /**
     * @param key
     * @return
     * @see org.apache.commons.configuration.CompositeConfiguration#getProperty(java.lang.String)
     */
    public static Object getProperty( String key ) {
        return config.getProperty( key );
    }

    /**
     * @param key
     * @param defaultValue
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getShort(java.lang.String, short)
     */
    public static short getShort( String key, short defaultValue ) {
        return config.getShort( key, defaultValue );
    }

    /**
     * @param key
     * @param defaultValue
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getShort(java.lang.String, java.lang.Short)
     */
    public static Short getShort( String key, Short defaultValue ) {
        return config.getShort( key, defaultValue );
    }

    /**
     * @param key
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getShort(java.lang.String)
     */
    public static short getShort( String key ) {
        try {
            return config.getShort( key );

        } catch ( NoSuchElementException nsee ) {
            log.info( key + " is not configured, returning default value of 1" );
            return 1;
        }
    }

    /**
     * @param key
     * @param defaultValue
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getString(java.lang.String, java.lang.String)
     */
    public static String getString( String key, String defaultValue ) {
        return config.getString( key, defaultValue );
    }

    /**
     * @param key
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getString(java.lang.String)
     */
    public static String getString( String key ) {
        try {
            return config.getString( key );
        } catch ( NoSuchElementException nsee ) {
            log.info( key + " is not configured, returning empty string" );
            return "";
        }
    }

    /**
     * @param key
     * @return
     * @see org.apache.commons.configuration.CompositeConfiguration#getStringArray(java.lang.String)
     */
    public static String[] getStringArray( String key ) {
        try {
            return config.getStringArray( key );
        } catch ( NoSuchElementException nsee ) {
            log.info( key + " is not configured, returning default value of null" );
            return null;
        }
    }

    /**
     * Set an environment/application variable programatically.
     * 
     * @param enablePropertyName
     * @param b
     */
    public static void setProperty( String key, Object value ) {
        config.setProperty( key, value );
    }

    public static String getAdminEmailAddress() {
        return getString( "gemma.admin.email" );
    }

}
