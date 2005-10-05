/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.util;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ConfigUtils {

    /**
     * 
     */
    private static final String GEMMA_PROPERTIES = "Gemma.properties";

    /**
     * Get a Gemma property.
     * 
     * @param propertyName
     * @return
     */
    public static String getProperty( String propertyName ) {
        Configuration config;
        try {
            config = new PropertiesConfiguration( GEMMA_PROPERTIES );
            return ( String ) config.getProperty( propertyName );
        } catch ( ConfigurationException e ) {
            throw new RuntimeException( e );
        }
    }

}
