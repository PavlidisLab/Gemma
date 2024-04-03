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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ubic.gemma.core.util.test.category.PubMedTest;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.common.description.BibliographicReference;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import static org.junit.Assert.*;

/**
 * @author pavlidis
 */
@Category({ PubMedTest.class, SlowTest.class })
public class PubMedXMLFetcherTest {
    private static final Log log = LogFactory.getLog( PubMedXMLFetcherTest.class.getName() );
    private PubMedXMLFetcher pmf;

    @Test
    public final void testRetrieveByHTTP() {
        try {
            BibliographicReference br = pmf.retrieveByHTTP( 15173114 );

            assertNotNull( br );

            assertEquals( "Lee, Homin K; Hsu, Amy K; Sajdak, Jon; Qin, Jie; Pavlidis, Paul", br.getAuthorList() );
            assertEquals( "Genome Res", br.getPublication() );
            assertEquals( "Coexpression analysis of human genes across many microarray data sets.", br.getTitle() );

            SimpleDateFormat f = new SimpleDateFormat( "mm/HH/MM/dd/yyyy", Locale.ENGLISH );
            assertEquals( "00/00/06/01/2004", f.format( br.getPublicationDate() ) );
        } catch ( RuntimeException e ) {
            this.checkCause( e );
        }
    }

    @Test
    public final void testRetrieveByHTTP2() {
        try {
            BibliographicReference br = pmf.retrieveByHTTP( 24850731 );

            assertNotNull( br );

            assertEquals(
                    "Iwata-Yoshikawa, Naoko; Uda, Akihiko; Suzuki, Tadaki; Tsunetsugu-Yokota, Yasuko; Sato, Yuko; "
                            + "Morikawa, Shigeru; Tashiro, Masato; Sata, Tetsutaro; Hasegawa, Hideki; Nagata, Noriyo",
                    br.getAuthorList() );
            assertEquals( "J Virol", br.getPublication() );

        } catch ( RuntimeException e ) {
            this.checkCause( e );
        }
    }

    /*
     * 23865096 is a NCBI bookshelf article, not a paper
     */
    @Test
    public final void testRetrieveByHTTPBookshelf() {
        try {
            BibliographicReference br = pmf.retrieveByHTTP( 23865096 );

            assertNotNull( br );

            assertEquals( "Ocansey, Sharon; Tatton-Brown, Katrina", br.getAuthorList() );

            assertEquals( "GeneReviews", br.getPublication().substring( 0, "GeneReviews".length() ) );
            assertEquals( "EZH2-Related Overgrowth", br.getTitle() );

            SimpleDateFormat f = new SimpleDateFormat( "yyyy", Locale.ENGLISH );
            assertEquals( "2013", f.format( br.getPublicationDate() ) );
        } catch ( RuntimeException e ) {
            this.checkCause( e );
        }
    }

    @Test
    public final void testRetrieveByHTTPNotFound() {
        try {
            BibliographicReference br = pmf.retrieveByHTTP( 1517311444 );
            assertNull( br );
        } catch ( RuntimeException e ) {
            this.checkCause( e );
        }
    }

    @Before
    public void setUp() {
        pmf = new PubMedXMLFetcher();
    }

    @After
    public void tearDown() {
        pmf = null;
    }

    private void checkCause( RuntimeException e ) {
        if ( e.getCause() instanceof java.net.ConnectException ) {
            PubMedXMLFetcherTest.log.warn( "Test skipped due to connection exception" );
        } else if ( e.getCause() instanceof java.net.UnknownHostException ) {
            PubMedXMLFetcherTest.log.warn( "Test skipped due to unknown host exception" );
        } else if ( e.getCause() instanceof IOException && e.getMessage().contains( "503" ) ) {
            PubMedXMLFetcherTest.log.warn( "Test skipped due to a 503 error from NCBI" );
        } else if ( e.getCause() instanceof IOException && e.getMessage().contains( "502" ) ) {
            PubMedXMLFetcherTest.log.warn( "Test skipped due to a 502 error from NCBI" );
        } else {
            throw ( e );
        }
    }
}
