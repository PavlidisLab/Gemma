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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.loader.util.fetcher.HttpFetcher;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author pavlidis
 */
public class HttpFetcherTest {

    private static final Log log = LogFactory.getLog( HttpFetcherTest.class.getName() );
    private File f;

    /**
     * Regression guard for the Settings-retirement refactor (commit {@code f31cba192d}):
     * {@link HttpFetcher#fetch(String)} no longer reads {@code gemma.download.path} from
     * {@code Settings}; the caller must call {@link HttpFetcher#setLocalBasePath} first.
     * This test runs WITHOUT network access — the precondition fires before any fetch
     * attempt, so it stays in the fast default-run suite (no {@code @Tag("slow")}).
     */
    @Test
    public void fetch_withoutSetLocalBasePath_throwsIllegalState() {
        HttpFetcher fetcher = new HttpFetcher();
        IllegalStateException ise = assertThrows( IllegalStateException.class,
                () -> fetcher.fetch( "http://example.invalid/anything" ) );
        assertTrue( ise.getMessage().contains( "localBasePath" ),
                "exception message should explain the missing setter: " + ise.getMessage() );
    }

    /*
     * Test method for 'ubic.gemma.core.loader.loaderutils.HttpFetcher.fetch(String)'
     */
    @Test
    @Tag("slow")
    public void testFetch() {
        HttpFetcher hf = new HttpFetcher();
        hf.setLocalBasePath( System.getProperty( "java.io.tmpdir" ) );

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
