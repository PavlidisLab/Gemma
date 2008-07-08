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
package ubic.gemma.loader.entrez.pubmed;

import java.util.Collection;
import java.util.HashSet;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.common.description.BibliographicReference;

/**
 * @author pavlidis
 * @version $Id$
 */
public class PubMedSearchTest extends TestCase {

    private static Log log = LogFactory.getLog( PubMedSearchTest.class.getName() );

    /*
     * Test method for 'ubic.gemma.loader.entrez.pubmed.PubMedSearch.searchAndRetriveByHTTP(Collection<String>)'
     */
    public void testSearchAndRetrieveByHTTP() throws Exception {
        try {
            PubMedSearch pms = new PubMedSearch();
            Collection<String> searchTerms = new HashSet<String>();
            searchTerms.add( "brain" );
            searchTerms.add( "hippocampus" );
            searchTerms.add( "habenula" );
            searchTerms.add( "glucose" );
            Collection<BibliographicReference> actualResult = pms.searchAndRetrieveByHTTP( searchTerms );
            assertTrue( "Expected at least 5 results, got " + actualResult.size(), actualResult.size() >= 5 ); // at
            // least,
            // this
            // was
            // the
            // result
            // on
            // 4/2008
        } catch ( java.net.UnknownHostException e ) {
            log.warn( "Test skipped due to unknown host exception" );
            return;
        } catch ( java.io.IOException e ) {
            if ( e.getMessage().contains( "503" ) ) {
                log.warn( "Test skipped due to a 503 from NCBI" );
                return;
            } else {
                throw e;
            }
        }
    }

    public void testSearchAndRetrievIdsByHTTP() throws Exception {
        try {
            PubMedSearch pms = new PubMedSearch();
            Collection<String> searchTerms = new HashSet<String>();
            searchTerms.add( "brain" );
            searchTerms.add( "hippocampus" );
            searchTerms.add( "habenula" );
            searchTerms.add( "glucose" );
            Collection<String> actualResult = pms.searchAndRetrieveIdsByHTTP( searchTerms );
            assertTrue( "Expect at least 5 results, got " + actualResult.size(), actualResult.size() >= 5 ); // at
            // least,
            // this
            // was
            // the
            // result
            // on
            // 4/2008.
        } catch ( java.net.UnknownHostException e ) {
            log.warn( "Test skipped due to unknown host exception" );
            return;
        } catch ( java.io.IOException e ) {
            if ( e.getMessage().contains( "503" ) ) {
                log.warn( "Test skipped due to a 503 from NCBI" );
                return;
            } else {
                throw e;
            }
        }
    }

    /*
     * Test method for 'ubic.gemma.loader.entrez.pubmed.PubMedSearch.searchAndRetriveByHTTP(Collection<String>)'
     */
    public void testSearchAndRetrieveIdByHTTP() throws Exception {
        try {
            PubMedSearch pms = new PubMedSearch();
            Collection<String> searchTerms = new HashSet<String>();
            searchTerms.add( "17084362" );
            Collection<BibliographicReference> actualResult = pms.searchAndRetrieveIdByHTTP( searchTerms );
            assertEquals( 1, actualResult.size() ); // at least, this was the result on 8/10/2006.
        } catch ( java.net.UnknownHostException e ) {
            log.warn( "Test skipped due to unknown host exception" );
            return;
        } catch ( java.io.IOException e ) {
            if ( e.getMessage().contains( "503" ) ) {
                log.warn( "Test skipped due to a 503 from NCBI" );
                return;
            } else {
                throw e;
            }
        }
    }

    /*
     * Test method for 'ubic.gemma.loader.entrez.pubmed.PubMedSearch.searchAndRetriveByHTTP(Collection<String>)'
     */
    public void testSearchAndRetrieveByHTTPInChunks() throws Exception {
        try {
            PubMedSearch pms = new PubMedSearch();
            Collection<String> searchTerms = new HashSet<String>();
            searchTerms.add( "brain" );
            searchTerms.add( "hippocampus" );
            searchTerms.add( "habenula" );
            Collection<BibliographicReference> actualResult = pms.searchAndRetrieveByHTTP( searchTerms );
            assertTrue( actualResult.size() >= 10 ); // at least, this was the result on 4/2008. }
        } catch ( java.net.UnknownHostException e ) {
            log.warn( "Test skipped due to unknown host exception" );
        } catch ( java.io.IOException e ) {
            if ( e.getMessage().contains( "503" ) ) {
                log.warn( "Test skipped due to a 503 from NCBI" );
                return;
            } else {
                throw e;
            }
        }

        return;
    }
}
