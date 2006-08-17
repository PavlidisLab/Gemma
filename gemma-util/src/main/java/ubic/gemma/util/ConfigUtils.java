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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
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

    private static final String GEMMA_PROPERTIES = "Gemma.properties";

    private static CompositeConfiguration config;

    static {
        config = new CompositeConfiguration();
        config.addConfiguration( new SystemConfiguration() );

        try {
            config.addConfiguration( new PropertiesConfiguration( "build.properties" ) );
        } catch ( ConfigurationException e ) {
            // that's okay, but warn
            log.warn( "build.properties not found" );
        }

        try {
            config.addConfiguration( new PropertiesConfiguration( "project.properties" ) );
        } catch ( ConfigurationException e ) {
            // that's okay too.
        }

        try {
            config.addConfiguration( new PropertiesConfiguration( GEMMA_PROPERTIES ) );
        } catch ( ConfigurationException e ) {
            // that's okay, but warn
            log.warn( "Gemma.properties not found" ); // this is not here for all modules.
        }
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
        return config.getBoolean( key );
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
        return config.getByte( key );
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
        return config.getDouble( key );
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
        return config.getFloat( key );
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
        return config.getInt( key );
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
        return config.getList( key );
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
        return config.getLong( key );
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
        return config.getShort( key );
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
        return config.getString( key );
    }

    /**
     * @param key
     * @return
     * @see org.apache.commons.configuration.CompositeConfiguration#getStringArray(java.lang.String)
     */
    public static String[] getStringArray( String key ) {
        return config.getStringArray( key );
    }

}
