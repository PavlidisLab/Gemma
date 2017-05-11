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

import java.io.IOException;
import java.util.Collection;

/**
 * @author Paul
 */
public interface DifferentialExpressionAnalyzerService {

    /**
     * Delete any differential expression analyses associated with the experiment. Also deletes files associated with
     * the analysis (e.g., results dumps) and associated hitlist sizes and pvalue distributions.
     *
     * @return the number of analyses that were deleted
     */
    int deleteAnalyses( ExpressionExperiment expressionExperiment );

    /**
     * Deletes the given analysis. Also deletes files associated with the analysis. (e.g., results dumps)
     */
    void deleteAnalysis( ExpressionExperiment expressionExperiment, DifferentialExpressionAnalysis existingAnalysis );

    /**
     * Like redo, but we don't save the results, we just add the full set of results to the analysis given. If we want
     * to keep these results, must call update on the old one.
     */
    Collection<ExpressionAnalysisResultSet> extendAnalysis( ExpressionExperiment ee,
            DifferentialExpressionAnalysis toUpdate );

    /**
     * @return all DifferentialExpressionAnalysis entities for the experiment.
     */
    Collection<DifferentialExpressionAnalysis> getAnalyses( ExpressionExperiment expressionExperiment );

    /**
     * @param qvalueThreshold to use for the re-run.
     */
    Collection<DifferentialExpressionAnalysis> redoAnalysis( ExpressionExperiment ee,
            DifferentialExpressionAnalysis copyMe, Double qvalueThreshold );

    /**
     * Redo with the default qvalueThreshold.
     */
    Collection<DifferentialExpressionAnalysis> redoAnalysis( ExpressionExperiment ee,
            DifferentialExpressionAnalysis copyMe );

    /**
     * @return persistent analyses. The qvalue threshold configured for retention will be applied.
     */
    Collection<DifferentialExpressionAnalysis> runDifferentialExpressionAnalyses(
            ExpressionExperiment expressionExperiment, DifferentialExpressionAnalysisConfig config );

    /**
     * @deprecated because we store this in the database, not a file; and it shouldn't be a problem any more
     */
    @Deprecated
    void updateScoreDistributionFiles( ExpressionExperiment ee ) throws IOException;

    /**
     * Update the pvalue distributions and the hit count sizes, in cases where these are corrupted etc. One could use
     * redoAnalysis but this should be faster.
     *
     * @deprecated because we store this in the database and it shouldn't be a problem any more
     */
    @Deprecated
    void updateSummaries( DifferentialExpressionAnalysis toUpdate );

    /**
     * Made public for testing purposes only.
     */
    DifferentialExpressionAnalysis persistAnalysis( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysis analysis, DifferentialExpressionAnalysisConfig config );

}