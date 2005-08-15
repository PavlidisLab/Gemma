package edu.columbia.gemma;

import junit.framework.Test;
import junit.framework.TestSuite;
import edu.columbia.gemma.common.auditAndSecurity.UserDaoImplTest;
import edu.columbia.gemma.common.auditAndSecurity.UserRoleServiceImplTest;
import edu.columbia.gemma.common.auditAndSecurity.UserServiceImplTest;
import edu.columbia.gemma.common.description.BibliographicReferenceDaoImplTest;
import edu.columbia.gemma.common.description.ExternalDatabaseServiceImplTest;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignServiceImplTest;
import edu.columbia.gemma.externalDb.ExternalDatabaseTest;
import edu.columbia.gemma.genome.sequenceAnalysis.BlatResultImplTest;
import edu.columbia.gemma.loader.entrez.pubmed.PubMedXMLFetcherTest;
import edu.columbia.gemma.loader.entrez.pubmed.PubMedXMLParserTest;
import edu.columbia.gemma.loader.expression.arrayDesign.AffyProbeReaderTest;
import edu.columbia.gemma.loader.expression.arrayDesign.IlluminaProbeReaderTest;
import edu.columbia.gemma.loader.expression.mage.MageMLParserTest;
import edu.columbia.gemma.loader.genome.gene.GeneParserTest;
import edu.columbia.gemma.sequence.QtlDaoImplTest;
import edu.columbia.gemma.tools.GoldenPathTest;
import edu.columbia.gemma.tools.SequenceManipulationTest;
import edu.columbia.gemma.web.controller.common.auditAndSecurity.SignupControllerTest;
import edu.columbia.gemma.web.controller.entrez.pubmed.PubMedArticleListControllerTest;
import edu.columbia.gemma.web.controller.entrez.pubmed.PubMedXmlControllerTest;
import edu.columbia.gemma.web.controller.flow.action.entrez.pubmed.GetPubMedActionTests;
import edu.columbia.gemma.web.controller.flow.entrez.pubmed.DetailBibRefFlowTests;
import edu.columbia.gemma.web.controller.flow.entrez.pubmed.SearchPubMedFlowTests;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author pavlidis
 * @author keshav
 * @version $Id$
 */
public class AllTests {

    public static Test suite() {

        TestSuite suite = new TestSuite( "Test for edu.columbia.gemma" );
        // $JUnit-BEGIN$
        suite.addTestSuite( BibliographicReferenceDaoImplTest.class );
        
        suite.addTestSuite( ArrayDesignServiceImplTest.class );
        suite.addTestSuite( QtlDaoImplTest.class );

        suite.addTestSuite( SequenceManipulationTest.class );
        suite.addTestSuite( AffyProbeReaderTest.class );
        suite.addTestSuite( IlluminaProbeReaderTest.class );

        suite.addTestSuite( PubMedXMLFetcherTest.class );
        suite.addTestSuite( PubMedXMLParserTest.class );

     
        suite.addTestSuite( MageMLParserTest.class );

        // suite.addTestSuite( LoaderControllerTest.class );

        suite.addTestSuite( PubMedXmlControllerTest.class );
        suite.addTestSuite( PubMedArticleListControllerTest.class );

        suite.addTestSuite( ExternalDatabaseTest.class );
        suite.addTestSuite( ExternalDatabaseServiceImplTest.class );

        suite.addTestSuite( GoldenPathTest.class );

        suite.addTestSuite( SearchPubMedFlowTests.class );
        suite.addTestSuite( GetPubMedActionTests.class );
        suite.addTestSuite( DetailBibRefFlowTests.class );

        suite.addTestSuite( BlatResultImplTest.class );

        suite.addTestSuite( UserRoleServiceImplTest.class );
        suite.addTestSuite( UserServiceImplTest.class );
        suite.addTestSuite( UserDaoImplTest.class );

        suite.addTestSuite( SignupControllerTest.class );
        
        suite.addTestSuite( GeneParserTest.class );
        // $JUnit-END$

        return suite;
    }
}
