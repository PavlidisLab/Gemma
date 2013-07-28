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
import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.util.ConfigUtils;

/**
 * Convenience class to access Gemma properties defined in a resource. Methods will look in Gemma.properties,
 * project.properties, build.properties and in the system properties.
 * 
 * @author pavlidis
 * @version $Id$
 * @see org.apache.commons.configuration.CompositeConfiguration
 */
public class Settings {

    /**
     * For web application, the key for the tracker ID in your configuration file. Tracker id for Google is something
     * like 'UA-12441-1'. In your Gemma.properties file add a line like:
     * 
     * <pre>
     * ga.tracker = UA_123456_1
     * </pre>
     */
    private static final String ANALYTICS_TRACKER_PROPERTY = "ga.tracker";

    private static final String ANALYTICS_TRACKER_DOMAIN_PROPERTY = "ga.domain";

    /**
     * Name of the resource that is used to configure Gemma internally.
     */
    private static final String BUILTIN_CONFIGURATION = "project.properties";

    private static CompositeConfiguration config;

    /**
     * Name of the resource containing defaults that the user can override (classpath)
     */
    private static final String DEFAULT_CONFIGURATION = "default.properties";

    /**
     * Configuration parameter for lib directory, where jars shouldu be copied to make them available to the compute
     * grid (for example)
     */
    private static final String GEMMA_LIB_DIR = "gemma.lib.dir";

    private static final String REMOTE_TASKS_ENABLED_PROPERTY = "gemma.remoteTasks.enabled";

    private static Log log = LogFactory.getLog( Settings.class.getName() );

    private static final String QUARTZ_ENABLED_PROPERTY = "quartzOn";

    /**
     * The name of the file users can use to configure Gemma.
     */
    private static final String USER_CONFIGURATION = "Gemma.properties";

