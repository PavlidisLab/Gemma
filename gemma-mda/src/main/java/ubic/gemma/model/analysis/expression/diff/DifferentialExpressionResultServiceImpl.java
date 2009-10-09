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
import java.util.Map;

import ubic.gemma.model.analysis.AnalysisResultSet;
import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.ProbeAnalysisResult;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

/**
 * @author keshav
 * @version $Id$
 * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultService
 */
public class DifferentialExpressionResultServiceImpl extends
        ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultServiceBase {

    public void thaw( ProbeAnalysisResult result ) throws Exception {
        this.getDifferentialExpressionResultDao().thaw( result );
    }

    /*
     * (non-Javadoc)
     * @seeubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultServiceBase#
     * handleGetExperimentalFactors(java.util.Collection)
     */
    @Override
    protected Map<ProbeAnalysisResult, Collection<ExperimentalFactor>> handleGetExperimentalFactors(
            Collection<ProbeAnalysisResult> differentialExpressionAnalysisResults ) throws Exception {
        return this.getDifferentialExpressionResultDao().getExperimentalFactors( differentialExpressionAnalysisResults );
    }

    /*
     * (non-Javadoc)
     * @seeubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultServiceBase#
     * handleGetExperimentalFactors
     * (ubic.gemma.model.expression.analysis.expression.diff.DifferentialExpressionAnalysisResult)
     */
    @Override
    protected Collection<ExperimentalFactor> handleGetExperimentalFactors(
            ProbeAnalysisResult differentialExpressionAnalysisResult ) throws Exception {
        return this.getDifferentialExpressionResultDao().getExperimentalFactors( differentialExpressionAnalysisResult );
    }

    @Override
    protected void handleThaw( ExpressionAnalysisResultSet resultSet ) throws Exception {
        this.getExpressionAnalysisResultSetDao().thaw( resultSet );
    }

    public AnalysisResultSet loadAnalysisResult( Long analysisResultId ) {
        return this.getExpressionAnalysisResultSetDao().load( analysisResultId );
    }

    public void thawLite( ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet resultSet ) {
        this.getExpressionAnalysisResultSetDao().thawLite( resultSet );

    }

    /*
     * 
     */
    public java.util.Map<ubic.gemma.model.expression.experiment.ExpressionExperiment, java.util.Collection<ProbeAnalysisResult>> find(
            Gene gene, Collection<ExpressionExperiment> experimentsAnalyzed, double threshold, Integer limit ) {
        return this.getDifferentialExpressionResultDao().find( gene, experimentsAnalyzed, threshold, limit );
    }

    public java.util.Map<ExpressionAnalysisResultSet, java.util.Collection<ProbeAnalysisResult>> findInResultSets(
            java.util.Collection<ExpressionAnalysisResultSet> resultsAnalyzed, double threshold, Integer limit ) {

        return this.getDifferentialExpressionResultDao().findInResultSets( resultsAnalyzed, threshold, limit );
    }

    public java.util.Map<ubic.gemma.model.expression.experiment.ExpressionExperiment, java.util.Collection<ProbeAnalysisResult>> find(
            Collection<ExpressionExperiment> experimentsAnalyzed, double threshold ) {
        return this.getDifferentialExpressionResultDao().find( experimentsAnalyzed, threshold, null );
    }

    public java.util.Map<ubic.gemma.model.expression.experiment.ExpressionExperiment, java.util.Collection<ProbeAnalysisResult>> find(
            Collection<ExpressionExperiment> experimentsAnalyzed, double threshold, Integer limit ) {
        return this.getDifferentialExpressionResultDao().find( experimentsAnalyzed, threshold, limit );

    }

    public java.util.Map<ExpressionAnalysisResultSet, java.util.Collection<ProbeAnalysisResult>> findInResultSets(
            java.util.Collection<ExpressionAnalysisResultSet> resultsAnalyzed, double threshold ) {
        return this.getDifferentialExpressionResultDao().findInResultSets( resultsAnalyzed, threshold, null );

    }

    public Map<ExpressionExperiment, Collection<ProbeAnalysisResult>> find( Gene gene,
            Collection<ExpressionExperiment> experimentsAnalyzed ) {
        return this.getDifferentialExpressionResultDao().find( gene, experimentsAnalyzed );
    }

    public Map<ExpressionExperiment, Collection<ProbeAnalysisResult>> find( Gene gene ) {
        return this.getDifferentialExpressionResultDao().find( gene );
    }

    public Map<ExpressionExperiment, Collection<ProbeAnalysisResult>> find( Gene gene, double threshold, Integer limit ) {
        return this.getDifferentialExpressionResultDao().find( gene, threshold, limit );
    }

}