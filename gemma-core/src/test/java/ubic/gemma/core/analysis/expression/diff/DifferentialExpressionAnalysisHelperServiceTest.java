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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ubic.gemma.core.util.test.category.SlowTest;

import static org.junit.Assert.assertTrue;

/**
 * @author keshav
 */
public class DifferentialExpressionAnalysisHelperServiceTest extends BaseAnalyzerConfigurationTest {

    /*
     * Tests the AnalyzerHelper.checkBiologicalReplicates method.
     * Expected result: null exception
     */
    @Test
    public void testCheckBiologicalReplicates() {
        boolean result = DifferentialExpressionAnalysisUtil.checkBiologicalReplicates( expressionExperiment,
                expressionExperiment.getExperimentalDesign().getExperimentalFactors() );
        assertTrue( result );
    }

}
