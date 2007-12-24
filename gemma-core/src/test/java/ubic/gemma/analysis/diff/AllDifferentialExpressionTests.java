/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.analysis.diff;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * A test suite for the differential analysis tool.
 * 
 * @author keshav
 * @version $Id$
 */
public class AllDifferentialExpressionTests {

    public static Test suite() {
        TestSuite suite = new TestSuite( "Test for ubic.gemma.analysis.diff" );
        // $JUnit-BEGIN$
        suite.addTestSuite( AnalyzerHelperTest.class );
        suite.addTestSuite( OneWayAnovaAnalyzerTest.class );
        suite.addTestSuite( TTestAnalyzerTest.class );
        suite.addTestSuite( TwoWayAnovaWithoutInteractionsAnalyzerTest.class );
        suite.addTestSuite( TwoWayAnovaWithInteractionsAnalyzerTest.class );
        suite.addTestSuite( DifferentialExpressionAnalyzerTest.class );
        // no tests in these ...they are extended
        // suite.addTestSuite( BaseAnalyzerConfigurationTest.class );
        // $JUnit-END$
        return suite;
    }

}
