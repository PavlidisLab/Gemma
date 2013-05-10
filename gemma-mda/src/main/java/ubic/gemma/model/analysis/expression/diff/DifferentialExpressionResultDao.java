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

import org.springframework.stereotype.Repository;

import ubic.basecode.math.distribution.Histogram;
import ubic.gemma.analysis.expression.diff.ContrastsValueObject;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.BaseDao;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult
 */
@Repository
public interface DifferentialExpressionResultDao extends BaseDao<DifferentialExpressionAnalysisResult> {

    public Integer countNumberOfDifferentiallyExpressedProbes( long resultSetId, double threshold );

    /**
     * Find differential expression for a gene in given data sets, exceeding a given significance level (using the
     * corrected pvalue field)
     * 
     * @param gene
     * @param experimentsAnalyzed
     * @param threshold
     * @return
     */
    public java.util.Map<BioAssaySet, java.util.List<DifferentialExpressionAnalysisResult>> find( Gene gene,
            Collection<BioAssaySet> experimentsAnalyzed, double threshold, Integer limit );

    /**
     * Given a list of experiments and a threshold value finds all the probes that met the cut off in the given
     * experiments
     * 
     * @param experimentsAnalyzed
     * @param threshold
     * @return
     */
    public java.util.Map<BioAssaySet, java.util.List<DifferentialExpressionAnalysisResult>> find(
            java.util.Collection<BioAssaySet> experimentsAnalyzed, double threshold, Integer limit );

    /**
     * Returns a map of a collection of {@link DifferentialExpressionAnalysisResult}s keyed by {@link BioAssaySet}.
     * 
     * @param gene
     * @return Map<ExpressionExperiment, Collection<DifferentialExpressionAnalysisResult>>
     */
    public java.util.Map<BioAssaySet, java.util.List<DifferentialExpressionAnalysisResult>> find(
            ubic.gemma.model.genome.Gene gene );

    /**
     * Returns a map of a collection of {@link DifferentialExpressionAnalysisResult}s keyed by {@link BioAssaySet}.
     * 
     * @param gene
     * @param experimentsAnalyzed
     * @return Map<ExpressionExperiment, Collection<DifferentialExpressionAnalysisResult>>
     */
    public java.util.Map<BioAssaySet, java.util.List<DifferentialExpressionAnalysisResult>> find(
            ubic.gemma.model.genome.Gene gene, java.util.Collection<BioAssaySet> experimentsAnalyzed );

    /**
     * @param resultSets2arrayDesigns
     * @param geneIds
     * @return map of resultsetId to map of gene to DiffExprGeneSearchResult
     */
    public Map<Long, Map<Long, DiffExprGeneSearchResult>> findDifferentialExpressionAnalysisResultIdsInResultSet(
            Map<ExpressionAnalysisResultSet, Collection<Long>> resultSetIdsToArrayDesignsUsed, Collection<Long> geneIds );

    public List<Double> findGeneInResultSets( Gene gene, ExpressionAnalysisResultSet resultSet,
            Collection<Long> arrayDesignIds, Integer limit );

    public List<DifferentialExpressionAnalysisResult> findInResultSet( ExpressionAnalysisResultSet resultSet,
            Double threshold, Integer maxResultsToReturn, Integer minNumberOfResults );

    /**
     * Given a list of result sets finds the diff expression results that met the given threshold
     * 
     * @param resultsAnalyzed
     * @param threshold
     * @return
     */
    public Map<ExpressionAnalysisResultSet, List<DifferentialExpressionAnalysisResult>> findInResultSets(
            java.util.Collection<ExpressionAnalysisResultSet> resultsAnalyzed, double threshold, Integer limit );

    /**
     * @param rs
     * @return
     */
    public DifferentialExpressionAnalysis getAnalysis( ExpressionAnalysisResultSet rs );

    /**
     * 
     */
    public Collection<ExperimentalFactor> getExperimentalFactors(
            DifferentialExpressionAnalysisResult differentialExpressionAnalysisResult );

    /**
     * 
     */
    public Map<DifferentialExpressionAnalysisResult, Collection<ExperimentalFactor>> getExperimentalFactors(
            Collection<DifferentialExpressionAnalysisResult> differentialExpressionAnalysisResults );

    /**
     * @param ids
     * @return
     */
    public Map<Long, ContrastsValueObject> loadContrastDetailsForResults( Collection<Long> ids );

    /**
     * @param results
     */
    public void thaw( Collection<DifferentialExpressionAnalysisResult> results );

    /**
     * @param result
     */
    public void thaw( final DifferentialExpressionAnalysisResult result );

    /**
     * Find differential expression for a gene, exceeding a given significance level (using the corrected pvalue field)
     * 
     * @param gene
     * @param threshold
     * @param limit
     * @return
     */
    java.util.Map<ubic.gemma.model.expression.experiment.BioAssaySet, java.util.List<DifferentialExpressionAnalysisResult>> find(
            ubic.gemma.model.genome.Gene gene, double threshold, Integer limit );

    /**
     * @param resultSetId
     * @return
     */
    public Histogram loadPvalueDistribution( Long resultSetId );

}
