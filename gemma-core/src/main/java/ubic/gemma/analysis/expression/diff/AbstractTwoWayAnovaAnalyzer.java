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
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;

import ubic.basecode.util.r.type.TwoWayAnovaResult;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.analysis.expression.ExpressionAnalysis;
import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.ProbeAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.designElement.CompositeSequence;
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
 * @deprecated this functionality is now folded into the generic linearmodelanalyzer.
 */
@Deprecated
public abstract class AbstractTwoWayAnovaAnalyzer extends AbstractDifferentialExpressionAnalyzer {

    protected final int mainEffectAIndex = 0;
    protected final int mainEffectBIndex = 1;
    protected final int mainEffectInteractionIndex = 2;
    protected final int maxResults = 3;

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.analysis.diff.AbstractAnalyzer#getExpressionAnalysis(ubic.gemma.model.expression.experiment.
     * ExpressionExperiment)
     */
    @Override
    public Collection<DifferentialExpressionAnalysis> run( ExpressionExperiment expressionExperiment ) {

        Collection<ExperimentalFactor> experimentalFactors = expressionExperiment.getExperimentalDesign()
                .getExperimentalFactors();

        return run( expressionExperiment, experimentalFactors );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.AbstractDifferentialExpressionAnalyzer#run(ubic.gemma.model.expression.experiment
     * .ExpressionExperiment, java.util.Collection)
     */
    @Override
    public Collection<DifferentialExpressionAnalysis> run( ExpressionExperiment expressionExperiment,
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
     * Creates and returns an {@link ExpressionAnalysis} and fills in the expression analysis results, writes the
     * distributions.
     * 
     * @param dmatrix
     * @param mainEffectAPvalues
     * @param mainEffectBPvalues
     * @param interactionEffectPvalues - null if no interactions estimated.
     * @param anovaResult
     * @param experimentalFactorA
     * @param experimentalFactorB
     * @param quantitationType
     * @param expressionExperiment
     * @return
     */
    protected DifferentialExpressionAnalysis createExpressionAnalysis( ExpressionDataDoubleMatrix dmatrix,
            Double[] mainEffectAPvalues, Double[] mainEffectBPvalues, Double[] interactionEffectPvalues,
            Map<String, TwoWayAnovaResult> anovaResults, ExperimentalFactor experimentalFactorA,
            ExperimentalFactor experimentalFactorB, QuantitationType quantitationType,
            ExpressionExperiment expressionExperiment ) {

        assert mainEffectAPvalues.length == mainEffectBPvalues.length;

        int pvaluesPerExample = 2;

        if ( interactionEffectPvalues != null ) {
            assert interactionEffectPvalues.length == mainEffectAPvalues.length;
            pvaluesPerExample = 3;
        }

        Collection<ExpressionAnalysisResultSet> resultSets = new HashSet<ExpressionAnalysisResultSet>();

        DifferentialExpressionAnalysis expressionAnalysis = super.initAnalysisEntity( expressionExperiment );

        /* All results for the first main effect */
        List<DifferentialExpressionAnalysisResult> analysisResultsMainEffectA = new ArrayList<DifferentialExpressionAnalysisResult>();

        /* All results for the second main effect */
        List<DifferentialExpressionAnalysisResult> analysisResultsMainEffectB = new ArrayList<DifferentialExpressionAnalysisResult>();

        /* Interaction effect */
        List<DifferentialExpressionAnalysisResult> analysisResultsInteractionEffect = new ArrayList<DifferentialExpressionAnalysisResult>();

        /* q-values */
        double[] mainEffectAQvalues = super.getQValues( mainEffectAPvalues );
        double[] mainEffectBQvalues = super.getQValues( mainEffectBPvalues );
        double[] interactionEffectQvalues = new double[mainEffectAPvalues.length]; // temporary.
        if ( interactionEffectPvalues != null ) {
            interactionEffectQvalues = super.getQValues( interactionEffectPvalues );
            assert interactionEffectQvalues.length == interactionEffectPvalues.length;
        }

        /* ranks */
        double[] ranksA = computeRanks( ArrayUtils.toPrimitive( mainEffectAPvalues ) );
        double[] ranksB = computeRanks( ArrayUtils.toPrimitive( mainEffectBPvalues ) );
        double[] ranksI = new double[mainEffectAPvalues.length]; // temporary.
        if ( interactionEffectPvalues != null ) {
            ranksI = computeRanks( ArrayUtils.toPrimitive( interactionEffectPvalues ) );
        }

        assert ranksA.length == mainEffectAPvalues.length;
        assert ranksB.length == ranksA.length;

        for ( int i = 0; i < dmatrix.rows(); i++ ) {

            CompositeSequence cs = dmatrix.getDesignElementForRow( i );

            // TwoWayAnovaResult twoWayAnovaResult = anovaResults.get( cs.getId() + "" );

            for ( int j = 0; j < pvaluesPerExample; j++ ) {

                ProbeAnalysisResult probeAnalysisResult = ProbeAnalysisResult.Factory.newInstance();
                probeAnalysisResult.setProbe( cs );
                probeAnalysisResult.setQuantitationType( quantitationType );

                // Double fstat = null;
                // switch ( j ) {
                // case 0: {
                // fstat = twoWayAnovaResult.getMainEffectAfVal();
                // break;
                // }
                // case 1: {
                // fstat = twoWayAnovaResult.getMainEffectBfVal();
                // break;
                // }
                // case 2: {
                // fstat = twoWayAnovaResult.getInteractionfVal();
                // break;
                // }
                // }

                if ( j % pvaluesPerExample == mainEffectAIndex ) {
                    probeAnalysisResult.setPvalue( nan2Null( mainEffectAPvalues[i] ) );
                    probeAnalysisResult.setCorrectedPvalue( nan2Null( mainEffectAQvalues[i] ) );
                    probeAnalysisResult.setRank( nan2Null( ranksA[i] ) );
                    analysisResultsMainEffectA.add( probeAnalysisResult );

                } else if ( j % pvaluesPerExample == mainEffectBIndex ) {
                    probeAnalysisResult.setPvalue( nan2Null( mainEffectBPvalues[i] ) );
                    probeAnalysisResult.setCorrectedPvalue( nan2Null( mainEffectBQvalues[i] ) );
                    probeAnalysisResult.setRank( nan2Null( ranksB[i] ) );
                    analysisResultsMainEffectB.add( probeAnalysisResult );

                } else if ( interactionEffectPvalues != null && j % pvaluesPerExample == mainEffectInteractionIndex ) {
                    probeAnalysisResult.setPvalue( nan2Null( interactionEffectPvalues[i] ) );
                    probeAnalysisResult.setRank( nan2Null( ranksI[i] ) );
                    probeAnalysisResult.setCorrectedPvalue( nan2Null( interactionEffectQvalues[i] ) );
                    analysisResultsInteractionEffect.add( probeAnalysisResult );
                }

                // TODO contrasts
            }
        }

        /* main effects */
        Collection<ExperimentalFactor> mainA = new HashSet<ExperimentalFactor>();
        mainA.add( experimentalFactorA );
        ExpressionAnalysisResultSet mainEffectResultSetA = ExpressionAnalysisResultSet.Factory.newInstance( null,
                expressionAnalysis, analysisResultsMainEffectA, null, mainA );
        resultSets.add( mainEffectResultSetA );

        Collection<ExperimentalFactor> mainB = new HashSet<ExperimentalFactor>();
        mainB.add( experimentalFactorB );
        ExpressionAnalysisResultSet mainEffectResultSetB = ExpressionAnalysisResultSet.Factory.newInstance( null,
                expressionAnalysis, analysisResultsMainEffectB, null, mainB );
        resultSets.add( mainEffectResultSetB );

        /* interaction effect */
        if ( interactionEffectPvalues != null ) {
            Collection<ExperimentalFactor> interAB = new HashSet<ExperimentalFactor>();
            interAB.add( experimentalFactorA );
            interAB.add( experimentalFactorB );
            ExpressionAnalysisResultSet interactionEffectResultSet = ExpressionAnalysisResultSet.Factory.newInstance(
                    null, expressionAnalysis, analysisResultsInteractionEffect, null, interAB );
            resultSets.add( interactionEffectResultSet );
        }

        expressionAnalysis.setResultSets( resultSets );

        expressionAnalysis.setName( this.getClass().getSimpleName() );

        expressionAnalysis.setDescription( "Two-way ANOVA for " + experimentalFactorA + " and " + experimentalFactorB
                + ( interactionEffectPvalues != null ? " with " : " without " ) + "interactions" );

        return expressionAnalysis;
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
    protected abstract Collection<DifferentialExpressionAnalysis> twoWayAnova(
            ExpressionExperiment expressionExperiment, ExperimentalFactor experimentalFactorA,
            ExperimentalFactor experimentalFactorB );

}
