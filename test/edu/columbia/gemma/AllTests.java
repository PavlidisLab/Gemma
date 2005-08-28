package edu.columbia.gemma;

import junit.framework.Test;
import junit.framework.TestSuite;
import edu.columbia.gemma.common.auditAndSecurity.AuditTrailDaoTest;
import edu.columbia.gemma.common.auditAndSecurity.UserDaoImplTest;
import edu.columbia.gemma.common.auditAndSecurity.UserRoleServiceImplTest;
import edu.columbia.gemma.common.auditAndSecurity.UserServiceImplTest;
import edu.columbia.gemma.common.description.BibliographicReferenceDaoImplTest;
import edu.columbia.gemma.common.description.BibliographicReferenceServiceImplTest;
import edu.columbia.gemma.common.description.DatabaseEntryDaoImplTest;
import edu.columbia.gemma.common.description.ExternalDatabaseServiceImplTest;
import edu.columbia.gemma.common.protocol.ProtocolServiceTest;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignServiceImplTest;
import edu.columbia.gemma.expression.experiment.ExpressionExperimentServiceImplTest;
import edu.columbia.gemma.externalDb.ExternalDatabaseTest;
import edu.columbia.gemma.genome.gene.CandidateGeneImplTest;
import edu.columbia.gemma.genome.gene.CandidateGeneListDAOImplTest;
import edu.columbia.gemma.genome.gene.CandidateGeneListImplTest;
import edu.columbia.gemma.genome.gene.CandidateGeneListServiceImplTest;
import edu.columbia.gemma.genome.gene.GeneServiceImplTest;
import edu.columbia.gemma.genome.sequenceAnalysis.BlatResultImplTest;
import edu.columbia.gemma.loader.association.Gene2GOAssociationParserTest;
import edu.columbia.gemma.loader.description.OntologyEntryLoaderTest;
import edu.columbia.gemma.loader.entrez.pubmed.PubMedXMLFetcherTest;
import edu.columbia.gemma.loader.entrez.pubmed.PubMedXMLParserTest;
import edu.columbia.gemma.loader.expression.arrayDesign.AffyProbeReaderTest;
import edu.columbia.gemma.loader.expression.arrayDesign.ArrayDesignParserTest;
import edu.columbia.gemma.loader.expression.arrayDesign.IlluminaProbeReaderTest;
import edu.columbia.gemma.loader.expression.arrayExpress.DataFileFetcherTest;
import edu.columbia.gemma.loader.expression.geo.GeoDatasetServiceTest;
import edu.columbia.gemma.loader.expression.geo.GeoFamilyParserTest;
import edu.columbia.gemma.loader.expression.mage.MageLoadTest;
import edu.columbia.gemma.loader.expression.mage.MageMLParserTest;
import edu.columbia.gemma.loader.expression.smd.SMDManagerImplTest;
import edu.columbia.gemma.loader.genome.gene.GeneParserTest;
import edu.columbia.gemma.loader.loaderutils.BeanPropertyCompleterTest;
import edu.columbia.gemma.loader.smd.model.ExptMetaTest;
import edu.columbia.gemma.loader.smd.model.PublicationMetaTest;
import edu.columbia.gemma.security.interceptor.PersistAclInterceptorTest;
import edu.columbia.gemma.sequence.QtlDaoImplTest;
import edu.columbia.gemma.tools.GoldenPathTest;
import edu.columbia.gemma.tools.SequenceManipulationTest;
import edu.columbia.gemma.web.controller.common.auditAndSecurity.SignupControllerTest;
import edu.columbia.gemma.web.controller.entrez.pubmed.PubMedArticleListControllerTest;
import edu.columbia.gemma.web.controller.entrez.pubmed.PubMedXmlControllerTest;
import edu.columbia.gemma.web.controller.expression.arrayDesign.ArrayDesignControllerTest;
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

        // common.auditAndSecurity
        suite.addTestSuite( AuditTrailDaoTest.class );
        suite.addTestSuite( UserRoleServiceImplTest.class );
        suite.addTestSuite( UserServiceImplTest.class );
        suite.addTestSuite( UserDaoImplTest.class );

        // common.description
        suite.addTestSuite( BibliographicReferenceDaoImplTest.class );
        suite.addTestSuite( BibliographicReferenceServiceImplTest.class );
        suite.addTestSuite( DatabaseEntryDaoImplTest.class );
        suite.addTestSuite( ExternalDatabaseServiceImplTest.class );

        // common.protocol
        suite.addTestSuite( ProtocolServiceTest.class );

        // expression.arraydesign
        suite.addTestSuite( ArrayDesignServiceImplTest.class );

        // expression.experiment
        suite.addTestSuite( ExpressionExperimentServiceImplTest.class );

        // externalDb -- test is in wrong place
        suite.addTestSuite( ExternalDatabaseTest.class );

        // genome.gene
        suite.addTestSuite( CandidateGeneImplTest.class );
        suite.addTestSuite( CandidateGeneListDAOImplTest.class );
        suite.addTestSuite( CandidateGeneListImplTest.class );
        suite.addTestSuite( CandidateGeneListServiceImplTest.class );
        suite.addTestSuite( GeneServiceImplTest.class );

        // genome.sequenceAnalysis
        suite.addTestSuite( BlatResultImplTest.class );

        // loader.association
        suite.addTestSuite( Gene2GOAssociationParserTest.class );

        // loader.description
        suite.addTestSuite( OntologyEntryLoaderTest.class );

        // loader.entrez.pubmed
        suite.addTestSuite( PubMedXMLFetcherTest.class );
        suite.addTestSuite( PubMedXMLParserTest.class );

        // loader.expression.arrayDesign
        suite.addTestSuite( AffyProbeReaderTest.class );
        suite.addTestSuite( ArrayDesignParserTest.class );
        suite.addTestSuite( IlluminaProbeReaderTest.class );

        // loader.expression.arrayExpress
        suite.addTestSuite( DataFileFetcherTest.class );

        // loader.expression.geo
        suite.addTestSuite( GeoDatasetServiceTest.class );
        suite.addTestSuite( GeoFamilyParserTest.class );

        // loader.expression.mage
        suite.addTestSuite( MageMLParserTest.class );
        suite.addTestSuite( MageLoadTest.class );

        // loader.expression.smd
        suite.addTestSuite( SMDManagerImplTest.class );

        // loader.expression.smd.model
        suite.addTestSuite( ExptMetaTest.class );
        suite.addTestSuite( PublicationMetaTest.class );

        // loader.genome.gene
        suite.addTestSuite( GeneParserTest.class );

        // loader.loaderutils
        suite.addTestSuite( BeanPropertyCompleterTest.class );

        // security.interceptor
        suite.addTestSuite( PersistAclInterceptorTest.class );

        // sequence
        suite.addTestSuite( QtlDaoImplTest.class );

        // tools
        suite.addTestSuite( GoldenPathTest.class );
        suite.addTestSuite( SequenceManipulationTest.class );

        // web.controller.common.auditAndSecurity
        suite.addTestSuite( SignupControllerTest.class );

        // web.controller.entrez.pubmed
        suite.addTestSuite( PubMedXmlControllerTest.class );
        suite.addTestSuite( PubMedArticleListControllerTest.class );

        // web.controller.expression.arrayDesign
        suite.addTestSuite( ArrayDesignControllerTest.class );

        // web.controller.flow.action.entrez.pubmed
        suite.addTestSuite( GetPubMedActionTests.class );

        // web.controller.flow.entrez.pubmed
        suite.addTestSuite( SearchPubMedFlowTests.class );
        suite.addTestSuite( DetailBibRefFlowTests.class );
        // $JUnit-END$

        return suite;
    }
}
