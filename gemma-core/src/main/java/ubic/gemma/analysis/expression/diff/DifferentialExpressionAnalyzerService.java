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

import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerServiceImpl.AnalysisType;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

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
     * Run the differential expression analysis. First deletes the matching existing differential expression analysis,
     * if any.
     * 
     * @param expressionExperiment
     * @param factors
     * @return persistent analyses
     */
    public abstract Collection<DifferentialExpressionAnalysis> runDifferentialExpressionAnalyses(
            ExpressionExperiment expressionExperiment, Collection<ExperimentalFactor> factors );

    /**
     * Runs the differential expression analysis, then deletes the matching old differential expression analysis (if
     * any).
     * 
     * @param expressionExperiment
     * @param factors
     * @param type
     * @return persistent analyses
     */
    public abstract Collection<DifferentialExpressionAnalysis> runDifferentialExpressionAnalyses(
            ExpressionExperiment expressionExperiment, Collection<ExperimentalFactor> factors, AnalysisType type );

    /**
     * @param expressionExperiment
     * @param config
     * @return persistent analyses
     */
    public abstract Collection<DifferentialExpressionAnalysis> runDifferentialExpressionAnalyses(
            ExpressionExperiment expressionExperiment, DifferentialExpressionAnalysisConfig config );

    /**
     * @param ee
     * @throws IOException
     */
    public abstract void updateScoreDistributionFiles( ExpressionExperiment ee ) throws IOException;

    /**
     * Returns true if any differential expression data exists for the experiment, else false.
     * 
     * @param ee
     * @return
     */
    public abstract boolean wasDifferentialAnalysisRun( ExpressionExperiment ee );

    /**
     * Returns true if differential expression data exists for the experiment with the given factors, else false.
     * 
     * @param ee
     * @param factors
     * @return
     */
    public abstract boolean wasDifferentialAnalysisRun( ExpressionExperiment ee, Collection<ExperimentalFactor> factors );

}