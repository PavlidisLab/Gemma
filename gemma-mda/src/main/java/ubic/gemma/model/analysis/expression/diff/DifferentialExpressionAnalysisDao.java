/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.model.analysis.expression.diff;

import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.ProbeAnalysisResult;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis
 */
public interface DifferentialExpressionAnalysisDao extends
        ubic.gemma.model.analysis.expression.ExpressionAnalysisDao<DifferentialExpressionAnalysis> {
    /**
     * <p>
     * Find results for a given resultSet.
     * </p>
     */
    public java.util.Collection<DifferentialExpressionAnalysis> find( ubic.gemma.model.genome.Gene gene,
            ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet resultSet, double threshold );

    /**
     * 
     */
    public java.util.Map findByInvestigationIds( java.util.Collection<Long> investigationIds );

    /**
     * @param gene
     * @return
     */
    public java.util.Collection<ExpressionExperiment> findExperimentsWithAnalyses( ubic.gemma.model.genome.Gene gene );

    /**
     * Returns a map of a collection of {@link ProbeAnalysisResult}s keyed by {@link ExpressionExperiment}.
     * 
     * @param gene
     * @param experimentsAnalyzed
     * @return Map<ExpressionExperiment, Collection<ProbeAnalysisResult>>
     */
    public java.util.Map<ubic.gemma.model.expression.experiment.ExpressionExperiment, java.util.Collection<ProbeAnalysisResult>> findResultsForGeneInExperiments(
            ubic.gemma.model.genome.Gene gene,
            java.util.Collection<ubic.gemma.model.expression.experiment.ExpressionExperiment> experimentsAnalyzed );

    /**
     * Find differential expression for a gene in given data sets, exceeding a given significance level (using the
     * corrected pvalue field)
     * 
     * @param gene
     * @param experimentsAnalyzed
     * @param threshold
     * @return
     */
    public java.util.Map<ubic.gemma.model.expression.experiment.ExpressionExperiment, java.util.Collection<ProbeAnalysisResult>> findResultsForGeneInExperimentsMetThreshold(
            ubic.gemma.model.genome.Gene gene,
            java.util.Collection<ubic.gemma.model.expression.experiment.ExpressionExperiment> experimentsAnalyzed,
            double threshold, Integer limit );
    

    /**
     * Given a list of experiments and a threshold value finds all the probes that met the cut off in the given experiments
     * 
     * @param experimentsAnalyzed
     * @param threshold
     * @return
     */
    public java.util.Map<ubic.gemma.model.expression.experiment.ExpressionExperiment, java.util.Collection<ProbeAnalysisResult>> findGenesInExperimentsThatMetThreshold(
            java.util.Collection<ubic.gemma.model.expression.experiment.ExpressionExperiment> experimentsAnalyzed,
            double threshold, Integer limit );

   
    
    /**
     * Given a list of result sets finds the diff expression results that met the given threshold
     * 
     * @param resultsAnalyzed
     * @param threshold
     * @return
     */
    public java.util.Map<ExpressionAnalysisResultSet, java.util.Collection<ProbeAnalysisResult>> findGenesInResultSetsThatMetThreshold(
            java.util.Collection<ExpressionAnalysisResultSet> resultsAnalyzed,
            double threshold, Integer limit );
    
    /**
     * 
     */
    public java.util.Collection getResultSets(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment );

    /**
     * 
     */
    public void thaw( java.util.Collection<DifferentialExpressionAnalysis> expressionAnalyses );

    /**
     * 
     */
    public void thaw(
            ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis differentialExpressionAnalysis );
    
    
    public long countProbesMeetingThreshold(ExpressionAnalysisResultSet ears, double threshold);

}
