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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Tests the {@link DifferentialExpessionAnalysis} tool.
 * 
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionAnalysisTest extends BaseAnalyzerConfigurationTest {

    private Log log = LogFactory.getLog( this.getClass() );

    DifferentialExpressionAnalyzer analysis = null;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.diff.BaseAnalyzerConfigurationTest#onSetUpInTransaction()
     */
    @Override
    public void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

        analysis = ( DifferentialExpressionAnalyzer ) this.getBean( "differentialExpressionAnalysis" );

    }

    /**
     * Works with GSE22, GSE23.
     */
    // /**
    // * Determines the analysis for GSE23.
    // * <p>
    // * Expected Analyzer: {@link OneWayAnovaAnalyzer}
    // */
    // public void testDetermineAnalysisForGSE23() {
    // ExpressionExperimentService service = ( ExpressionExperimentService ) this
    // .getBean( "expressionExperimentService" );
    // expressionExperiment = service.findByShortName( "GSE23" );
    // Collection<QuantitationType> quantitationTypes = expressionExperiment.getQuantitationTypes();
    //
    // for ( QuantitationType qt : quantitationTypes ) {
    // log.debug( qt );
    // if ( qt.getIsBackground() ) {
    // quantitationType = qt;
    // break;
    // }
    // }
    // assertNotNull( quantitationType );
    // Collection<DesignElementDataVector> vectors = expressionExperiment.getDesignElementDataVectors();
    // bioAssayDimension = vectors.iterator().next().getBioAssayDimension();
    //
    // AbstractAnalyzer analyzer = analysis.determineAnalysis( expressionExperiment, quantitationType,
    // bioAssayDimension );
    // assertEquals( analyzer.getClass(), OneWayAnovaAnalyzer.class );
    //
    // ExpressionAnalysis expressionAnalysis = analyzer.getExpressionAnalysis( expressionExperiment, quantitationType,
    // bioAssayDimension );
    //
    // Collection<ExpressionAnalysisResult> results = expressionAnalysis.getAnalysisResults();
    // for ( ExpressionAnalysisResult result : results ) {
    // log.info( "p-value: " + result.getPvalue() + ", score: " + result.getScore() );
    // }
    // }
    /**
     * * Tests determineAnalysis.
     * <p>
     * 2 experimental factors
     * <p>
     * 2 factor value / experimental factor
     * <p>
     * complete block design and biological replicates
     * <p>
     * Expected analyzer: {@link TwoWayAnovaWithInteractionsAnalyzer}
     * 
     * @throws Exception
     */
    public void testDetermineAnalysisA() throws Exception {
        configureMocks();
        AbstractAnalyzer analyzer = analysis.determineAnalysis( expressionExperiment );
        assertTrue( analyzer instanceof TwoWayAnovaWithInteractionsAnalyzer );
    }

    /**
     * Tests determineAnalysis.
     * <p>
     * 2 experimental factors
     * <p>
     * 2 factor value / experimental factor
     * <p>
     * no replicates
     * <p>
     * Expected analyzer: {@link TwoWayAnovaWithoutInteractionsAnalyzer}
     * 
     * @throws Exception
     */
    public void testDetermineAnalysisB() throws Exception {
        super.configureTestDataForTwoWayAnovaWithoutInteractions();
        configureMocks();
        AbstractAnalyzer analyzer = analysis.determineAnalysis( expressionExperiment );
        assertTrue( analyzer instanceof TwoWayAnovaWithoutInteractionsAnalyzer );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.diff.BaseAnalyzerConfigurationTest#configureMocks()
     */
    @Override
    protected void configureMocks() throws Exception {

        configureMockAnalysisServiceHelper( 2 );

        AnalyzerHelper analyzerHelper = ( AnalyzerHelper ) this.getBean( "analyzerHelper" );
        analyzerHelper.setAnalysisHelperService( analysisHelperService );
        analysis.setAnalyzerHelper( analyzerHelper );

    }

}
