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

import org.apache.commons.configuration.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.basecode.util.ConfigUtils;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * Convenience class to access Gemma properties defined in a resource. Methods will look in Gemma.properties,
 * project.properties, build.properties and in the system properties.
 *
 * @author pavlidis
 * @see org.apache.commons.configuration.CompositeConfiguration
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class Settings {

    /**
     * For web application, the key for the tracker ID in your configuration file. Tracker id for Google is something
     * like 'UA-12441-1'. In your Gemma.properties file add a line like:
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
    /**
     * Name of the resource containing defaults that the user can override (classpath)
     */
    private static final String DEFAULT_CONFIGURATION = "default.properties";
    /**
     * Configuration parameter for lib directory, where jars shouldu be copied to make them available to the compute
     * grid (for example)
     */

    private static final String REMOTE_TASKS_ENABLED_PROPERTY = "gemma.remoteTasks.enabled";
    private static final String QUARTZ_ENABLED_PROPERTY = "quartzOn";
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

        try {
            PropertiesConfiguration pc = ConfigUtils.loadConfig( Settings.USER_CONFIGURATION );

            Settings.config.addConfiguration( pc );
        } catch ( ConfigurationException e ) {
            Settings.log.warn( Settings.USER_CONFIGURATION + " not found" );
        }

        try {
            // Default comes first.
            PropertiesConfiguration pc = ConfigUtils.loadClasspathConfig( Settings.DEFAULT_CONFIGURATION );
            // ConfigurationUtils.dump( pc, System.err );
            Settings.config.addConfiguration( pc );
        } catch ( ConfigurationException e ) {
            throw new RuntimeException( "Default configuration could not be loaded: " + e.getMessage(), e );
        }

        try {
            PropertiesConfiguration pc = ConfigUtils.loadClasspathConfig( Settings.BUILTIN_CONFIGURATION );
            Settings.config.addConfiguration( pc );
        } catch ( ConfigurationException e ) {
            throw new RuntimeException( "Extra built-in configuration could not be loaded: " + e.getMessage(), e );
        }

        try {
            String gemmaAppDataHome = Settings.config.getString( "gemma.appdata.home" );
            if ( StringUtils.isNotBlank( gemmaAppDataHome ) ) {
                PropertiesConfiguration pc = ConfigUtils
                        .loadConfig( gemmaAppDataHome + File.separatorChar + "local.properties" );
                Settings.config.addConfiguration( pc );

            }
        } catch ( ConfigurationException e ) {
            // that's okay
        }

        try {
            PropertiesConfiguration pc = ConfigUtils.loadClasspathConfig( "version.properties" );

            Settings.config.addConfiguration( pc );
        } catch ( ConfigurationException e ) {
            Settings.log.debug( "version.properties not found" );
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

        if ( Settings.log.isDebugEnabled() ) {
            Settings.log.debug( "********** Configuration details ***********" );
            ConfigurationUtils.dump( Settings.config, System.err );
            Settings.log.debug( "********** End of configuration details ***********" );
        }

    }

    public static String getAdminEmailAddress() {
        return Settings.getString( "gemma.admin.email" );
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

    public static String getAnalyticsDomain() {
        return Settings.getString( Settings.ANALYTICS_TRACKER_DOMAIN_PROPERTY );
    }

    public static String getAnalyticsKey() {
        return Settings.getString( Settings.ANALYTICS_TRACKER_PROPERTY );
    }

    /**
     * Attempt to get the version information about the application.
     */
    public static String getAppVersion() {
        return Settings.getString( "gemma.version" );
    }

    /**
     * @see org.apache.commons.configuration.AbstractConfiguration#getBigDecimal(java.lang.String)
     */
    public static BigDecimal getBigDecimal( String key ) {
        return Settings.config.getBigDecimal( key );
    }

    /**
     * @see org.apache.commons.configuration.AbstractConfiguration#getBigDecimal(java.lang.String, java.math.BigDecimal)
     */
    public static BigDecimal getBigDecimal( String key, BigDecimal defaultValue ) {
        return Settings.config.getBigDecimal( key, defaultValue );
    }

    /**
     * @see org.apache.commons.configuration.AbstractConfiguration#getBigInteger(java.lang.String)
     */
    public static BigInteger getBigInteger( String key ) {
        return Settings.config.getBigInteger( key );
    }

    /**
     * @see org.apache.commons.configuration.AbstractConfiguration#getBigInteger(java.lang.String, java.math.BigInteger)
     */
    public static BigInteger getBigInteger( String key, BigInteger defaultValue ) {
        return Settings.config.getBigInteger( key, defaultValue );
    }

    /**
     * @see org.apache.commons.configuration.AbstractConfiguration#getBoolean(java.lang.String)
     */
    public static boolean getBoolean( String key ) {
        try {
            return Settings.config.getBoolean( key );
        } catch ( NoSuchElementException nsee ) {
            Settings.log.info( key + " is not configured, returning default value of false" );
            return false;
        }
    }

    /**
     * @see org.apache.commons.configuration.AbstractConfiguration#getBoolean(java.lang.String, boolean)
     */
    public static boolean getBoolean( String key, boolean defaultValue ) {
        return Settings.config.getBoolean( key, defaultValue );
    }

    /**
     * @see org.apache.commons.configuration.AbstractConfiguration#getBoolean(java.lang.String, java.lang.Boolean)
     */
    public static Boolean getBoolean( String key, Boolean defaultValue ) {
        return Settings.config.getBoolean( key, defaultValue );
    }

    /**
     * @see org.apache.commons.configuration.AbstractConfiguration#getByte(java.lang.String)
     */
    public static byte getByte( String key ) {
        try {
            return Settings.config.getByte( key );
        } catch ( NoSuchElementException nsee ) {
            Settings.log.info( key + " is not configured, returning default value of 1" );
            return 1;
        }
    }

    /**
     * @see org.apache.commons.configuration.AbstractConfiguration#getByte(java.lang.String, byte)
     */
    public static byte getByte( String key, byte defaultValue ) {
        return Settings.config.getByte( key, defaultValue );
    }

    /**
     * @see org.apache.commons.configuration.AbstractConfiguration#getByte(java.lang.String, java.lang.Byte)
     */
    public static Byte getByte( String key, Byte defaultValue ) {
        return Settings.config.getByte( key, defaultValue );
    }

    /**
     * @see org.apache.commons.configuration.CompositeConfiguration#getConfiguration(int)
     */
    public static Configuration getConfiguration( int index ) {
        return Settings.config.getConfiguration( index );
    }

    /**
     * The default value given if none is defined is AND.
     */
    public static String getDefaultSearchOperator() {
        return Settings.getString( "gemma.search.defaultOperator", "AND" );
    }

    /**
     * @see org.apache.commons.configuration.AbstractConfiguration#getDouble(java.lang.String)
     */
    public static double getDouble( String key ) {
        try {
            return Settings.config.getDouble( key );
        } catch ( NoSuchElementException nsee ) {
            Settings.log.info( key + " is not configured, returning default value of 1" );
            return 1;
        }
    }

    /**
     * @see org.apache.commons.configuration.AbstractConfiguration#getDouble(java.lang.String, double)
     */
    public static double getDouble( String key, double defaultValue ) {
        return Settings.config.getDouble( key, defaultValue );
    }

    /**
     * @see org.apache.commons.configuration.AbstractConfiguration#getDouble(java.lang.String, java.lang.Double)
     */
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

    /**
     * @see org.apache.commons.configuration.AbstractConfiguration#getFloat(java.lang.String)
     */
    public static float getFloat( String key ) {
        try {
            return Settings.config.getFloat( key );
        } catch ( NoSuchElementException nsee ) {
            Settings.log.info( key + " is not configured, returning default value of 1" );
            return 1;
        }
    }

    /**
     * @see org.apache.commons.configuration.AbstractConfiguration#getFloat(java.lang.String, float)
     */
    public static float getFloat( String key, float defaultValue ) {
        return Settings.config.getFloat( key, defaultValue );
    }

    /**
     * @see org.apache.commons.configuration.AbstractConfiguration#getFloat(java.lang.String, java.lang.Float)
     */
    public static Float getFloat( String key, Float defaultValue ) {
        return Settings.config.getFloat( key, defaultValue );
    }

    /**
     * @return host url e.g. http://gemma.msl.ubc.ca
     */
    public static String getHostUrl() {
        String host = Settings.getString( "gemma.hosturl", "http://gemma.msl.ubc.ca" );
        if ( host.length() > 1 && host.endsWith( "/" ) ) {
            return host.substring( 0, host.length() - 1 );
        }
        return host;
    }

    /**
     * @return root context e.g. /Gemma
     */
    public static String getRootContext() {
        String ctx = Settings.getString( "gemma.rootcontext", "" );
        if ( ctx.isEmpty() || ctx.equals( "/" ) ) {
            return "";
        }
        if ( !ctx.startsWith( "/" ) ) {
            ctx = "/" + ctx;
        }
        if ( ctx.length() > 1 && ctx.endsWith( "/" ) ) {
            return ctx.substring( 0, ctx.length() - 1 );
        }
        return ctx;
    }

    /**
     * @return the configured base url (e.g., http://gemma.msl.ubc.ca/Gemma/). It will always end in a slash.
     */
    public static String getBaseUrl() {
        String url = Settings.getString( "gemma.baseurl", Settings.getHostUrl() + Settings.getRootContext() + "/" );
        if ( !url.endsWith( "/" ) ) {
            return url + "/";
        }
        return url;
    }

    /**
     * @see org.apache.commons.configuration.CompositeConfiguration#getInMemoryConfiguration()
     */
    public static Configuration getInMemoryConfiguration() {
        return Settings.config.getInMemoryConfiguration();
    }

    /**
     * @see org.apache.commons.configuration.AbstractConfiguration#getInt(java.lang.String)
     */
    public static int getInt( String key ) {
        try {
            return Settings.config.getInt( key );
        } catch ( NoSuchElementException nsee ) {
            Settings.log.info( key + " is not configured, returning default value of 1" );
            return 1;
        }
    }

    /**
     * @see org.apache.commons.configuration.AbstractConfiguration#getInt(java.lang.String, int)
     */
    public static int getInt( String key, int defaultValue ) {
        return Settings.config.getInt( key, defaultValue );
    }

    /**
     * @see org.apache.commons.configuration.AbstractConfiguration#getInteger(java.lang.String, java.lang.Integer)
     */
    public static Integer getInteger( String key, Integer defaultValue ) {
        return Settings.config.getInteger( key, defaultValue );
    }

    /**
     * @see org.apache.commons.configuration.CompositeConfiguration#getKeys()
     */
    public static Iterator<String> getKeys() {
        return Settings.config.getKeys();
    }

    /**
     * @see org.apache.commons.configuration.CompositeConfiguration#getKeys(java.lang.String)
     */
    public static Iterator<String> getKeys( String key ) {
        return Settings.config.getKeys( key );
    }

    /**
     * @see org.apache.commons.configuration.AbstractConfiguration#getList(java.lang.String)
     */
    public static List<?> getList( String key ) {

        try {
            return Settings.config.getList( key );

        } catch ( NoSuchElementException nsee ) {
            Settings.log.info( key + " is not configured, returning empty arrayList" );
            return new ArrayList<Object>();
        }
    }

    /**
     * @see org.apache.commons.configuration.CompositeConfiguration#getList(java.lang.String, java.util.List)
     */
    public static List<?> getList( String key, List<Object> defaultValue ) {
        return Settings.config.getList( key, defaultValue );
    }

    /**
     * @see org.apache.commons.configuration.AbstractConfiguration#getLong(java.lang.String)
     */
    public static long getLong( String key ) {
        try {
            return Settings.config.getLong( key );
        } catch ( NoSuchElementException nsee ) {
            Settings.log.info( key + " is not configured, returning default value of 1" );
            return 1;
        }
    }

    /**
     * @see org.apache.commons.configuration.AbstractConfiguration#getLong(java.lang.String, long)
     */
    public static long getLong( String key, long defaultValue ) {
        return Settings.config.getLong( key, defaultValue );
    }

    /**
     * @see org.apache.commons.configuration.AbstractConfiguration#getLong(java.lang.String, java.lang.Long)
     */
    public static Long getLong( String key, Long defaultValue ) {
        return Settings.config.getLong( key, defaultValue );
    }

    /**
     * @see org.apache.commons.configuration.CompositeConfiguration#getNumberOfConfigurations()
     */
    public static int getNumberOfConfigurations() {
        return Settings.config.getNumberOfConfigurations();
    }

    /**
     * @see org.apache.commons.configuration.AbstractConfiguration#getProperties(java.lang.String)
     */
    public static Properties getProperties( String key ) {
        return Settings.config.getProperties( key );
    }

    /**
     * @see org.apache.commons.configuration.AbstractConfiguration#getProperties(java.lang.String, java.util.Properties)
     */
    public static Properties getProperties( String key, Properties defaults ) {
        return Settings.config.getProperties( key, defaults );
    }

    /**
     * @see org.apache.commons.configuration.CompositeConfiguration#getProperty(java.lang.String)
     */
    public static Object getProperty( String key ) {
        return Settings.config.getProperty( key );
    }

    /**
     * @see org.apache.commons.configuration.AbstractConfiguration#getShort(java.lang.String)
     */
    public static short getShort( String key ) {
        try {
            return Settings.config.getShort( key );

        } catch ( NoSuchElementException nsee ) {
            Settings.log.info( key + " is not configured, returning default value of 1" );
            return 1;
        }
    }

    /**
     * @see org.apache.commons.configuration.AbstractConfiguration#getShort(java.lang.String, short)
     */
    public static short getShort( String key, short defaultValue ) {
        return Settings.config.getShort( key, defaultValue );
    }

    /**
     * @see org.apache.commons.configuration.AbstractConfiguration#getShort(java.lang.String, java.lang.Short)
     */
    public static Short getShort( String key, Short defaultValue ) {
        return Settings.config.getShort( key, defaultValue );
    }

    /**
     * @see org.apache.commons.configuration.AbstractConfiguration#getString(java.lang.String)
     */
    public static String getString( String key ) {
        try {
            return StringUtils.strip( Settings.config.getString( key ), "\"\'" );
        } catch ( NoSuchElementException nsee ) {
            Settings.log.info( key + " is not configured, returning empty string" );
            return "";
        }
    }

    /**
     * @see org.apache.commons.configuration.AbstractConfiguration#getString(java.lang.String, java.lang.String)
     */
    public static String getString( String key, String defaultValue ) {
        return Settings.config.getString( key, defaultValue );
    }

    /**
     * @see org.apache.commons.configuration.CompositeConfiguration#getStringArray(java.lang.String)
     */
    public static String[] getStringArray( String key ) {
        try {
            return Settings.config.getStringArray( key );
        } catch ( NoSuchElementException nsee ) {
            Settings.log.info( key + " is not configured, returning default value of null" );
            return null;
        }
    }

    public static String getTaskControlQueue() {
        return Settings.getString( "gemma.remoteTasks.controlQueue" );
    }

    public static String getTaskLifeCycleQueuePrefix() {
        return Settings.getString( "gemma.remoteTasks.lifeCycleQueuePrefix" );
    }

    public static String getTaskProgressQueuePrefix() {
        return Settings.getString( "gemma.remoteTasks.progressUpdatesQueuePrefix" );
    }

    public static String getTaskResultQueuePrefix() {
        return Settings.getString( "gemma.remoteTasks.resultQueuePrefix" );
    }

    public static String getTaskSubmissionQueue() {
        return Settings.getString( "gemma.remoteTasks.taskSubmissionQueue" );
    }

    public static boolean isRemoteTasksEnabled() {
        return Settings.getBoolean( Settings.REMOTE_TASKS_ENABLED_PROPERTY, false );
    }

    /**
     * @return true if the scheduler (e.g. Quartz for cron-style tasks) is enabled by the user's configuration
     */
    public static boolean isSchedulerEnabled() {
        return Settings.getBoolean( Settings.QUARTZ_ENABLED_PROPERTY, false );
    }

    /**
     * Set an environment/application variable programatically.
     */
    public static void setProperty( String key, Object value ) {
        Settings.config.setProperty( key, value );
    }

}