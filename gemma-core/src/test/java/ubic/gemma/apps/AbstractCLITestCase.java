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
package ubic.gemma.apps;

import java.io.IOException;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.util.ConfigUtils;

/**
 * Base class for CLI tests.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class AbstractCLITestCase extends TestCase {

    protected static Log log = LogFactory.getLog( AbstractCLITestCase.class.getName() );

    /**
     * Get the location of the Gemma home; used to create absolute paths to files so that command line tools can be
     * tested.
     * 
     * @return
     * @throws IOException
     */
    protected final String getTestFileBasePath() throws IOException {
        String path = ConfigUtils.getString( "gemma.home" );
        if ( path == null ) {
            throw new IOException( "You must define the 'gemma.home' variable in your build.properties file" );
        }
        return path;
    }

    /**
     * Here to keep auto-test-runners happy (e.g. Eclipse)
     */
    public final void testDummy() {
        assertTrue( true );
        return;
    }

}
