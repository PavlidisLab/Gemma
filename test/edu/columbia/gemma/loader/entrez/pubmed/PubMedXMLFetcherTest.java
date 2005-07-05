package edu.columbia.gemma.loader.entrez.pubmed;

import java.io.IOException;
import java.text.SimpleDateFormat;

import org.xml.sax.SAXParseException;

import junit.framework.TestCase;
import edu.columbia.gemma.common.description.BibliographicReference;

public class PubMedXMLFetcherTest extends TestCase {

    PubMedXMLFetcher pmf;

    protected void setUp() throws Exception {
        super.setUp();
        pmf = new PubMedXMLFetcher();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        pmf = null;
    }

    public final void testRetrieveByHTTP() throws Exception {
        BibliographicReference br = pmf.retrieveByHTTP( 15173114 );
        assertEquals( "Lee, Homin K; Hsu, Amy K; Sajdak, Jon; Qin, Jie; Pavlidis, Paul", br.getAuthorList() );
        assertEquals( "Genome Res", br.getPublication() );
        assertEquals( "Coexpression analysis of human genes across many microarray data sets.", br.getTitle() );

        SimpleDateFormat f = new SimpleDateFormat( "mm/HH/MM/dd/yyyy" );
        assertEquals( "00/05/06/03/2004", f.format( br.getPublicationDate() ) );
    }

    public final void testRetrieveByHTTPNotFound() throws Exception {
        try {
            BibliographicReference br = pmf.retrieveByHTTP( 1517311444 );
            fail( "Should have gotten a 'Not found' exception" );
        } catch ( SAXParseException e ) {
        } catch ( IOException e ) {
        }
    }

}
