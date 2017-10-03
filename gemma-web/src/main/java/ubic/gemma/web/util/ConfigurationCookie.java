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

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.io.FileHandler;

import javax.servlet.http.Cookie;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * Cookie class that also presents a commons configuration interface.
 *
 * @author pavlidis
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
        FileHandler fh = new FileHandler( configuration );
        fh.load( reader );
    }

    /**
     * Create a cookie with the given name. The value should be populated using the setProperty() or addProperty()
     * methods.
     *
     * @param name name
     */
    public ConfigurationCookie( String name ) {
        super( name, "" );
        configuration = new PropertiesConfiguration();
    }

    public void addProperty( String key, Object value ) {
        this.configuration.addProperty( key, value );
        // Rewrite the value.
        StringWriter writer = new StringWriter();
        try {
            FileHandler fh = new FileHandler( configuration );
            fh.save( writer );
        } catch ( ConfigurationException e ) {
            throw new RuntimeException( e );
        }
        setValue( writer.toString().replaceAll( "\n", PROPERTY_DELIMITER ) );
    }

    public boolean getBoolean( String key ) {
        return this.configuration.getBoolean( key );
    }

    public double getDouble( String key ) {
        return this.configuration.getDouble( key );
    }

    public int getInt( String key ) {
        return this.configuration.getInt( key );
    }

    public Iterator<String> getKeys() {
        return this.configuration.getKeys();
    }

    public List<?> getList( String key ) {
        return this.configuration.getList( key );
    }

    public long getLong( String key ) {
        return this.configuration.getLong( key );
    }

    public Properties getProperties( String key ) {
        return this.configuration.getProperties( key );
    }

    public Object getProperty( String key ) {
        return this.configuration.getProperty( key );
    }

    public short getShort( String key ) {
        return this.configuration.getShort( key );
    }

    public String getString( String key ) {
        return this.configuration.getString( key );
    }

    public void setProperty( String key, Object value ) {
        this.configuration.setProperty( key, value );

        // Rewrite the value.
        StringWriter writer = new StringWriter();
        try {
            FileHandler fh = new FileHandler( configuration );
            fh.save( writer );
        } catch ( ConfigurationException e ) {
            throw new RuntimeException( e );
        }
        setValue( PROPERTY_DELIMITER + writer.toString().replaceAll( "\n", PROPERTY_DELIMITER ) );
    }

    /**
     * Don't use this method if you can help it! Use setProperty instead.
     *
     * @param value value
     */
    @Override
    public final void setValue( String value ) {
        super.setValue( value );
    }

}
