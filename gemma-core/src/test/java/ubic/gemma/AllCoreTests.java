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
package ubic.gemma;

import junit.framework.Test;
import junit.framework.TestSuite;
import ubic.gemma.analysis.AnalysisServiceTest;
import ubic.gemma.analysis.diff.AllDifferentialExpressionTests;
import ubic.gemma.analysis.preprocess.AllPreProcessTests;
import ubic.gemma.analysis.sequence.ProbeMapperTest;
import ubic.gemma.apps.ProbeMapperCliTest;
import ubic.gemma.datastructure.matrix.AllExpressionMatrixTests;
import ubic.gemma.externalDb.ExternalDatabaseTest;
import ubic.gemma.loader.association.NCBIGene2GOAssociationParserTest;
import ubic.gemma.loader.entrez.pubmed.AllPubMedTests;
import ubic.gemma.loader.expression.arrayDesign.AllArrayDesignTests;
import ubic.gemma.loader.expression.arrayExpress.DataFileFetcherIntegrationTest;
import ubic.gemma.loader.expression.geo.AllGeoTests;
import ubic.gemma.loader.expression.mage.AllMageTests;
import ubic.gemma.loader.expression.smd.AllSmdTests;
import ubic.gemma.loader.genome.AllGenomeTests;
import ubic.gemma.loader.util.HttpFetcherTest;
import ubic.gemma.loader.util.fetcher.AbstractFetcherTest;
import ubic.gemma.model.AllModelTests;
import ubic.gemma.ontology.AllOntologyTests;
import ubic.gemma.persistence.AllPersistenceTests;
import ubic.gemma.persistence.GenomePersisterTest;
import ubic.gemma.persistence.PersisterTest;
import ubic.gemma.scheduler.SchedulerFactoryBeanTest;
import ubic.gemma.security.AllSecurityTests;
import ubic.gemma.util.BusinessKeyTest;
import ubic.gemma.util.TaxonUtilityTest;
import ubic.gemma.util.gemmaspaces.GemmaSpacesUtilTest;
import ubic.gemma.util.progress.ProgressAppenderTest;
import ubic.gemma.util.progress.ProgressIntegrationTest;
import ubic.gemma.visualization.ExpressionDataMatrixVisualizationServiceTest;

/**
 * Tests for gemma-core
 * 
 * @author pavlidis
 * @version $Id$
 */
public class AllCoreTests {

    public static Test suite() {

        TestSuite suite = new TestSuite( "Tests for gemma-core" );

        suite.addTestSuite( ProgressIntegrationTest.class );

        suite.addTest( AllPubMedTests.suite() );
        suite.addTest( AllGeoTests.suite() );
        suite.addTest( AllMageTests.suite() );
        suite.addTest( AllSmdTests.suite() );

        suite.addTest( AllArrayDesignTests.suite() );
        suite.addTest( AllPreProcessTests.suite() );
        suite.addTest( AllDifferentialExpressionTests.suite() );
        suite.addTest( AllGenomeTests.suite() );
        suite.addTest( AllModelTests.suite() );

        suite.addTest( AllSecurityTests.suite() );
        suite.addTest( AllPersistenceTests.suite() );

        suite.addTest( AllExpressionMatrixTests.suite() );

        suite.addTest( AllOntologyTests.suite() );

        suite.addTestSuite( AnalysisServiceTest.class);
        
        suite.addTestSuite( HttpFetcherTest.class );
        suite.addTestSuite( AbstractFetcherTest.class );

        suite.addTestSuite( ExternalDatabaseTest.class );
        suite.addTestSuite( ProbeMapperCliTest.class );
        suite.addTestSuite( ProbeMapperTest.class );

        suite.addTestSuite( NCBIGene2GOAssociationParserTest.class );
        suite.addTestSuite( DataFileFetcherIntegrationTest.class );

        suite.addTestSuite( ExpressionDataMatrixVisualizationServiceTest.class );

        suite.addTestSuite( PersisterTest.class );
        suite.addTestSuite( GenomePersisterTest.class );
        suite.addTestSuite( SchedulerFactoryBeanTest.class );

        suite.addTestSuite( ProgressAppenderTest.class );
        suite.addTestSuite( ProgressIntegrationTest.class );
        suite.addTestSuite( TaxonUtilityTest.class );
        suite.addTestSuite( BusinessKeyTest.class );
        suite.addTestSuite( GemmaSpacesUtilTest.class );

        System.out.print( "----------------------\nGemma Core Tests\n" + suite.countTestCases()
                + " Tests to run\n----------------------\n" );

        return suite;
    }

}
