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
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import ubic.gemma.model.analysis.AnalysisResultSet;
import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.ProbeAnalysisResult;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

/**
 * @author keshav
 * @version $Id$
 * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultService
 */
@Service
public class DifferentialExpressionResultServiceImpl extends
        ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultServiceBase {

    public java.util.Map<ubic.gemma.model.expression.experiment.BioAssaySet, java.util.List<ProbeAnalysisResult>> find(
            Collection<BioAssaySet> experimentsAnalyzed, double threshold ) {
        return this.getDifferentialExpressionResultDao().find( experimentsAnalyzed, threshold, null );
    }

    public java.util.Map<ubic.gemma.model.expression.experiment.BioAssaySet, java.util.List<ProbeAnalysisResult>> find(
            Collection<BioAssaySet> experimentsAnalyzed, double threshold, Integer limit ) {
        return this.getDifferentialExpressionResultDao().find( experimentsAnalyzed, threshold, limit );

    }

    public Map<BioAssaySet, List<ProbeAnalysisResult>> find( Gene gene ) {
        return this.getDifferentialExpressionResultDao().find( gene );
    }

    public Map<BioAssaySet, List<ProbeAnalysisResult>> find( Gene gene, Collection<BioAssaySet> experimentsAnalyzed ) {
        return this.getDifferentialExpressionResultDao().find( gene, experimentsAnalyzed );
    }

    /*
     * 
     */
    public java.util.Map<ubic.gemma.model.expression.experiment.BioAssaySet, java.util.List<ProbeAnalysisResult>> find(
            Gene gene, Collection<BioAssaySet> experimentsAnalyzed, double threshold, Integer limit ) {
        return this.getDifferentialExpressionResultDao().find( gene, experimentsAnalyzed, threshold, limit );
    }

    public Map<BioAssaySet, List<ProbeAnalysisResult>> find( Gene gene, double threshold, Integer limit ) {
        return this.getDifferentialExpressionResultDao().find( gene, threshold, limit );
    }

    public java.util.Map<ExpressionAnalysisResultSet, List<ProbeAnalysisResult>> findInResultSets(
            java.util.Collection<ExpressionAnalysisResultSet> resultsAnalyzed, double threshold ) {
        return this.getDifferentialExpressionResultDao().findInResultSets( resultsAnalyzed, threshold, null );

    }

    public java.util.Map<ExpressionAnalysisResultSet, List<ProbeAnalysisResult>> findInResultSets(
            java.util.Collection<ExpressionAnalysisResultSet> resultsAnalyzed, double threshold, Integer limit ) {

        return this.getDifferentialExpressionResultDao().findInResultSets( resultsAnalyzed, threshold, limit );
    }

    public ExpressionAnalysisResultSet loadAnalysisResult( Long analysisResultId ) {
        return this.getExpressionAnalysisResultSetDao().load( analysisResultId );
    }

    public void thaw( ProbeAnalysisResult result ) {
        this.getDifferentialExpressionResultDao().thaw( result );
    }

    public void thaw( Collection<ProbeAnalysisResult> results ) {
        this.getDifferentialExpressionResultDao().thaw( results );
    }

    public void thawLite( ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet resultSet ) {
        this.getExpressionAnalysisResultSetDao().thawLite( resultSet );

    }

    /*
     * (non-Javadoc)
     * 
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
     * 
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

}