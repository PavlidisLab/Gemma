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
package ubic.gemma.testing;

import java.io.IOException;

import ubic.gemma.util.ConfigUtils;

/**
 * @author pavlidis
 * @version $Id$
 */
public abstract class AbstractGeoServiceTest extends BaseSpringContextTest {

    public static final String GEO_TEST_DATA_ROOT = "/gemma-core/src/test/resources/data/loader/expression/geo/";

    protected String getTestFileBasePath() throws IOException {
        String path = ConfigUtils.getString( "gemma.home" );
        if ( path == null ) {
            throw new IOException( "You must define the 'gemma.home' variable in your Gemma.properties file" );
        }
        return path;
    }

}
