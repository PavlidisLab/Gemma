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
package ubic.gemma.core.analysis.expression.diff;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link differentialExpressionAnalysisHelperService}.
 * 
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionAnalysisHelperServiceTest extends BaseAnalyzerConfigurationTest {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.core.analysis.diff.BaseAnalyzerConfigurationTest#configureMocks()
     */
    @Override
    @Before
    public void configureMocks() throws Exception {
        configureMockAnalysisServiceHelper( 1 );
    }

    /**
     * Tests the AnalyzerHelper.checkBiologicalReplicates method.
     * <p>
     * Expected result: null exception
     */
    @Test
    public void testCheckBiologicalReplicates() {
        boolean result = DifferentialExpressionAnalysisUtil.checkBiologicalReplicates( expressionExperiment,
                expressionExperiment.getExperimentalDesign().getExperimentalFactors() );
        assertTrue( result );
    }

}