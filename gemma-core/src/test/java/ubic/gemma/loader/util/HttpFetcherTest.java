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
package ubic.gemma.loader.util;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.loader.util.fetcher.HttpFetcher;
import ubic.gemma.model.common.description.LocalFile;

/**
 * @author pavlidis
 * @version $Id$
 */
public class HttpFetcherTest extends TestCase {

    private static Log log = LogFactory.getLog( HttpFetcherTest.class.getName() );
    File f;

    /*
     * Test method for 'ubic.gemma.loader.loaderutils.HttpFetcher.fetch(String)'
     */
    public void testFetch() {
        HttpFetcher hf = new HttpFetcher();

        try {
            hf.setForce( true );
            Collection<LocalFile> results = hf.fetch( "http://www.yahoo.com" );
            assertNotNull( results );
            assertTrue( results.size() > 0 && results.iterator().next().getLocalURL() != null );
            f = new File( results.iterator().next().getLocalURL().toURI() );
            assertTrue( f.length() > 0 );
        } catch ( Exception e ) {
            if ( e.getCause() instanceof IOException ) {
                log.error( "Got IOException, skipping test" );
                return;
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if ( f != null ) {
            f.delete();
            f.getParentFile().delete();
        }
    }
}
