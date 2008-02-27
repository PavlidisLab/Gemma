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
package ubic.gemma.model.analysis;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import ubic.gemma.model.expression.analysis.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.expression.analysis.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionAnalysisResultServiceTest extends BaseSpringContextTest {

    private DifferentialExpressionAnalysisResultService analysisResultService = null;

    private DifferentialExpressionAnalysisService analysisService = null;

    private ExpressionExperimentService expressionExperimentService = null;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.testing.BaseSpringContextTest#onSetUpInTransaction()
     */
    public void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

        this.analysisResultService = ( DifferentialExpressionAnalysisResultService ) getBean( "differentialExpressionAnalysisResultService" );

        this.analysisService = ( DifferentialExpressionAnalysisService ) getBean( "differentialExpressionAnalysisService" );

        this.expressionExperimentService = ( ExpressionExperimentService ) getBean( "expressionExperimentService" );

    }

    /**
     * Tests getting the collection of factor values for the result.
     */
    @SuppressWarnings("unchecked")
    public void testGetFactorValues() {
        String shortName = "GSE2018";
        ExpressionExperiment ee = expressionExperimentService.findByShortName( shortName );
        if ( ee == null ) {
            log.error( "Could not find experiment for " + shortName + ". Skipping test ..." );
            return;
        }
        Collection<DifferentialExpressionAnalysis> analyses = analysisService.findByInvestigation( ee );
        if ( analyses == null || !( analyses.iterator().hasNext() ) ) {
            log.error( "Could not find analyses for " + shortName + ". Skipping test ..." );
            return;
        }

        DifferentialExpressionAnalysis a = analyses.iterator().next();
        Collection<ExpressionAnalysisResultSet> resultSets = a.getResultSets();

        assertEquals( 1, resultSets.size() );

        ExpressionAnalysisResultSet rs = resultSets.iterator().next();

        Collection<DifferentialExpressionAnalysisResult> results = rs.getResults();

        DifferentialExpressionAnalysisResult r = results.iterator().next();

        Collection<FactorValue> fvs = analysisResultService.getFactorValues( r );
        log.debug( "Num factor values: " + fvs.size() );
        assertEquals( 2, fvs.size() );

    }

    /**
     * Tests getting the map of factors keyed by results
     */
    @SuppressWarnings("unchecked")
    public void testGetMapOfFactorValues() {
        String shortName = "GSE2018";
        ExpressionExperiment ee = expressionExperimentService.findByShortName( shortName );
        if ( ee == null ) {
            log.error( "Could not find experiment for " + shortName + ".  Skipping test ..." );
            return;
        }
        Collection<DifferentialExpressionAnalysis> analyses = analysisService.findByInvestigation( ee );
        if ( analyses == null || !( analyses.iterator().hasNext() ) ) {
            log.error( "Could not find analyses for " + shortName + ".  Skipping test ..." );
            return;
        }

        DifferentialExpressionAnalysis a = analyses.iterator().next();
        Collection<ExpressionAnalysisResultSet> resultSets = a.getResultSets();

        assertEquals( 1, resultSets.size() );

        ExpressionAnalysisResultSet rs = resultSets.iterator().next();

        Collection<DifferentialExpressionAnalysisResult> results = rs.getResults();
        Iterator<DifferentialExpressionAnalysisResult> iter = results.iterator();

        Collection<DifferentialExpressionAnalysisResult> testResults = new HashSet<DifferentialExpressionAnalysisResult>();
        int testResultsSize = 3;

        for ( int i = 0; i < testResultsSize; i++ ) {
            testResults.add( iter.next() );
        }

        Map<DifferentialExpressionAnalysisResult, Collection<FactorValue>> fvs = analysisResultService
                .getFactorValues( testResults );

        Collection<DifferentialExpressionAnalysisResult> diffResultKeys = fvs.keySet();

        for ( DifferentialExpressionAnalysisResult d : diffResultKeys ) {

            Collection<FactorValue> factorValues = fvs.get( d );

            log.debug( "result key: " + d.getPvalue() + " has " + factorValues.size() );

            for ( FactorValue f : factorValues ) {
                log.debug( "value in map :" + f.getId() );
            }
        }
        assertEquals( testResultsSize, fvs.size() );
    }

}
