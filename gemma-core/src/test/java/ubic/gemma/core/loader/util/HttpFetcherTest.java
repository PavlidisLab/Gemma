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
package ubic.gemma.core.loader.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.loader.util.fetcher.HttpFetcher;
import ubic.gemma.core.util.test.category.SlowTest;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author pavlidis
 */
@Tag("slow")
@Category(SlowTest.class)
public class HttpFetcherTest {

    private static final Log log = LogFactory.getLog( HttpFetcherTest.class.getName() );
    private File f;

    /*
     * Test method for 'ubic.gemma.core.loader.loaderutils.HttpFetcher.fetch(String)'
     */
    @Test
    public void testFetch() {
        HttpFetcher hf = new HttpFetcher();

        try {
            hf.setForce( true );
            Collection<File> results = hf.fetch( "http://www.yahoo.com" );
            assertNotNull( results );
            assertTrue( results.size() > 0 && results.iterator().next() != null );
            f = results.iterator().next();
            assertTrue( f.length() > 0 );
        } catch ( Exception e ) {
            if ( e.getCause() instanceof IOException ) {
                HttpFetcherTest.log.error( "Got IOException, skipping test" );
            }
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored") // Does not matter
    @AfterEach
    protected void tearDown() throws Exception {
        if ( f != null ) {
            f.delete();
            f.getParentFile().delete();
        }
    }
}