    static {

        config = new CompositeConfiguration();
        config.addConfiguration( new SystemConfiguration() );

        /*
         * the order matters - first come, first serve. Items added later do not overwrite items defined earlier. Thus
         * the user configuration has to be listed first. org.apache.commons.configuration.CompositeConfiguration
         * javadoc: "If you add Configuration1, and then Configuration2, any properties shared will mean that the value
         * defined by Configuration1 will be returned. If Configuration1 doesn't have the property, then Configuration2
         * will be checked"
         */

        try {
            PropertiesConfiguration pc = ConfigUtils.loadConfig( USER_CONFIGURATION );
            ConfigurationUtils.dump( pc, System.err );

            config.addConfiguration( pc );
        } catch ( ConfigurationException e ) {
            // hmm, this is pretty much required.
            log.warn( USER_CONFIGURATION + " not found" );
        }

        try {
            // Default comes first.
            PropertiesConfiguration pc = ConfigUtils.loadConfig( DEFAULT_CONFIGURATION );
            ConfigurationUtils.dump( pc, System.err );

            config.addConfiguration( pc );
        } catch ( ConfigurationException e ) {
            // hmm, this is pretty much required.
            log.warn( DEFAULT_CONFIGURATION + " not found" );
        }

        try {
            PropertiesConfiguration pc = ConfigUtils.loadClasspathConfig( BUILTIN_CONFIGURATION );
            ConfigurationUtils.dump( pc, System.err );
            config.addConfiguration( pc );
        } catch ( ConfigurationException e ) {
            throw new RuntimeException( "build-in configuration could not be loaded" );
        }

        try {
            String gemmaAppDataHome = config.getString( "gemma.appdata.home" );
            if ( StringUtils.isNotBlank( gemmaAppDataHome ) ) {
                PropertiesConfiguration pc = ConfigUtils.loadConfig( gemmaAppDataHome + File.separatorChar
                        + "local.properties" );
                config.addConfiguration( pc );

            }
        } catch ( ConfigurationException e ) {
            // that's okay
            // log.warn( "local.properties not found" );
        }

        try {
            PropertiesConfiguration pc = ConfigUtils.loadClasspathConfig( "version.properties" );
            ConfigurationUtils.dump( pc, System.err );

            config.addConfiguration( pc );
        } catch ( ConfigurationException e ) {
            log.debug( "version.properties not found" );
        }

        try {
            PropertiesConfiguration pc = ConfigUtils.loadConfig( "geommtx.properties" );
            ConfigurationUtils.dump( pc, System.err );

            config.addConfiguration( pc );
        } catch ( Exception e ) {
            // no big deal...hopefully.
        }

        // step through the result and do a final round of variable substitution. is this needed?
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
            ConfigurationUtils.dump( config, System.err );
            log.debug( "********** End of configuration details ***********" );
        }

    }

    public static String getAdminEmailAddress() {
        return getString( "gemma.admin.email" );
    }

    /**
     * @return The local directory where files generated by analyses are stored. It will end in a file separator ("/" on
     *         unix).
     */
    public static String getAnalysisStoragePath() {
        String val = getString( "gemma.analysis.dir" );
        if ( val.endsWith( File.separator ) ) return val;
        return val + File.separatorChar;
    }

    public static String getAnalyticsDomain() {
        return getString( ANALYTICS_TRACKER_DOMAIN_PROPERTY );
    }

    public static String getAnalyticsKey() {
        return getString( ANALYTICS_TRACKER_PROPERTY );
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
     * @return the configured base url (e.g., http://www.chibi.ubc.ca/Gemma/). It will always end in a slash.
     */
    public static String getBaseUrl() {
        String url = getString( "gemma.base.url", "http://www.chibi.ubc.ca/Gemma/" );
        if ( !url.endsWith( "/" ) ) {
            return url + "/";
        }
        return url;
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
     * @see org.apache.commons.configuration.AbstractConfiguration#getBigDecimal(java.lang.String, java.math.BigDecimal)
     */
    public static BigDecimal getBigDecimal( String key, BigDecimal defaultValue ) {
        return config.getBigDecimal( key, defaultValue );
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
     * @see org.apache.commons.configuration.AbstractConfiguration#getBigInteger(java.lang.String, java.math.BigInteger)
     */
    public static BigInteger getBigInteger( String key, BigInteger defaultValue ) {
        return config.getBigInteger( key, defaultValue );
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
     * @param index
     * @return
     * @see org.apache.commons.configuration.CompositeConfiguration#getConfiguration(int)
     */
    public static Configuration getConfiguration( int index ) {
        return config.getConfiguration( index );
    }

    /**
     * The default value given if none is defined is AND.
     * 
     * @return
     */
    public static String getDefaultSearchOperator() {
        return getString( "gemma.search.defaultOperator", "AND" );
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
     * @return The local directory where files downloaded/uploaded are stored. It will end in a file separator ("/" on
     *         unix).
     */
    public static String getDownloadPath() {
        String val = getString( "gemma.download.path" );
        if ( val.endsWith( File.separator ) ) return val;
        return val + File.separatorChar;
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
     * @return host url e.g. http://www.chibi.ubc.ca
     */
    public static String getHostUrl() {
        return getString( "gemma.hosturl", "http://www.chibi.ubc.ca/" );
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
     * @see org.apache.commons.configuration.AbstractConfiguration#getInt(java.lang.String, int)
     */
    public static int getInt( String key, int defaultValue ) {
        return config.getInt( key, defaultValue );
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
    public static Iterator<?> getKeys() {
        return config.getKeys();
    }

    /**
     * @param key
     * @return
     * @see org.apache.commons.configuration.CompositeConfiguration#getKeys(java.lang.String)
     */
    public static Iterator<?> getKeys( String key ) {
        return config.getKeys( key );
    }

    /**
     * The directory where the Gemma jar files will be available to other applications (e.g., the compute grid).
     * 
     * @return
     */
    public static String getLibDirectoryPath() {
        return getString( GEMMA_LIB_DIR );
    }

    /**
     * @param key
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getList(java.lang.String)
     */
    public static List<?> getList( String key ) {

        try {
            return config.getList( key );

        } catch ( NoSuchElementException nsee ) {
            log.info( key + " is not configured, returning empty arrayList" );
            return new ArrayList<Object>();
        }
    }

    /**
     * @param key
     * @param defaultValue
     * @return
     * @see org.apache.commons.configuration.CompositeConfiguration#getList(java.lang.String, java.util.List)
     */
    public static List<?> getList( String key, List<Object> defaultValue ) {
        return config.getList( key, defaultValue );
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
     * @return
     * @see org.apache.commons.configuration.CompositeConfiguration#getNumberOfConfigurations()
     */
    public static int getNumberOfConfigurations() {
        return config.getNumberOfConfigurations();
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
     * @see org.apache.commons.configuration.CompositeConfiguration#getProperty(java.lang.String)
     */
    public static Object getProperty( String key ) {
        return config.getProperty( key );
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
     * @see org.apache.commons.configuration.AbstractConfiguration#getString(java.lang.String)
     */
    public static String getString( String key ) {
        try {
            return StringUtils.strip( config.getString( key ), "\"\'" );
        } catch ( NoSuchElementException nsee ) {
            log.info( key + " is not configured, returning empty string" );
            return "";
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

    public static String getTaskControlQueue() {
        return getString( "gemma.remoteTasks.controlQueue" );
    }

    public static String getTaskLifeCycleQueuePrefix() {
        return getString( "gemma.remoteTasks.lifeCycleQueuePrefix" );
    }

    public static String getTaskProgressQueuePrefix() {
        return getString( "gemma.remoteTasks.progressUpdatesQueuePrefix" );
    }

    public static String getTaskResultQueuePrefix() {
        return getString( "gemma.remoteTasks.resultQueuePrefix" );
    }

    public static String getTaskSubmissionQueue() {
        return getString( "gemma.remoteTasks.taskSubmissionQueue" );
    }

    public static boolean isRemoteTasksEnabled() {
        return getBoolean( REMOTE_TASKS_ENABLED_PROPERTY, false );
    }

    /**
     * @return true if the scheduler (e.g. Quartz for cron-style tasks) is enabled by the user's configuration
     */
    public static boolean isSchedulerEnabled() {
        return getBoolean( QUARTZ_ENABLED_PROPERTY, false );
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

}
