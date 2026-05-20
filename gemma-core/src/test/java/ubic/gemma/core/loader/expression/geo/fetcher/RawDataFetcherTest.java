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
package ubic.gemma.core.loader.expression.geo.fetcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.loader.util.TestUtils;
import ubic.gemma.core.util.test.category.SlowTest;

import java.io.File;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author pavlidis
 */
@Category(SlowTest.class)
@Tag("slow")
public class RawDataFetcherTest {
    private static final Log log = LogFactory.getLog( RawDataFetcherTest.class.getName() );

    /**
     * Test method for 'ubic.gemma.core.loader.expression.geo.RawDataFetcher.fetch(String)'. This is kind of a slow test
     * because the file is big.
     */
    @Test
    public void testFetch() {
        RawDataFetcher rdf = new RawDataFetcher();
        try {
            Collection<File> result = rdf.fetch( "GSE1105" );
            assertNotNull( result );
            assertEquals( 8, result.size() );
        } catch ( Exception e ) {
            if ( !TestUtils.LogNcbiError( RawDataFetcherTest.log, e ) )
                throw e;
        }

    }

    @Test
    public void testFetchNothingThere() {
        RawDataFetcher rdf = new RawDataFetcher();

        try {
            Collection<File> result = rdf.fetch( "GSE1001" );
            assert ( result == null );
        } catch ( Exception e ) {
            if ( e.getCause() instanceof java.net.UnknownHostException ) {
                RawDataFetcherTest.log.error( "Failed to connect to NCBI, skipping test" );
                return;
            } else if ( e.getCause() instanceof org.apache.commons.net.ftp.FTPConnectionClosedException ) {
                RawDataFetcherTest.log.error( "Failed to connect to NCBI, skipping test" );
                return;
            }
            throw e;
        }
    }

}
