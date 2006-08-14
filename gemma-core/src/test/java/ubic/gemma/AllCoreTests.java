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
import ubic.gemma.loader.association.Gene2GOAssociationParserTest;
import ubic.gemma.loader.description.OntologyEntryLoaderIntegrationTest;
import ubic.gemma.loader.entrez.pubmed.AllPubMedTests;
import ubic.gemma.loader.expression.arrayDesign.AllArrayDesignTests;
import ubic.gemma.loader.expression.arrayExpress.DataFileFetcherTest;
import ubic.gemma.loader.expression.geo.AllGeoTests;
import ubic.gemma.loader.expression.mage.AllMageTests;
import ubic.gemma.loader.expression.smd.AllSmdTests;
import ubic.gemma.loader.genome.AllGenomeTests;
import ubic.gemma.loader.util.HttpFetcherTest;
import ubic.gemma.model.AllModelTests;
import ubic.gemma.security.SecurityIntegrationTest;
import ubic.gemma.security.interceptor.AuditInterceptorTest;
import ubic.gemma.security.interceptor.PersistAclInterceptorTest;
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

        suite.addTestSuite( ExternalDatabaseTest.class );

        suite.addTestSuite( AuditInterceptorTest.class );
        suite.addTestSuite( PersistAclInterceptorTest.class );
        suite.addTestSuite( SecurityIntegrationTest.class );

        suite.addTestSuite( ProbeMapperCliTest.class );
        suite.addTestSuite( ProbeMapperTest.class );

        suite.addTestSuite( Gene2GOAssociationParserTest.class );
        suite.addTestSuite( OntologyEntryLoaderIntegrationTest.class );
        suite.addTestSuite( DataFileFetcherTest.class );

        suite.addTestSuite( ExpressionDataMatrixTest.class );
        suite.addTestSuite( ExpressionDataMatrixVisualizationTest.class );

        System.out.print( "----------------------\nGemma Core Tests\n" + suite.countTestCases()
                + " Tests to run\n----------------------\n" );

        return suite;
    }

}
