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

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Collection;

/**
 * @author Paul
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public interface DifferentialExpressionAnalyzerService {

    /**
     * Delete any differential expression analyses associated with the experiment. Also deletes files associated with
     * the analysis (e.g., results dumps) and associated hitlist sizes and pvalue distributions.
     *
     * @param expressionExperiment the experiment
     * @return the number of analyses that were deleted
     */
    int deleteAnalyses( ExpressionExperiment expressionExperiment );

    /**
     * Delete the specified differential expression analyses associated with the experiment.
     */
    int deleteAnalyses( ExpressionExperiment ee, Collection<DifferentialExpressionAnalysis> analysesToDelete );

    /**
     * Deletes the given analysis. Also deletes files associated with the analysis. (e.g., results dumps)
     *
     * @param expressionExperiment the experiment
     * @param existingAnalysis     analysis
     */
    void deleteAnalysis( ExpressionExperiment expressionExperiment, DifferentialExpressionAnalysis existingAnalysis );

    /**
     * Like redo, but we don't save the results, we just add the full set of results to the analysis given. If we want
     * to keep these results, must call update on the old one.
     *
     * @param ee       the experiment
     * @param toUpdate analysis
     * @return collection of results
     */
    Collection<ExpressionAnalysisResultSet> extendAnalysis( ExpressionExperiment ee,
            DifferentialExpressionAnalysis toUpdate, DifferentialExpressionAnalysisConfig config );

    /**
     * @param expressionExperiment the experiment
     * @return all DifferentialExpressionAnalysis entities for the experiment.
     */
    Collection<DifferentialExpressionAnalysis> getAnalyses( ExpressionExperiment expressionExperiment );

    /**
     * Redo an analysis.
     * @see #redoAnalysis(ExpressionExperiment, DifferentialExpressionAnalysis, DifferentialExpressionAnalysisConfig)
     */
    Collection<DifferentialExpressionAnalysis> redoAnalysis( ExpressionExperiment ee, DifferentialExpressionAnalysis dea );

    /**
     * Redo an analysis.
     *
     * @param ee     the experiment
     * @param dea    analysis to base new one on
     * @param config configuration for the analysis, factors and interactions will be ignored, but all other settings
     *               apply as usual
     * @return DEAs
     */
    Collection<DifferentialExpressionAnalysis> redoAnalysis( ExpressionExperiment ee,
            DifferentialExpressionAnalysis dea, DifferentialExpressionAnalysisConfig config );

    /**
     * Redo multiple analyses.
     *
     * @param ignoreFailingAnalyses if true, analyses that fail will not be reported as errors, but will be skipped.
     *                              Note that if all analyses fail, a {@link AllAnalysesFailedException} will be raised.
     * @see #redoAnalysis(ExpressionExperiment, DifferentialExpressionAnalysis, DifferentialExpressionAnalysisConfig)
     */
    Collection<DifferentialExpressionAnalysis> redoAnalyses( ExpressionExperiment ee, Collection<DifferentialExpressionAnalysis> deas, DifferentialExpressionAnalysisConfig config, boolean ignoreFailingAnalyses );

    /**
     * @param expressionExperiment the experiment
     * @param config               config
     * @return persistent analyses.
     */
    Collection<DifferentialExpressionAnalysis> runDifferentialExpressionAnalyses(
            ExpressionExperiment expressionExperiment, DifferentialExpressionAnalysisConfig config );

    /**
     * Made public for testing purposes only.
     *
     * @param expressionExperiment the experiment
     * @param config               config
     * @param analysis             analysis
     * @return persistent analysis
     */
    DifferentialExpressionAnalysis persistAnalysis( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysis analysis, DifferentialExpressionAnalysisConfig config );
}