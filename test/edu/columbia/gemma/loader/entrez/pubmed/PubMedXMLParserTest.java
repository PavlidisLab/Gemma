package edu.columbia.gemma.loader.entrez.pubmed;

import java.io.InputStream;
import java.text.SimpleDateFormat;

import junit.framework.TestCase;
import edu.columbia.gemma.common.description.BibliographicReference;
import edu.columbia.gemma.loader.entrez.pubmed.PubMedXMLParser;

public class PubMedXMLParserTest extends TestCase {

    InputStream testStream;
    PubMedXMLParser testParser;

    protected void setUp() throws Exception {
        super.setUp();
        testStream = PubMedXMLParserTest.class.getResourceAsStream( "/data/pubmed-test.xml" );
        testParser = new PubMedXMLParser();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        testStream.close();
        testParser = null;
        testStream = null;
    }

    public void testParse() throws Exception {
        BibliographicReference br = testParser.parse( testStream );

        assertEquals( "Lee, Homin K; Hsu, Amy K; Sajdak, Jon; Qin, Jie; Pavlidis, Paul", br.getAuthorList() );
        assertEquals( "Genome Res", br.getPublication() );
        assertEquals( "Coexpression analysis of human genes across many microarray data sets.", br.getTitle() );

        SimpleDateFormat f = new SimpleDateFormat( "mm/HH/MM/dd/yyyy" );
        assertEquals( "00/05/06/03/2004", f.format( br.getPublicationDate() ) );
    }

}
