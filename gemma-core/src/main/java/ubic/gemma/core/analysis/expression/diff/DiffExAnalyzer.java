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

import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;

import java.util.Collection;
import java.util.Map;

/**
 * @author paul
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public interface DiffExAnalyzer {

    /**
     * Analyze a dataset.
     * @param expressionExperiment experiment to analyze
     * @param dmatrix              D matrix
     * @param config               config
     * @return analyses. There will be more than one if a subset factor is defined.
     */
    Collection<DifferentialExpressionAnalysis> run( ExpressionExperiment expressionExperiment,
            ExpressionDataDoubleMatrix dmatrix, DifferentialExpressionAnalysisConfig config );

    /**
     * Analyze a dataset with a pre-existing subset structure.
     * <p>
     * A subset must be defined in the configuration.
     */
    Collection<DifferentialExpressionAnalysis> run( ExpressionExperiment expressionExperiment,
            Map<FactorValue, ExpressionExperimentSubSet> subsets,
            ExpressionDataDoubleMatrix dmatrix,
            DifferentialExpressionAnalysisConfig config );

    /**
     * Analyze a subset.
     * <p>
     * Note that normally when we run a subset analysis, the subsetting is done internally, so we pass in the expression
     * experiment, not the subset. This method is used for exceptions to that.
     *
     * @param subset subset
     * @param config config
     * @return analysis
     */
    DifferentialExpressionAnalysis run( ExpressionExperimentSubSet subset, ExpressionDataDoubleMatrix dmatrix,
            DifferentialExpressionAnalysisConfig config );
}