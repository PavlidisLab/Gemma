package edu.columbia.gemma;

import junit.framework.Test;
import junit.framework.TestSuite;
import edu.columbia.gemma.common.description.BibliographicReferenceDaoImplTest;
import edu.columbia.gemma.controller.LoaderControllerTest;
import edu.columbia.gemma.controller.entrez.pubmed.PubMedXmlControllerTest;
import edu.columbia.gemma.controller.flow.DetailBibRefFlowTests;
import edu.columbia.gemma.controller.flow.SearchPubMedFlowTests;
import edu.columbia.gemma.controller.flow.action.GetPubMedActionTests;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignServiceImplTest;
import edu.columbia.gemma.externalDb.ExternalDatabaseTest;
import edu.columbia.gemma.genome.sequenceAnalysis.BlatResultImplTest;
import edu.columbia.gemma.loader.arraydesign.AffyProbeReaderTest;
import edu.columbia.gemma.loader.arraydesign.IlluminaProbeReaderTest;
import edu.columbia.gemma.loader.entrez.pubmed.PubMedXMLFetcherTest;
import edu.columbia.gemma.loader.entrez.pubmed.PubMedXMLParserTest;
import edu.columbia.gemma.loader.genome.TaxonLoaderServiceTest;
import edu.columbia.gemma.loader.loaderutils.IdentifierCreatorTest;
import edu.columbia.gemma.loader.mage.MageMLConverterTest;
import edu.columbia.gemma.loader.mage.MageMLParserTest;
import edu.columbia.gemma.sequence.QtlDaoImplTest;
import edu.columbia.gemma.tools.GoldenPathTest;
import edu.columbia.gemma.tools.SequenceManipulationTest;

public class AllTests {

    public static Test suite() {
        
        TestSuite suite = new TestSuite( "Test for edu.columbia.gemma" );
        //$JUnit-BEGIN$
        suite.addTestSuite( BibliographicReferenceDaoImplTest.class );
        suite.addTestSuite( ArrayDesignServiceImplTest.class );
        suite.addTestSuite( QtlDaoImplTest.class );

        suite.addTestSuite( SequenceManipulationTest.class );
        suite.addTestSuite( AffyProbeReaderTest.class );
        suite.addTestSuite( IlluminaProbeReaderTest.class );
        
        suite.addTestSuite( IdentifierCreatorTest.class);
        
        suite.addTestSuite(PubMedXMLFetcherTest.class);
        suite.addTestSuite(PubMedXMLParserTest.class);
        
        suite.addTestSuite(MageMLConverterTest.class);
        suite.addTestSuite(MageMLParserTest.class);
        
        suite.addTestSuite(LoaderControllerTest.class);
        suite.addTestSuite(PubMedXmlControllerTest.class);
        
        suite.addTestSuite(TaxonLoaderServiceTest.class);
        
        suite.addTestSuite(ExternalDatabaseTest.class);
        
        suite.addTestSuite(GoldenPathTest.class);
        
        suite.addTestSuite(SearchPubMedFlowTests.class);
        suite.addTestSuite(GetPubMedActionTests.class);
        suite.addTestSuite(DetailBibRefFlowTests.class);
        
        suite.addTestSuite(BlatResultImplTest.class);
        //$JUnit-END$
        
        return suite;
    }
}
