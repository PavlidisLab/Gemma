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
package ubic.gemma.analysis.expression.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.analysis.expression.ExpressionAnalysis;
import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.ProbeAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * A two way anova base class as described by P. Pavlidis, Methods 31 (2003) 282-289.
 * <p>
 * See http://www.bioinformatics.ubc.ca/pavlidis/lab/docs/reprints/anova-methods.pdf.
 * <p>
 * For specific implementations with and without interactions, see the {@link TwoWayAnovaWithInteractionsAnalyzer} and
 * {@link TwoWayAnovaWithoutInteractionsAnalyzer} respectively.
 * 
 * @author keshav
 * @version $Id$
 */
public abstract class AbstractTwoWayAnovaAnalyzer extends AbstractDifferentialExpressionAnalyzer {
    private Log log = LogFactory.getLog( this.getClass() );

    private ExpressionExperiment ee = null;

    protected final int mainEffectAIndex = 0;
    protected final int mainEffectBIndex = 1;
    protected final int mainEffectInteractionIndex = 2;
    protected final int maxResults = 3;

    /**
     * Creates and returns an {@link ExpressionAnalysis} and fills in the expression analysis results.
     * 
     * @param dmatrix
     * @param mainEffectAPvalues
     * @param mainEffectBPvalues
     * @param interactionEffectPvalues
     * @param fStatistics
     * @param numResultsFromR
     * @param experimentalFactorA
     * @param experimentalFactorB
     * @param quantitationType
     * @return
     */
    protected DifferentialExpressionAnalysis createExpressionAnalysis( ExpressionDataDoubleMatrix dmatrix,
            double[] mainEffectAPvalues, double[] mainEffectBPvalues, double[] interactionEffectPvalues,
            double[] fStatistics, int numResultsFromR, ExperimentalFactor experimentalFactorA,
            ExperimentalFactor experimentalFactorB, QuantitationType quantitationType ) {

        Collection<ExpressionAnalysisResultSet> resultSets = new HashSet<ExpressionAnalysisResultSet>();

        // TODO pass the DifferentialExpressionAnalysisConfig in (see LinkAnalysisService)
        /* Create the expression analysis and pack the results. */
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        DifferentialExpressionAnalysis expressionAnalysis = config.toAnalysis();

        ExpressionExperimentSet eeSet = ExpressionExperimentSet.Factory.newInstance();
        Collection<BioAssaySet> experimentsAnalyzed = new HashSet<BioAssaySet>();
        experimentsAnalyzed.add( dmatrix.getExpressionExperiment() );
        eeSet.setExperiments( experimentsAnalyzed );
        expressionAnalysis.setExpressionExperimentSetAnalyzed( eeSet );

        /* All results for the first main effect */
        List<DifferentialExpressionAnalysisResult> analysisResultsMainEffectA = new ArrayList<DifferentialExpressionAnalysisResult>();

        /* All results for the second main effect */
        List<DifferentialExpressionAnalysisResult> analysisResultsMainEffectB = new ArrayList<DifferentialExpressionAnalysisResult>();

        /* Interaction effect */
        List<DifferentialExpressionAnalysisResult> analysisResultsInteractionEffect = new ArrayList<DifferentialExpressionAnalysisResult>();

        /* q-values */
        double[] mainEffectAQvalues = super.getQValues( mainEffectAPvalues );
        double[] mainEffectBQvalues = super.getQValues( mainEffectBPvalues );

        double[] interactionEffectQvalues = null;
        if ( interactionEffectPvalues != null ) {
            interactionEffectQvalues = super.getQValues( interactionEffectPvalues );
        }

        int k = 0;// statistics
        int l = 0;// main effect A
        int m = 0;// main effect B
        int n = 0;// interaction effect
        for ( int i = 0; i < dmatrix.rows(); i++ ) {

            DesignElement de = dmatrix.getDesignElementForRow( i );

            CompositeSequence cs = ( CompositeSequence ) de;

            for ( int j = 0; j < numResultsFromR; j++ ) {

                ProbeAnalysisResult probeAnalysisResult = ProbeAnalysisResult.Factory.newInstance();
                probeAnalysisResult.setProbe( cs );
                probeAnalysisResult.setQuantitationType( quantitationType );

                probeAnalysisResult.setScore( fStatistics[k] );
                // probeAnalysisResult.setParameters( parameters );

                if ( j % numResultsFromR == mainEffectAIndex ) {
                    probeAnalysisResult.setPvalue(  Double.isNaN(mainEffectAPvalues[l] ) ? null : mainEffectAPvalues[l] );
                    probeAnalysisResult.setCorrectedPvalue( Double.isNaN( mainEffectAQvalues[l] ) ? null
                            : mainEffectAQvalues[l] );
                    analysisResultsMainEffectA.add( probeAnalysisResult );
                    l++;
                } else if ( j % numResultsFromR == mainEffectBIndex ) {
                    probeAnalysisResult.setPvalue(  Double.isNaN(mainEffectBPvalues[m] ) ? null : mainEffectBPvalues[m] );
                    probeAnalysisResult.setCorrectedPvalue(  Double.isNaN(mainEffectBQvalues[m] ) ? null
                            : mainEffectBQvalues[m] );
                    analysisResultsMainEffectB.add( probeAnalysisResult );
                    m++;
                } else if ( j % numResultsFromR == mainEffectInteractionIndex ) {
                    if ( interactionEffectPvalues != null ) {
                        probeAnalysisResult.setPvalue(  Double.isNaN(interactionEffectPvalues[n] ) ? null
                                : interactionEffectPvalues[n] );
                    }

                    if ( interactionEffectQvalues != null ) {
                        probeAnalysisResult.setCorrectedPvalue( Double.isNaN( interactionEffectQvalues[n] ) ? null
                                : interactionEffectQvalues[n] );
                    }
                    analysisResultsInteractionEffect.add( probeAnalysisResult );
                    n++;
                }

                k++;
            }
        }

        /* main effects */
        Collection<ExperimentalFactor> mainA = new HashSet<ExperimentalFactor>();
        mainA.add( experimentalFactorA );
        ExpressionAnalysisResultSet mainEffectResultSetA = ExpressionAnalysisResultSet.Factory.newInstance(
                expressionAnalysis, analysisResultsMainEffectA, mainA );
        resultSets.add( mainEffectResultSetA );

        Collection<ExperimentalFactor> mainB = new HashSet<ExperimentalFactor>();
        mainB.add( experimentalFactorB );
        ExpressionAnalysisResultSet mainEffectResultSetB = ExpressionAnalysisResultSet.Factory.newInstance(
                expressionAnalysis, analysisResultsMainEffectB, mainB );
        resultSets.add( mainEffectResultSetB );

        /* interaction effect */
        if ( numResultsFromR == maxResults ) {
            Collection<ExperimentalFactor> interAB = new HashSet<ExperimentalFactor>();
            interAB.add( experimentalFactorA );
            interAB.add( experimentalFactorB );
            ExpressionAnalysisResultSet interactionEffectResultSet = ExpressionAnalysisResultSet.Factory.newInstance(
                    expressionAnalysis, analysisResultsInteractionEffect, interAB );
            resultSets.add( interactionEffectResultSet );
        }

        expressionAnalysis.setResultSets( resultSets );

        expressionAnalysis.setName( this.getClass().getSimpleName() );
        if ( ee != null ) {
            boolean interactions = false;
            if ( numResultsFromR == maxResults ) interactions = true;
            expressionAnalysis.setDescription( "Two-way ANOVA for " + experimentalFactorA + " and "
                    + experimentalFactorB + ( interactions ? " with " : " without " ) + "interactions" );
        }

        return expressionAnalysis;
    }

