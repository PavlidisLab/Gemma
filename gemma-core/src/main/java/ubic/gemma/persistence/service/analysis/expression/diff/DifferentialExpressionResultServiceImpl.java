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
package ubic.gemma.persistence.service.analysis.expression.diff;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ubic.basecode.math.distribution.Histogram;
import ubic.gemma.model.analysis.expression.diff.*;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;

/**
 * @author keshav
 * @version $Id$
 * @see DifferentialExpressionResultService
 */
@Service
public class DifferentialExpressionResultServiceImpl extends DifferentialExpressionResultServiceBase {

    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> find(
            Collection<Long> experimentsAnalyzed, double threshold, Integer limit ) {
        return this.getDifferentialExpressionResultDao().find( experimentsAnalyzed, threshold, limit );

    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> find( Gene gene ) {
        return this.getDifferentialExpressionResultDao().find( gene );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> find( Gene gene,
            Collection<Long> experimentsAnalyzed ) {
        return this.getDifferentialExpressionResultDao().find( gene, experimentsAnalyzed );
    }

    /*
     * 
     */
    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> find( Gene gene,
            Collection<Long> experimentsAnalyzed, double threshold, Integer limit ) {
        return this.getDifferentialExpressionResultDao().find( gene, experimentsAnalyzed, threshold, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> find( Gene gene,
            double threshold, Integer limit ) {
        return this.getDifferentialExpressionResultDao().find( gene, threshold, limit );
    }

    /*
     * (non-Javadoc)
     * 
     * @see DifferentialExpressionResultService#
     * findDifferentialExpressionAnalysisResultIdsInResultSet(Collection, Collection, Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Long, Map<Long, DiffExprGeneSearchResult>> findDiffExAnalysisResultIdsInResultSets(
            Collection<DiffExResultSetSummaryValueObject> resultSets, Collection<Long> geneIds ) {
        return this.getDifferentialExpressionResultDao().findDiffExAnalysisResultIdsInResultSets( resultSets, geneIds );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Double> findGeneInResultSet( Gene gene, ExpressionAnalysisResultSet resultSet,
            Collection<Long> arrayDesignIds, Integer limit ) {
        return this.getDifferentialExpressionResultDao().findGeneInResultSets( gene, resultSet, arrayDesignIds, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public List<DifferentialExpressionValueObject> findInResultSet( ExpressionAnalysisResultSet resultSet,
            Double threshold, Integer maxResultsToReturn, Integer minNumberOfResults ) {
        return this.getDifferentialExpressionResultDao().findInResultSet( resultSet, threshold, maxResultsToReturn,
                minNumberOfResults );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionAnalysisResultSet, List<DifferentialExpressionAnalysisResult>> findInResultSets(
            Collection<ExpressionAnalysisResultSet> resultsAnalyzed, double threshold, Integer limit ) {
        return this.getDifferentialExpressionResultDao().findInResultSets( resultsAnalyzed, threshold, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public DifferentialExpressionAnalysis getAnalysis( ExpressionAnalysisResultSet rs ) {
        return this.getDifferentialExpressionResultDao().getAnalysis( rs );
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public Collection<DifferentialExpressionAnalysisResult> load( Collection<Long> ids ) {
        return ( Collection<DifferentialExpressionAnalysisResult> ) this.getDifferentialExpressionResultDao()
                .load( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionAnalysisResultSet loadAnalysisResultSet( Long analysisResultSetId ) {
        return this.getExpressionAnalysisResultSetDao().load( analysisResultSetId );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, ContrastsValueObject> loadContrastDetailsForResults( Collection<Long> ids ) {
        return this.getDifferentialExpressionResultDao().loadContrastDetailsForResults( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public Histogram loadPvalueDistribution( Long resultSetId ) {
        return this.getDifferentialExpressionResultDao().loadPvalueDistribution( resultSetId );
    }

    @Override
    @Transactional(readOnly = true)
    public void thaw( Collection<DifferentialExpressionAnalysisResult> results ) {
        this.getDifferentialExpressionResultDao().thaw( results );
    }

    @Override
    @Transactional(readOnly = true)
    public void thaw( DifferentialExpressionAnalysisResult result ) {
        this.getDifferentialExpressionResultDao().thaw( result );
    }

    @Override
    @Transactional(readOnly = true)
    public void thawLite( ExpressionAnalysisResultSet resultSet ) {
        this.getExpressionAnalysisResultSetDao().thawLite( resultSet );

    }

    @Override
    @Transactional
    public void update( ExpressionAnalysisResultSet resultSet ) {
        this.getExpressionAnalysisResultSetDao().update( resultSet );
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultServiceBase#
     * handleGetExperimentalFactors(Collection)
     */
    @Override
    protected Map<DifferentialExpressionAnalysisResult, Collection<ExperimentalFactor>> handleGetExperimentalFactors(
            Collection<DifferentialExpressionAnalysisResult> differentialExpressionAnalysisResults ) {
        return this.getDifferentialExpressionResultDao()
                .getExperimentalFactors( differentialExpressionAnalysisResults );
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultServiceBase#
     * handleGetExperimentalFactors
     * (ubic.gemma.model.expression.analysis.expression.diff.DifferentialExpressionAnalysisResult)
     */
    @Override
    protected Collection<ExperimentalFactor> handleGetExperimentalFactors(
            DifferentialExpressionAnalysisResult differentialExpressionAnalysisResult ) {
        return this.getDifferentialExpressionResultDao().getExperimentalFactors( differentialExpressionAnalysisResult );
    }

    @Override
    protected ExpressionAnalysisResultSet handleThaw( ExpressionAnalysisResultSet resultSet ) {
        return this.getExpressionAnalysisResultSetDao().thaw( resultSet );
    }

}