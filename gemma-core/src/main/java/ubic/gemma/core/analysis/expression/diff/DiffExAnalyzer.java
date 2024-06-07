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
package ubic.gemma.core.analysis.expression.diff;

import ubic.gemma.core.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.HitListSize;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.genome.Gene;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author paul
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public interface DiffExAnalyzer {

    ExperimentalFactor determineInterceptFactor( Collection<ExperimentalFactor> factors,
            QuantitationType quantitationType );

    /**
     * @param expressionExperiment the experiment
     * @param config               config
     * @return analyses. There will be more than one if a subset factor is defined.
     */
    Collection<DifferentialExpressionAnalysis> run( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysisConfig config );

    /***
     * Allows entry of modified data matrices into the workflow.
     * @param config config
     * @param expressionExperiment the experiment
     * @param dmatrix D matrix
     * @return analyses
     */
    Collection<DifferentialExpressionAnalysis> run( ExpressionExperiment expressionExperiment,
            ExpressionDataDoubleMatrix dmatrix, DifferentialExpressionAnalysisConfig config );

    /**
     * Generate HitListSize entities that will be stored to count the number of diff. ex probes at various preset
     * thresholds, to avoid wasting time generating these counts on the fly later. This is done automatically during
     * analysis, so is just here to allow 'backfilling'.
     *
     * @param probeToGeneMap map
     * @param results        results
     * @return hit list sizes
     */
    Set<HitListSize> computeHitListSizes( Collection<DifferentialExpressionAnalysisResult> results,
            Map<CompositeSequence, Collection<Gene>> probeToGeneMap );

    /**
     * Utility method
     *
     * @param probeToGeneMap map
     * @param resultList     result list
     * @return number of genes tested
     */
    int getNumberOfGenesTested( Collection<DifferentialExpressionAnalysisResult> resultList,
            Map<CompositeSequence, Collection<Gene>> probeToGeneMap );

    /**
     * this is needed so we can alter this in tests
     *
     * @param expressionDataMatrixService EE data matrix service
     */
    void setExpressionDataMatrixService( ExpressionDataMatrixService expressionDataMatrixService );

    /**
     * Note that normally when we run a subset analysis, the subsetting is done internally, so we pass in the expression
     * experiment, not the subset. This method is used for exceptions to that.
     *
     * @param subset subset
     * @param config config
     * @return analysis
     */
    DifferentialExpressionAnalysis run( ExpressionExperimentSubSet subset,
            DifferentialExpressionAnalysisConfig config );

}