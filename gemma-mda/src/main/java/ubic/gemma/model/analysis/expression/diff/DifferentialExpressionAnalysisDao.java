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

import java.util.Collection;

import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis
 */
public interface DifferentialExpressionAnalysisDao extends
        ubic.gemma.model.analysis.expression.ExpressionAnalysisDao<DifferentialExpressionAnalysis> {

    public Integer countDownregulated( ExpressionAnalysisResultSet par, double threshold );

    public Integer countProbesMeetingThreshold( ExpressionAnalysisResultSet ears, double threshold );

    public Integer countUpregulated( ExpressionAnalysisResultSet par, double threshold );

    public java.util.Collection<DifferentialExpressionAnalysis> find( ubic.gemma.model.genome.Gene gene,
            ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet resultSet, double threshold );

    public Collection<DifferentialExpressionAnalysis> findByFactor( ExperimentalFactor ef );

    /**
     * 
     */
    public java.util.Map findByInvestigationIds( java.util.Collection<Long> investigationIds );

    /**
     * @param gene
     * @return
     */
    public java.util.Collection<BioAssaySet> findExperimentsWithAnalyses( ubic.gemma.model.genome.Gene gene );

    /**
     * 
     */
    public java.util.Collection<ExpressionAnalysisResultSet> getResultSets(
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

}
