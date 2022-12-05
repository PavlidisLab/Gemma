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
package ubic.gemma.core.loader.entrez.pubmed;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ubic.gemma.core.util.test.category.PubMedTest;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.common.description.BibliographicReference;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author pavlidis
 */
@Category(PubMedTest.class)
public class PubMedSearchTest {

    private static final Log log = LogFactory.getLog( PubMedSearchTest.class.getName() );

    /*
     * Test method for 'ubic.gemma.core.loader.entrez.pubmed.PubMedSearch.searchAndRetriveByHTTP(Collection<String>)'
     */
    @Test
    @Category(SlowTest.class)
    public void testSearchAndRetrieveByHTTP() throws Exception {
        try {
            PubMedSearch pms = new PubMedSearch();
            Collection<String> searchTerms = new HashSet<>();
            searchTerms.add( "brain" );
            searchTerms.add( "hippocampus" );
            searchTerms.add( "habenula" );
            searchTerms.add( "glucose" );
            Collection<BibliographicReference> actualResult = pms.searchAndRetrieveByHTTP( searchTerms );
            assertTrue( "Expected at least 5 results, got " + actualResult.size(), actualResult.size() >= 5 );
            /*
             * at least, this was the result on 4/2008.
             */
            BibliographicReference r = actualResult.iterator().next();
            assertNotNull(r.getAuthorList());
            assertNotNull(r.getPublicationDate());
        } catch ( java.net.UnknownHostException e ) {
            PubMedSearchTest.log.warn( "Test skipped due to unknown host exception" );
        } catch ( java.io.IOException e ) {
            if ( e.getMessage().contains( "503" ) || e.getMessage().contains( "502" ) ) {
                PubMedSearchTest.log.warn( "Test skipped due to a 50X error from NCBI" );
                return;
            }
            throw e;
        }
    }

    /*
     * Test method for 'ubic.gemma.core.loader.entrez.pubmed.PubMedSearch.searchAndRetriveByHTTP(Collection<String>)'
     */
    @Test
    @Category(SlowTest.class)
    public void testSearchAndRetrieveByHTTPInChunks() throws Exception {
        try {
            PubMedSearch pms = new PubMedSearch();
            Collection<String> searchTerms = new HashSet<>();
            searchTerms.add( "brain" );
            searchTerms.add( "hippocampus" );
            searchTerms.add( "habenula" );
            Collection<BibliographicReference> actualResult = pms.searchAndRetrieveByHTTP( searchTerms );
            /*
             * at least, this was the result on 4/2008.
             */
            assertTrue( "Expected at least 10, got " + actualResult.size(), actualResult.size() >= 10 );
        } catch ( java.net.UnknownHostException e ) {
            PubMedSearchTest.log.warn( "Test skipped due to unknown host exception" );
        } catch ( java.io.IOException e ) {
            this.checkCause( e );
        }

    }

    @Test
    @Category(SlowTest.class)
    public void testSearchAndRetrieveIdByHTTPBookshelf() throws Exception {
        try {
            PubMedSearch pms = new PubMedSearch();
            Collection<String> searchTerms = new HashSet<>();
            searchTerms.add( "23865096" );
            Collection<BibliographicReference> actualResult = pms.searchAndRetrieveIdByHTTP( searchTerms );
            assertEquals( 1, actualResult.size() );
        } catch ( java.net.UnknownHostException e ) {
            PubMedSearchTest.log.warn( "Test skipped due to unknown host exception" );
        } catch ( java.io.IOException e ) {
            this.checkCause( e );
        }
    }

    @Test
    public void testSearchAndRetrieveIdsByHTTP() throws Exception {
        try {
            PubMedSearch pms = new PubMedSearch();
            Collection<String> searchTerms = new HashSet<>();
            searchTerms.add( "brain" );
            searchTerms.add( "hippocampus" );
            searchTerms.add( "habenula" );
            searchTerms.add( "glucose" );
            Collection<String> actualResult = pms.searchAndRetrieveIdsByHTTP( searchTerms );
            assertTrue( "Expect at least 5 results, got " + actualResult.size(), actualResult.size() >= 5 );
            /*
             * at least, this was the result on 4/2008.
             */
        } catch ( java.net.UnknownHostException e ) {
            PubMedSearchTest.log.warn( "Test skipped due to unknown host exception" );
        } catch ( java.io.IOException e ) {
            this.checkCause( e );
        }
    }

    private void checkCause( java.io.IOException e ) throws IOException {
        if ( e.getMessage().contains( "503" ) || e.getMessage().contains( "502" ) ) {
            PubMedSearchTest.log.warn( "Test skipped due to a 50x from NCBI" );
            return;
        }
        throw e;
    }
}
