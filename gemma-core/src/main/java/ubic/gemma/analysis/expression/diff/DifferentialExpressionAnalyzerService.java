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

import java.io.IOException;
import java.util.Collection;

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis; 
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;

/**
 * @author Paul
 * @version $Id$
 */
public interface DifferentialExpressionAnalyzerService {

    /**
     * @param expressionExperiment
     * @return
     */
    public abstract Collection<DifferentialExpressionAnalysis> getAnalyses( ExpressionExperiment expressionExperiment );

    /**
     * @param ee
     * @param copyMe
     * @param qvalueThreshold to use for the re-run.
     * @return
     */
    public abstract Collection<DifferentialExpressionAnalysis> redoAnalysis( ExpressionExperiment ee,
            DifferentialExpressionAnalysis copyMe, Double qvalueThreshold );

    /**
     * @param expressionExperiment
     * @param config
     * @return persistent analyses
     */
    public abstract Collection<DifferentialExpressionAnalysis> runDifferentialExpressionAnalyses(
            ExpressionExperiment expressionExperiment, DifferentialExpressionAnalysisConfig config );

    /**
     * @param subset
     * @param config
     * @return
     */
    public abstract DifferentialExpressionAnalysis runDifferentialExpressionAnalysis(
            ExpressionExperimentSubSet subset, DifferentialExpressionAnalysisConfig config );

    /**
     * @param ee
     * @throws IOException
     */
    public abstract void updateScoreDistributionFiles( ExpressionExperiment ee ) throws IOException;

    /**
     * Update the pvalue distributions and the hit count sizes, in cases where these are corrupted etc. One could use
     * redoAnalysis but this should be faster.
     * 
     * @param toUpdate
     */
    public abstract void updateSummaries( DifferentialExpressionAnalysis toUpdate );

    public int deleteAnalyses( ExpressionExperiment expressionExperiment );

    public void deleteAnalysis( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysis existingAnalysis );

    /**
     * This method is made available primarily for tests.
     * 
     * @param expressionExperiment
     * @param analysis
     * @return
     */
    public DifferentialExpressionAnalysis persistAnalysis( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysis analysis );

}