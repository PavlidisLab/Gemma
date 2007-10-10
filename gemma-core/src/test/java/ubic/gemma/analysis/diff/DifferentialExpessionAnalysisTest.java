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

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;

/**
 * Tests the {@link DifferentialExpessionAnalysis} tool.
 * 
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpessionAnalysisTest extends BaseAnalyzerConfigurationTest {

    private Log log = LogFactory.getLog( this.getClass() );

    DifferentialExpressionAnalysis analysis = null;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.diff.BaseAnalyzerConfigurationTest#onSetUpInTransaction()
     */
    @Override
    public void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

        analysis = new DifferentialExpressionAnalysis();
    }

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
    // analyzer.getPValues( expressionExperiment, quantitationType, bioAssayDimension );
    // }

    /**
     * Tests determineAnalysis.
     * <p>
     * 2 experimental factors
     * <p>
     * 2 factor value / experimental factor
     * <p>
     * complete block design and biological replicates
     * <p>
     * Expected analyzer: {@link TwoWayAnovaWithInteractionsAnalyzer}
     */
    public void testDetermineAnalysisA() {
        AbstractAnalyzer analyzer = analysis.determineAnalysis( expressionExperiment, quantitationType,
                bioAssayDimension );
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
     */
    public void testDetermineAnalysisB() {
        super.configureTestDataForTwoWayAnovaWithoutInteractions();
        AbstractAnalyzer analyzer = analysis.determineAnalysis( expressionExperiment, quantitationType,
                bioAssayDimension );
        assertTrue( analyzer instanceof TwoWayAnovaWithoutInteractionsAnalyzer );
    }

}
