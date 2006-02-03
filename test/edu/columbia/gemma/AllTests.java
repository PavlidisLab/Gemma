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
package edu.columbia.gemma;

import junit.framework.Test;
import junit.framework.TestSuite;
import edu.columbia.gemma.analysis.preprocess.QuantileNormalizerTest;
import edu.columbia.gemma.analysis.preprocess.RMABackgroundAdjusterTest;
import edu.columbia.gemma.analysis.preprocess.RMATest;
import edu.columbia.gemma.analysis.preprocess.TwoColorArrayLoessNormalizerTest;
import edu.columbia.gemma.common.auditAndSecurity.AuditTrailDaoTest;
import edu.columbia.gemma.common.auditAndSecurity.UserDaoImplTest;
import edu.columbia.gemma.common.auditAndSecurity.UserRoleServiceImplTest;
import edu.columbia.gemma.common.auditAndSecurity.UserServiceImplTest;
import edu.columbia.gemma.common.description.BibliographicReferenceDaoImplTest;
import edu.columbia.gemma.common.description.BibliographicReferenceServiceImplTest;
import edu.columbia.gemma.common.description.DatabaseEntryDaoImplTest;
import edu.columbia.gemma.common.description.ExternalDatabaseServiceImplTest;
import edu.columbia.gemma.common.description.LocalFileServiceImplTest;
import edu.columbia.gemma.common.protocol.ProtocolServiceTest;
import edu.columbia.gemma.expression.experiment.ExpressionExperimentServiceImplTest;
import edu.columbia.gemma.externalDb.ExternalDatabaseTest;
import edu.columbia.gemma.genome.gene.CandidateGeneImplTest;
import edu.columbia.gemma.genome.gene.CandidateGeneListDAOImplTest;
import edu.columbia.gemma.genome.gene.CandidateGeneListImplTest;
import edu.columbia.gemma.genome.gene.CandidateGeneListServiceImplTest;
import edu.columbia.gemma.genome.gene.GeneServiceImplTest;
import edu.columbia.gemma.genome.sequenceAnalysis.BlatResultImplTest;
import edu.columbia.gemma.loader.association.Gene2GOAssociationParserTest;
import edu.columbia.gemma.loader.description.OntologyEntryLoaderIntegrationTest;
import edu.columbia.gemma.loader.entrez.pubmed.PubMedXMLFetcherTest;
import edu.columbia.gemma.loader.entrez.pubmed.PubMedXMLParserTest;
import edu.columbia.gemma.loader.expression.arrayDesign.AffyProbeReaderTest;
import edu.columbia.gemma.loader.expression.arrayDesign.ArrayDesignParserIntegrationTest;
import edu.columbia.gemma.loader.expression.arrayDesign.IlluminaProbeReaderTest;
import edu.columbia.gemma.loader.expression.arrayExpress.DataFileFetcherTest;
import edu.columbia.gemma.loader.expression.geo.GeoConverterTest;
import edu.columbia.gemma.loader.expression.geo.GeoFamilyParserTest;
import edu.columbia.gemma.loader.expression.geo.RawDataFetcherTest;
import edu.columbia.gemma.loader.expression.mage.MageLoadTest;
import edu.columbia.gemma.loader.expression.mage.MageMLParserTest;
import edu.columbia.gemma.loader.expression.smd.SMDManagerImplTest;
import edu.columbia.gemma.loader.genome.gene.ncbi.NCBIGeneParserTest;
import edu.columbia.gemma.loader.loaderutils.BeanPropertyCompleterTest;
import edu.columbia.gemma.loader.smd.model.ExptMetaTest;
import edu.columbia.gemma.loader.smd.model.PublicationMetaTest;
import edu.columbia.gemma.security.SecurityIntegrationTest;
import edu.columbia.gemma.security.interceptor.AuditInterceptor;
import edu.columbia.gemma.security.interceptor.PersistAclInterceptorTest;
import edu.columbia.gemma.sequence.QtlDaoImplTest;
import edu.columbia.gemma.tools.AffyBatchTest;
import edu.columbia.gemma.tools.BlatTest;
import edu.columbia.gemma.tools.GoldenPathTest;
import edu.columbia.gemma.tools.MArrayRawTest;
import edu.columbia.gemma.tools.SequenceManipulationTest;
import edu.columbia.gemma.web.controller.common.auditAndSecurity.SignupControllerTest;
import edu.columbia.gemma.web.controller.expression.arrayDesign.ArrayDesignControllerTest;
import edu.columbia.gemma.web.flow.bibref.DetailBibRefFlowTests;
import edu.columbia.gemma.web.flow.bibref.SearchPubMedFlowTests;

