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
import ubic.gemma.analysis.AllAnalysisTests;
import ubic.gemma.analysis.report.ArrayDesignReportServiceTest;
import ubic.gemma.datastructure.matrix.AllExpressionMatrixTests;
import ubic.gemma.externalDb.GoldenPathQueryTest;
import ubic.gemma.loader.AllLoaderTests;
import ubic.gemma.model.AllModelTests;
import ubic.gemma.ontology.AllOntologyTests;
import ubic.gemma.persistence.AllPersistenceTests;
import ubic.gemma.scheduler.SchedulerFactoryBeanTest;
import ubic.gemma.security.AllSecurityTests;
import ubic.gemma.util.BusinessKeyTest;
import ubic.gemma.util.TaxonUtilityTest;
import ubic.gemma.util.grid.SpacesUtilTest;
import ubic.gemma.util.progress.ProgressAppenderTest;
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

        suite.addTest( AllAnalysisTests.suite() );
        suite.addTest( AllExpressionMatrixTests.suite() );
        suite.addTest( AllLoaderTests.suite() );
        suite.addTest( AllModelTests.suite() );
        suite.addTest( AllOntologyTests.suite() );
        suite.addTest( AllPersistenceTests.suite() );
        suite.addTest( AllSecurityTests.suite() );

        // tests in externalDb
        suite.addTestSuite( GoldenPathQueryTest.class );

        // tests in scheduler
        suite.addTestSuite( SchedulerFactoryBeanTest.class );

        // tests in
        suite.addTestSuite( ArrayDesignReportServiceTest.class );

        // tests in util.
        suite.addTestSuite( TaxonUtilityTest.class );
        suite.addTestSuite( BusinessKeyTest.class );
        suite.addTestSuite( SpacesUtilTest.class );
        suite.addTestSuite( ProgressAppenderTest.class );

        // tests in visualization
        suite.addTestSuite( ExpressionDataMatrixVisualizationServiceTest.class );

        System.out.print( "----------------------\nGemma Core Tests\n" + suite.countTestCases()
                + " Tests to run\n----------------------\n" );

        return suite;
    }

}
