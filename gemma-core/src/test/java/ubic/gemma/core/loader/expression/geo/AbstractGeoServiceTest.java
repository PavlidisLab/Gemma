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
package ubic.gemma.core.loader.expression.geo;

import java.net.URISyntaxException;

import ubic.basecode.util.FileTools;
import ubic.gemma.core.testing.BaseSpringContextTest;

/**
 * @author pavlidis
 */
public abstract class AbstractGeoServiceTest extends BaseSpringContextTest {

    private static final String GEO_TEST_DATA_ROOT = "/data/loader/expression/geo/";

    protected String getTestFileBasePath() throws URISyntaxException {

        return FileTools.resourceToPath( GEO_TEST_DATA_ROOT );

    }

    protected String getTestFileBasePath( String subPath ) throws URISyntaxException {

        return FileTools.resourceToPath( GEO_TEST_DATA_ROOT + subPath );

    }
}
