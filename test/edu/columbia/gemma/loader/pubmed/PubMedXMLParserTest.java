package edu.columbia.gemma.loader.pubmed;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;

import javax.xml.parsers.ParserConfigurationException;

import edu.columbia.gemma.common.description.BibliographicReference;

import junit.framework.TestCase;

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

        assertEquals( "2004", br.getYear() );
        assertEquals( "Lee, Homin K; Hsu, Amy K; Sajdak, Jon; Qin, Jie; Pavlidis, Paul", br.getAuthorList() );
        assertEquals( "Genome Res", br.getPublication() );
        assertEquals("Coexpression analysis of human genes across many microarray data sets.", br.getTitle());
        
        SimpleDateFormat f = new SimpleDateFormat("mm/HH/MM/dd/yyyy");
        assertEquals("00/05/06/03/2004", f.format(br.getPublicationDate()));
    }

}
