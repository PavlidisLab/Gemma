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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.basecode.math.distribution.Histogram;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.*;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.AbstractService;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author keshav
 * @see DifferentialExpressionResultService
 */
@Service
public class DifferentialExpressionResultServiceImpl extends AbstractService<DifferentialExpressionAnalysisResult>
        implements DifferentialExpressionResultService {

    private final DifferentialExpressionResultDao DERDao;

    @Autowired
    public DifferentialExpressionResultServiceImpl( DifferentialExpressionResultDao DERDao ) {
        super( DERDao );
        this.DERDao = DERDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> find(
            Collection<Long> experimentsAnalyzed, double threshold, int limit ) {
        return this.DERDao.find( experimentsAnalyzed, threshold, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> find( Gene gene ) {
        return this.DERDao.find( gene );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> find( Gene gene,
            Collection<Long> experimentsAnalyzed ) {
        return this.DERDao.find( gene, experimentsAnalyzed );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> find( Gene gene,
            Collection<Long> experimentsAnalyzed, double threshold, int limit ) {
        return this.DERDao.find( gene, experimentsAnalyzed, threshold, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> find( Gene gene,
            double threshold, int limit ) {
        return this.DERDao.find( gene, threshold, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Map<Long, DiffExprGeneSearchResult>> findDiffExAnalysisResultIdsInResultSets(
            Collection<DiffExResultSetSummaryValueObject> resultSets, Collection<Long> geneIds ) {
        return this.DERDao.findDiffExAnalysisResultIdsInResultSets( resultSets, geneIds );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Double> findGeneInResultSet( Gene gene, ExpressionAnalysisResultSet resultSet,
            Collection<Long> arrayDesignIds, int limit ) {
        return this.DERDao.findGeneInResultSets( gene, resultSet, arrayDesignIds, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public List<DifferentialExpressionValueObject> findInResultSet( ExpressionAnalysisResultSet resultSet,
            Double threshold, int maxResultsToReturn, int minNumberOfResults ) {
        return this.DERDao.findInResultSet( resultSet, threshold, maxResultsToReturn, minNumberOfResults );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionAnalysisResultSet, List<DifferentialExpressionAnalysisResult>> findInResultSets(
            Collection<ExpressionAnalysisResultSet> resultsAnalyzed, double threshold, int limit ) {
        return this.DERDao.findInResultSets( resultsAnalyzed, threshold, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public DifferentialExpressionAnalysis getAnalysis( ExpressionAnalysisResultSet rs ) {
        return this.DERDao.getAnalysis( rs );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<DifferentialExpressionAnalysisResult, Collection<ExperimentalFactor>> getExperimentalFactors(
            Collection<DifferentialExpressionAnalysisResult> differentialExpressionAnalysisResults ) {
        return this.DERDao.getExperimentalFactors( differentialExpressionAnalysisResults );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExperimentalFactor> getExperimentalFactors(
            DifferentialExpressionAnalysisResult differentialExpressionAnalysisResult ) {
        return this.DERDao.getExperimentalFactors( differentialExpressionAnalysisResult );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, ContrastsValueObject> loadContrastDetailsForResults( Collection<Long> ids ) {
        return this.DERDao.loadContrastDetailsForResults( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public Histogram loadPvalueDistribution( Long resultSetId ) {
        return this.DERDao.loadPvalueDistribution( resultSetId );
    }

    @Override
    @Transactional(readOnly = true)
    public void thaw( Collection<DifferentialExpressionAnalysisResult> results ) {
        this.DERDao.thaw( results );
    }

    @Override
    @Transactional(readOnly = true)
    public void thaw( DifferentialExpressionAnalysisResult result ) {
        this.DERDao.thaw( result );
    }

}