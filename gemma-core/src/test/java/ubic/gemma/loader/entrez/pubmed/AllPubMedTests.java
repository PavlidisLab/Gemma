package ubic.gemma.loader.entrez.pubmed;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * TODO - DOCUMENT ME
 *
 * @author pavlidis
 * @version $Id$
 */
public class AllPubMedTests {

    public static Test suite() {
        TestSuite suite = new TestSuite( "Test for ubic.gemma.loader.entrez.pubmed" );
        //$JUnit-BEGIN$
        suite.addTestSuite( PubMedXMLFetcherTest.class );
        suite.addTestSuite( ESearchXMLParserTest.class );
        suite.addTestSuite( PubMedXMLParserTest.class );
        suite.addTestSuite( PubMedSearchTest.class );
        //$JUnit-END$
        return suite;
    }

}
