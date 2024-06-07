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

import org.springframework.context.ApplicationContextAware;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalyzerServiceImpl.AnalysisType;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;

import java.util.Collection;

/**
 * @author paul
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public interface AnalysisSelectionAndExecutionService extends ApplicationContextAware {

    Collection<DifferentialExpressionAnalysis> analyze( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysisConfig config );

    AnalysisType determineAnalysis( BioAssaySet bioAssaySet, DifferentialExpressionAnalysisConfig config );

    /**
     * Determines the analysis to execute based on the experimental factors, factor values, and block design.
     *
     * @param experimentalFactors which factors to use, or null if to use all from the experiment
     * @param subsetFactor        can be null
     * @param includeInteractions if possible
     * @param bioAssaySet         bio assay set
     * @return an appropriate analyzer
     */
    AnalysisType determineAnalysis( BioAssaySet bioAssaySet, Collection<ExperimentalFactor> experimentalFactors,
            ExperimentalFactor subsetFactor, boolean includeInteractions );

    /**
     * @return a new instance of a linear model analyzer.
     */
    DiffExAnalyzer getAnalyzer();

    DifferentialExpressionAnalysis analyze( ExpressionExperimentSubSet subset,
            DifferentialExpressionAnalysisConfig config );

}