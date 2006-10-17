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
import ubic.gemma.analysis.preprocess.AllPreProcessTests;
import ubic.gemma.analysis.sequence.ProbeMapperTest;
import ubic.gemma.apps.ProbeMapperCliTest;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrixTest;
import ubic.gemma.externalDb.ExternalDatabaseTest;
import ubic.gemma.loader.association.NCBIGene2GOAssociationParserTest;
import ubic.gemma.loader.description.OntologyEntryLoaderIntegrationTest;
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
import ubic.gemma.model.expression.experiment.ExpressionExperimentDeleteTest;
import ubic.gemma.persistence.CrudUtilsTest;
import ubic.gemma.persistence.GenomePersisterTest;
import ubic.gemma.persistence.PersisterTest;
import ubic.gemma.security.AllSecurityTests;
import ubic.gemma.util.progress.ProgressIntegrationTest;
import ubic.gemma.visualization.ExpressionDataMatrixVisualizationTest;

/**
 * Tests for gemma-core
 * 
 * @author pavlidis
 * @version $Id$
 */
public class AllCoreTests {

    public static Test suite() {

        TestSuite suite = new TestSuite( "Tests for gemma-core" );

        suite.addTest( AllPubMedTests.suite() );
        suite.addTest( AllGeoTests.suite() );
        suite.addTest( AllMageTests.suite() );
        suite.addTest( AllSmdTests.suite() );

        suite.addTest( AllArrayDesignTests.suite() );
        suite.addTest( AllPreProcessTests.suite() );

        suite.addTest( AllGenomeTests.suite() );
        suite.addTest( AllModelTests.suite() );

        suite.addTestSuite( HttpFetcherTest.class );
        suite.addTestSuite( AbstractFetcherTest.class );

        suite.addTestSuite( ExternalDatabaseTest.class );

        suite.addTest( AllSecurityTests.suite() );

        suite.addTestSuite( ProbeMapperCliTest.class );
        suite.addTestSuite( ProbeMapperTest.class );

        suite.addTestSuite( NCBIGene2GOAssociationParserTest.class );
        suite.addTestSuite( OntologyEntryLoaderIntegrationTest.class );
        suite.addTestSuite( DataFileFetcherIntegrationTest.class );

        suite.addTestSuite( ExpressionDataMatrixTest.class );
        suite.addTestSuite( ExpressionDataMatrixVisualizationTest.class );

        suite.addTestSuite( ProgressIntegrationTest.class );
        suite.addTestSuite( PersisterTest.class );
        suite.addTestSuite( CrudUtilsTest.class );
        suite.addTestSuite( GenomePersisterTest.class );

        suite.addTestSuite( ExpressionExperimentDeleteTest.class );

        System.out.print( "----------------------\nGemma Core Tests\n" + suite.countTestCases()
                + " Tests to run\n----------------------\n" );

        return suite;
    }

}