/**
 * Combines all the tests. Tests that require resources that might not be available during a test (e.g., a network
 * device or web site) should not fail under those conditions. Integration tests should clean up after themselves
 * (delete entries put into the database, in particular).
 * 
 * @author pavlidis
 * @author keshav
 * @version $Id$
 */
public class AllTests {

    public static Test suite() {

        TestSuite suite = new TestSuite( "Test for edu.columbia.gemma" );
        // $JUnit-BEGIN$

        // analysis
        suite.addTestSuite( QuantileNormalizerTest.class );
        suite.addTestSuite( RMATest.class );
        suite.addTestSuite( RMABackgroundAdjusterTest.class );
        suite.addTestSuite( TwoColorArrayLoessNormalizerTest.class );

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
        suite.addTestSuite( LocalFileServiceImplTest.class );

        // common.protocol
        suite.addTestSuite( ProtocolServiceTest.class );

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
        suite.addTestSuite( OntologyEntryLoaderIntegrationTest.class );

        // loader.entrez.pubmed
        suite.addTestSuite( PubMedXMLFetcherTest.class );
        suite.addTestSuite( PubMedXMLParserTest.class );

        // loader.expression.arrayDesign
        suite.addTestSuite( AffyProbeReaderTest.class );
        suite.addTestSuite( ArrayDesignParserIntegrationTest.class );
        suite.addTestSuite( IlluminaProbeReaderTest.class );

        // loader.expression.arrayExpress
        suite.addTestSuite( DataFileFetcherTest.class );

        // loader.expression.geo
        // suite.addTestSuite( GeoDatasetServiceIntegrationTest.class );
        suite.addTestSuite( GeoFamilyParserTest.class );
        suite.addTestSuite( RawDataFetcherTest.class );
        suite.addTestSuite( GeoConverterTest.class );

        // loader.expression.mage
        suite.addTestSuite( MageMLParserTest.class );
        suite.addTestSuite( MageLoadTest.class );

        // loader.expression.smd
        suite.addTestSuite( SMDManagerImplTest.class );

        // loader.expression.smd.model
        suite.addTestSuite( ExptMetaTest.class );
        suite.addTestSuite( PublicationMetaTest.class );

        // loader.genome.gene
        suite.addTestSuite( NCBIGeneParserTest.class );

        // loader.loaderutils
        suite.addTestSuite( BeanPropertyCompleterTest.class );

        // security
        suite.addTestSuite( SecurityIntegrationTest.class );

        // security.interceptor
        suite.addTestSuite( PersistAclInterceptorTest.class );
        suite.addTestSuite( AuditInterceptor.class );

        // sequence
        suite.addTestSuite( QtlDaoImplTest.class );

        // tools
        suite.addTestSuite( AffyBatchTest.class );
        suite.addTestSuite( GoldenPathTest.class );
        suite.addTestSuite( SequenceManipulationTest.class );
        suite.addTestSuite( BlatTest.class );
        suite.addTestSuite( MArrayRawTest.class );

        // web.controller.common.auditAndSecurity
        suite.addTestSuite( SignupControllerTest.class );

        // web.controller.entrez.pubmed
        suite.addTestSuite( ArrayDesignControllerTest.class );

        // web.controller.flow.entrez.pubmed
        suite.addTestSuite( SearchPubMedFlowTests.class );
        suite.addTestSuite( DetailBibRefFlowTests.class );
        // $JUnit-END$

        return suite;
    }
}
