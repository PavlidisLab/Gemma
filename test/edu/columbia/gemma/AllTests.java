package edu.columbia.gemma;

import junit.framework.Test;
import junit.framework.TestSuite;
import edu.columbia.gemma.common.description.BibliographicReferenceDaoImplTest;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignServiceImplTest;
import edu.columbia.gemma.loader.arraydesign.AffyProbeReaderTest;
import edu.columbia.gemma.loader.arraydesign.IlluminaProbeReaderTest;
import edu.columbia.gemma.loader.entrez.pubmed.PubMedXMLFetcherTest;
import edu.columbia.gemma.loader.entrez.pubmed.PubMedXMLParserTest;
import edu.columbia.gemma.loader.loaderutils.IdentifierCreatorTest;
import edu.columbia.gemma.sequence.QtlDaoImplTest;
import edu.columbia.gemma.tools.SequenceManipulationTest;

public class AllTests {

    public static Test suite() {
        TestSuite suite = new TestSuite( "Test for edu.columbia.gemma" );
        // $JUnit-BEGIN$

        // $JUnit-END$
        suite.addTestSuite( BibliographicReferenceDaoImplTest.class );
        suite.addTestSuite( ArrayDesignServiceImplTest.class );
        suite.addTestSuite( QtlDaoImplTest.class );

        suite.addTestSuite( SequenceManipulationTest.class );
        suite.addTestSuite( AffyProbeReaderTest.class );
        suite.addTestSuite( IlluminaProbeReaderTest.class );
        
        suite.addTestSuite( IdentifierCreatorTest.class);
        
        suite.addTestSuite(PubMedXMLFetcherTest.class);
        suite.addTestSuite(PubMedXMLParserTest.class);
        return suite;
    }
}
