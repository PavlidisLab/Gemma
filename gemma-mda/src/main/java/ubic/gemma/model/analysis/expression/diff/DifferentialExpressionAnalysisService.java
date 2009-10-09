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
 * 
 */
public interface DifferentialExpressionAnalysisService extends
        ubic.gemma.model.analysis.AnalysisService<DifferentialExpressionAnalysis> {

    /**
     * 
     */
    public ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis create(
            ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis analysis );

    /**
     * 
     */
    public void delete( java.lang.Long idToDelete );

    /**
     * 
     */
    public java.util.Collection<DifferentialExpressionAnalysis> find( ubic.gemma.model.genome.Gene gene,
            ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet resultSet, double threshold );

    /**
     * <p>
     * Given a collection of ids, return a map of id -> differential expression analysis (one per id).
     * </p>
     */
    public java.util.Map<Long, DifferentialExpressionAnalysis> findByInvestigationIds(
            java.util.Collection<Long> investigationIds );

    /**
     * <p>
     * Return a collection of experiments in which the given gene was analyzed.
     * </p>
     */
    public java.util.Collection findExperimentsWithAnalyses( ubic.gemma.model.genome.Gene gene );

    /**
     * 
     */
    public java.util.Collection getResultSets(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment );

    public java.util.Collection<ExpressionAnalysisResultSet> getResultSets( java.util.Collection<Long> resultSetIds );

    /**
     * 
     */
    public void thaw( java.util.Collection expressionAnalyses );

    /**
     * 
     */
    public void thaw(
            ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis differentialExpressionAnalysis );

    /**
     * @param ExpressionAnalysisResultSet
     * @param threshold (double)
     * @return an integer count of all the probes that met the given threshold in the given expressionAnalysisResultSet
     */
    public long countProbesMeetingThreshold( ExpressionAnalysisResultSet ears, double threshold );

}