    /*
     * (non-Javadoc)
     * @seeubic.gemma.analysis.diff.AbstractAnalyzer#getExpressionAnalysis(ubic.gemma.model.expression.experiment.
     * ExpressionExperiment)
     */
    @Override
    public DifferentialExpressionAnalysis run( ExpressionExperiment expressionExperiment ) {

        ee = expressionExperiment;

        Collection<ExperimentalFactor> experimentalFactors = expressionExperiment.getExperimentalDesign()
                .getExperimentalFactors();

        return run( expressionExperiment, experimentalFactors );

    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.analysis.expression.diff.AbstractDifferentialExpressionAnalyzer#run(ubic.gemma.model.expression.experiment
     * .ExpressionExperiment, java.util.Collection)
     */
    @Override
    public DifferentialExpressionAnalysis run( ExpressionExperiment expressionExperiment,
            Collection<ExperimentalFactor> experimentalFactors ) {
        if ( experimentalFactors.size() != mainEffectInteractionIndex )
            throw new RuntimeException( "Two way anova supports 2 experimental factors.  Received "
                    + experimentalFactors.size() + "." );

        Iterator<ExperimentalFactor> iter = experimentalFactors.iterator();
        ExperimentalFactor experimentalFactorA = iter.next();
        ExperimentalFactor experimentalFactorB = iter.next();

        return twoWayAnova( expressionExperiment, experimentalFactorA, experimentalFactorB );
    }

    /**
     * To be implemented by the two way anova analyzer.
     * <p>
     * See class level javadoc of two way anova anlayzer for R Call.
     * 
     * @param expressionExperiment
     * @param experimentalFactorA
     * @param experimentalFactorB
     * @return
     */
    protected abstract DifferentialExpressionAnalysis twoWayAnova( ExpressionExperiment expressionExperiment,
            ExperimentalFactor experimentalFactorA, ExperimentalFactor experimentalFactorB );

}
