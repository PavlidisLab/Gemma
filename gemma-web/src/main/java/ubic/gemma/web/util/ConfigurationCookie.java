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
package ubic.gemma.web.util;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.Cookie;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * Cookie class that also presents a commons configuration interface.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ConfigurationCookie extends Cookie {

    /**
     * Used to delimit what would normally be separate lines in a properties file.
     */
    private static final String PROPERTY_DELIMITER = "@@";

    PropertiesConfiguration configuration;

    /**
     * Used when loading cookies from the user.
     * 
     * @param cookie Must have been originally created as a ConfigurationCookie (or look just like one).
     * @throws ConfigurationException if the cookie cannot be converted.
     */
    public ConfigurationCookie( Cookie cookie ) throws ConfigurationException {
        super( cookie.getName(), cookie.getValue() );

        // turn the value into properties.
        StringReader reader = new StringReader( this.getValue().replaceAll( PROPERTY_DELIMITER, "\n" ) );
        configuration = new PropertiesConfiguration();
        configuration.load( reader );
    }

    /**
     * Create a cookie with the given name. The value should be populated using the setProperty() or addProperty()
     * methods.
     * 
     * @param name
     */
    public ConfigurationCookie( String name ) {
        super( name, "" );
        configuration = new PropertiesConfiguration();
    }

    /**
     * Don't use this method if you can help it! Use setProperty instead.
     */
    @Override
    public final void setValue( String value ) {
        super.setValue( value );
    }

    /**
     * @param key
     * @param value
     * @see org.apache.commons.configuration.Configuration#setProperty(java.lang.String, java.lang.Object)
     */
    public void setProperty( String key, Object value ) {
        this.configuration.setProperty( key, value );

        // Rewrite the value.
        StringWriter writer = new StringWriter();
        try {
            configuration.save( writer );
        } catch ( ConfigurationException e ) {
            throw new RuntimeException( e );
        }
        setValue( PROPERTY_DELIMITER + writer.toString().replaceAll( "\n", PROPERTY_DELIMITER ) );
    }

    /**
     * @param key
     * @param value
     * @see org.apache.commons.configuration.AbstractConfiguration#addProperty(java.lang.String, java.lang.Object)
     */
    public void addProperty( String key, Object value ) {
        this.configuration.addProperty( key, value );
        // Rewrite the value.
        StringWriter writer = new StringWriter();
        try {
            configuration.save( writer );
        } catch ( ConfigurationException e ) {
            throw new RuntimeException( e );
        }
        setValue( writer.toString().replaceAll( "\n", PROPERTY_DELIMITER ) );
    }

    /**
     * @param key
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getBoolean(java.lang.String)
     */
    public boolean getBoolean( String key ) {
        return this.configuration.getBoolean( key );
    }

    /**
     * @param key
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getDouble(java.lang.String)
     */
    public double getDouble( String key ) {
        return this.configuration.getDouble( key );
    }

    /**
     * @return
     * @see org.apache.commons.configuration.AbstractFileConfiguration#getFile()
     */
    public File getFile() {
        return this.configuration.getFile();
    }

    /**
     * @return
     * @see org.apache.commons.configuration.AbstractFileConfiguration#getFileName()
     */
    public String getFileName() {
        return this.configuration.getFileName();
    }

    /**
     * @param key
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getInt(java.lang.String)
     */
    public int getInt( String key ) {
        return this.configuration.getInt( key );
    }

    /**
     * @param key
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getList(java.lang.String)
     */
    public List<?> getList( String key ) {
        return this.configuration.getList( key );
    }

    /**
     * @param key
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getLong(java.lang.String)
     */
    public long getLong( String key ) {
        return this.configuration.getLong( key );
    }

    /**
     * @param key
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getProperties(java.lang.String)
     */
    public Properties getProperties( String key ) {
        return this.configuration.getProperties( key );
    }

    /**
     * @param key
     * @return
     * @see org.apache.commons.configuration.AbstractFileConfiguration#getProperty(java.lang.String)
     */
    public Object getProperty( String key ) {
        return this.configuration.getProperty( key );
    }

    /**
     * @param key
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getShort(java.lang.String)
     */
    public short getShort( String key ) {
        return this.configuration.getShort( key );
    }

    /**
     * @return
     * @see org.apache.commons.configuration.AbstractFileConfiguration#getKeys()
     */
    public Iterator<String> getKeys() {
        return this.configuration.getKeys();
    }

    /**
     * @param key
     * @return
     * @see org.apache.commons.configuration.AbstractConfiguration#getString(java.lang.String)
     */
    public String getString( String key ) {
        return this.configuration.getString( key );
    }

}
