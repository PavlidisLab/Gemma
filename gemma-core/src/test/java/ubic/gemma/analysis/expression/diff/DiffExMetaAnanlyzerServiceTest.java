/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.analysis.expression.diff;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.HashSet;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysis;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisResult;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * Currently this test requires the 'test' miniGemma DB.
 * 
 * @author Paul
 * @version $Id$
 */
public class DiffExMetaAnanlyzerServiceTest extends BaseSpringContextTest {

    @Autowired
    private ExpressionExperimentService experimentService;

    @Autowired
    private DiffExMetaAnalyzerService analyzerService;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    @Test
    public void testAnalyze() {

        ExpressionExperiment ds1 = experimentService.findByShortName( "GSE2018" );
        ExpressionExperiment ds2 = experimentService.findByShortName( "GSE6344" );
        ExpressionExperiment ds3 = experimentService.findByShortName( "GSE2111" );
        assertNotNull( ds1 );
        assertNotNull( ds2 );
        assertNotNull( ds3 );
        Collection<DifferentialExpressionAnalysis> ds1Analyses = differentialExpressionAnalysisService
                .findByInvestigation( ds1 );
        Collection<DifferentialExpressionAnalysis> ds2Analyses = differentialExpressionAnalysisService
                .findByInvestigation( ds2 );
        Collection<DifferentialExpressionAnalysis> ds3Analyses = differentialExpressionAnalysisService
                .findByInvestigation( ds3 );
        assertTrue( !ds1Analyses.isEmpty() );
        assertTrue( !ds2Analyses.isEmpty() );
        assertTrue( !ds3Analyses.isEmpty() );
        differentialExpressionAnalysisService.thaw( ds1Analyses );
        differentialExpressionAnalysisService.thaw( ds2Analyses );
        differentialExpressionAnalysisService.thaw( ds3Analyses );
        ExpressionAnalysisResultSet rs1 = ds1Analyses.iterator().next().getResultSets().iterator().next();
        ExpressionAnalysisResultSet rs2 = ds2Analyses.iterator().next().getResultSets().iterator().next();
        ExpressionAnalysisResultSet rs3 = ds3Analyses.iterator().next().getResultSets().iterator().next();
		Collection<Long> analysisResultSetIds = new HashSet<Long>();
		analysisResultSetIds.add( rs1.getId() );
		analysisResultSetIds.add( rs2.getId() );
		analysisResultSetIds.add( rs3.getId() );
        /*
         * Perform the meta-analysis without saving it.
         */
		GeneDifferentialExpressionMetaAnalysis metaAnalysis = analyzerService.analyze( analysisResultSetIds, null, null );
        assertNotNull( metaAnalysis );
        assertEquals( 3, metaAnalysis.getResultSetsIncluded().size() );

        // not checked by hand; may change if we fix storage of contrasts for these test data set.
        assertEquals( 358, metaAnalysis.getResults().size() );

        for ( GeneDifferentialExpressionMetaAnalysisResult r : metaAnalysis.getResults() ) {
            assertTrue( r.getMetaPvalue() <= 1.0 && r.getMetaPvalue() >= 0.0 );
        }

    }

}
