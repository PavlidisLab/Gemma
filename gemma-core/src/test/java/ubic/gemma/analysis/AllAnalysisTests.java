/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.analysis;

import ubic.gemma.analysis.diff.AllDifferentialExpressionTests;
import ubic.gemma.analysis.preprocess.AllPreProcessTests;
import ubic.gemma.analysis.report.ArrayDesignReportServiceTest;
import ubic.gemma.analysis.sequence.AllSequenceTests;
import ubic.gemma.analysis.service.CompositeSequenceGeneMapperServiceIntegrationTest;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author paul
 * @version $Id$
 */
public class AllAnalysisTests {

    public static Test suite() {
        TestSuite suite = new TestSuite( "Tests for ubic.gemma.analysis.*" );
        suite.addTest( AllDifferentialExpressionTests.suite() );
        suite.addTest( AllPreProcessTests.suite() );
        suite.addTest( AllSequenceTests.suite() );
        suite.addTestSuite( ArrayDesignReportServiceTest.class );
        suite.addTestSuite( CompositeSequenceGeneMapperServiceIntegrationTest.class );
        return suite;
    }

}
