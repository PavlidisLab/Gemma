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

import org.springframework.context.ApplicationContextAware;

import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerServiceImpl.AnalysisType;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author paul
 * @version $Id$
 */
public interface AnalysisSelectionAndExecutionService extends ApplicationContextAware {

    /**
     * Initiates the differential expression analysis
     * 
     * @param expressionExperiment
     * @return
     */
    public abstract Collection<DifferentialExpressionAnalysis> analyze( ExpressionExperiment expressionExperiment );

    /**
     * @param expressionExperiment
     * @param config
     * @return
     */
    public abstract Collection<DifferentialExpressionAnalysis> analyze( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysisConfig config );

    /**
     * Initiates the differential expression analysis
     * 
     * @param expressionExperiment
     * @return
     */
    public abstract Collection<DifferentialExpressionAnalysis> analyze( ExpressionExperiment expressionExperiment,
            Collection<ExperimentalFactor> factors );

    /**
     * @param expressionExperiment
     * @param factors
     * @param type
     * @return
     */
    public abstract Collection<DifferentialExpressionAnalysis> analyze( ExpressionExperiment expressionExperiment,
            Collection<ExperimentalFactor> factors, AnalysisType type );

    /**
     * @param expressionExperiment
     * @param factors
     * @param type - preselected value rather than inferring it
     * @param subsetFactor - can be null
     * @return
     */
    public abstract AnalysisType determineAnalysis(
            ExpressionExperiment expressionExperiment, Collection<ExperimentalFactor> factors, AnalysisType type,
            ExperimentalFactor subsetFactor );

    /**
     * Determines the analysis to execute based on the experimental factors, factor values, and block design.
     * 
     * @param expressionExperiment
     * @param factors which factors to use, or null if to use all from the experiment
     * @param subsetFactor can ben null
     * @return an appropriate analyzer
     * @throws an exception if the experiment doesn't have a valid experimental design.
     */
    public abstract AnalysisType determineAnalysis(
            ExpressionExperiment expressionExperiment, Collection<ExperimentalFactor> experimentalFactors,
            ExperimentalFactor subsetFactor );

}