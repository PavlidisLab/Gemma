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

import java.util.Collection;
import java.util.Map;

import ubic.gemma.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.HitListSize;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.genome.Gene;

/**
 * @author paul
 * @version $Id$
 */
public interface DiffExAnalyzer {

    /**
     * @param factors
     * @param quantitationType
     * @return
     */
    public abstract ExperimentalFactor determineInterceptFactor( Collection<ExperimentalFactor> factors,
            QuantitationType quantitationType );

    /**
     * @param matrix
     * @param config
     * @param retainScale
     * @return
     */
    public abstract ExpressionDataDoubleMatrix regressionResiduals( ExpressionDataDoubleMatrix matrix,
            DifferentialExpressionAnalysisConfig config, boolean retainScale );

    /**
     * @param expressionExperiment
     * @param config
     * @return
     */
    public abstract Collection<DifferentialExpressionAnalysis> run( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysisConfig config );

    /***
     * Allows entry of modified data matrices into the workflow
     * 
     * @param expressionExperiment
     * @param dmatrix
     * @param config
     * @return
     */
    public abstract Collection<DifferentialExpressionAnalysis> run( ExpressionExperiment expressionExperiment,
            ExpressionDataDoubleMatrix dmatrix, DifferentialExpressionAnalysisConfig config );

    /**
     * Generate HitListSize entities that will be stored to count the number of diff. ex probes at various preset
     * thresholds, to avoid wasting time generating these counts on the fly later. This is done automatically during
     * analysis, so is just here to allow 'backfilling'.
     * 
     * @param results
     * @return
     */
    public Collection<HitListSize> computeHitListSizes( Collection<DifferentialExpressionAnalysisResult> results,
            Map<CompositeSequence, Collection<Gene>> probeToGeneMap );

    /**
     * Utility method
     * 
     * @param resultList
     * @param probeToGeneMap
     * @return
     */
    public int getNumberOfGenesTested( Collection<DifferentialExpressionAnalysisResult> resultList,
            Map<CompositeSequence, Collection<Gene>> probeToGeneMap );

    // this is needed so we can alter this in tests
    abstract void setExpressionDataMatrixService( ExpressionDataMatrixService expressionDataMatrixService );

    /**
     * Note that normally when we run a subset analysis, the subsetting is done internally, so we pass in the expression
     * experiment, not the subset. This method is used for exceptions to that.
     * 
     * @param subset
     * @param config
     * @return
     */
    abstract DifferentialExpressionAnalysis run( ExpressionExperimentSubSet subset,
            DifferentialExpressionAnalysisConfig config );

}