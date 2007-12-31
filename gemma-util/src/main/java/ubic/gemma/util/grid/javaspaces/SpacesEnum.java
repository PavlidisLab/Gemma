/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.util.grid.javaspaces;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * An enumeration of java spaces used by Gemma.
 * 
 * @author keshav
 * @version $Id$
 */
public enum SpacesEnum {

    DEFAULT_SPACE(System.getProperty( "user.home" ) + System.getProperty( "file.separator" ) + "Gemma.properties");

    private String spaceUrl = null;

    /**
     * @param url
     */
    private SpacesEnum( String gemmaProperties ) {
        Properties properties = new Properties();
        try {
            properties.load( new FileInputStream( gemmaProperties ) );
            this.spaceUrl = properties.getProperty( "gemma.spaces.url.0" );
        } catch ( IOException e ) {
            throw new RuntimeException( "Cannot load properties file.  Error is: " + e );
        }
    }

    /**
     * @return String
     */
    public String getSpaceUrl() {
        return this.spaceUrl;
    }

}
