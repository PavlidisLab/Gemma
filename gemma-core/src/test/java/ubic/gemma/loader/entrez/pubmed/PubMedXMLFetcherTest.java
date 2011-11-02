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

import java.io.IOException;
import java.text.SimpleDateFormat;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.common.description.BibliographicReference;

/**
 * @author pavlidis
 * @version $Id$
 */
public class PubMedXMLFetcherTest extends TestCase {
    private static Log log = LogFactory.getLog( PubMedXMLFetcherTest.class.getName() );
    PubMedXMLFetcher pmf;

    public final void testRetrieveByHTTP() throws Exception {
        try {
            BibliographicReference br = pmf.retrieveByHTTP( 15173114 );

            assertNotNull( br );

            assertEquals( "Lee, Homin K; Hsu, Amy K; Sajdak, Jon; Qin, Jie; Pavlidis, Paul", br.getAuthorList() );
            assertEquals( "Genome Res", br.getPublication() );
            assertEquals( "Coexpression analysis of human genes across many microarray data sets.", br.getTitle() );

            SimpleDateFormat f = new SimpleDateFormat( "mm/HH/MM/dd/yyyy" );
            assertEquals( "00/00/06/01/2004", f.format( br.getPublicationDate() ) );
        } catch ( RuntimeException e ) {
            checkCause( e );
            return;
        }
    }

    /**
     * 20301615 is a NCBI bookshelf article, not a paper
     * 
     * @throws Exception
     */
    public final void testRetrieveByHTTPBookshelf() throws Exception {
        try {
            BibliographicReference br = pmf.retrieveByHTTP( 20301615 );

            assertNotNull( br );

            assertEquals( "Miles, Judith H; McCathren, Rebecca B; Stichter, Janine; Shinawi, Marwan",
                    br.getAuthorList() );

            assertEquals( "GeneReviews", br.getPublication() );
            assertEquals( "Autism Spectrum Disorders", br.getTitle() );

            SimpleDateFormat f = new SimpleDateFormat( "yyyy" );
            assertEquals( "1993", f.format( br.getPublicationDate() ) );
        } catch ( RuntimeException e ) {
            checkCause( e );
            return;
        }
    }

    public final void testRetrieveByHTTPNotFound() throws Exception {
        try {
            BibliographicReference br = pmf.retrieveByHTTP( 1517311444 );
            assertNull( br );
        } catch ( RuntimeException e ) {
            checkCause( e );
            return;
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        pmf = new PubMedXMLFetcher();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        pmf = null;
    }

    /**
     * @param e
     */
    private void checkCause( RuntimeException e ) {
        if ( e.getCause() instanceof java.net.ConnectException ) {
            log.warn( "Test skipped due to connection exception" );
            return;
        } else if ( e.getCause() instanceof java.net.UnknownHostException ) {
            log.warn( "Test skipped due to unknown host exception" );
            return;
        } else if ( e.getCause() instanceof IOException && e.getMessage().contains( "503" ) ) {
            log.warn( "Test skipped due to a 503 error from NCBI" );
            return;
        } else if ( e.getCause() instanceof IOException && e.getMessage().contains( "502" ) ) {
            log.warn( "Test skipped due to a 502 error from NCBI" );
            return;
        } else {
            throw ( e );
        }
    }
}
