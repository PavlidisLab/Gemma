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
package ubic.gemma.analysis.preprocess;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test for ubic.gemma.analysis.preprocess
 * 
 * @author pavlidis
 * @version $Id$
 */
public class AllPreProcessTests {

    public static Test suite() {
        TestSuite suite = new TestSuite( "Test for ubic.gemma.analysis.preprocess" );
        // $JUnit-BEGIN$
        suite.addTestSuite( RMATest.class );
        suite.addTestSuite( QuantileNormalizerTest.class );
        suite.addTestSuite( TwoColorArrayLoessNormalizerTest.class );
        suite.addTestSuite( RMABackgroundAdjusterTest.class );
        suite.addTestSuite( TwoChannelMissingValuesTest.class );
        suite.addTestSuite( ProcessedExpressionDataCreateServiceTest.class );
        // $JUnit-END$
        return suite;
    }

}
