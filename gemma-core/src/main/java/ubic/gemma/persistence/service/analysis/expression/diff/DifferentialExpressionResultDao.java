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

import ubic.basecode.math.distribution.Histogram;
import ubic.gemma.model.analysis.expression.diff.*;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.BaseDao;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult
 */
public interface DifferentialExpressionResultDao extends BaseDao<DifferentialExpressionAnalysisResult> {

    Integer countNumberOfDifferentiallyExpressedProbes( long resultSetId, double threshold );

    /**
     * Find differential expression for a gene in given data sets, exceeding a given significance level (using the
     * corrected pvalue field)
     */
    Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> find( Gene gene,
            Collection<Long> experimentsAnalyzed, double threshold, Integer limit );

    /**
     * Given a list of experiments and a threshold value finds all the probes that met the cut off in the given
     * experiments
     */
    Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> find(
            Collection<Long> experimentsAnalyzed, double threshold, Integer limit );

    /**
     * Returns a map of a collection of {@link DifferentialExpressionAnalysisResult}s keyed by {@link BioAssaySet}.
     */
    Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> find( Gene gene );

    /**
     * Returns a map of a collection of {@link DifferentialExpressionAnalysisResult}s keyed by {@link BioAssaySet}.
     */
    Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> find( Gene gene,
            Collection<Long> experimentsAnalyzed );

    /**
     * @return map of resultSetId to map of gene to DiffExprGeneSearchResult
     */
    Map<Long, Map<Long, DiffExprGeneSearchResult>> findDiffExAnalysisResultIdsInResultSets(
            Collection<DiffExResultSetSummaryValueObject> resultSets, Collection<Long> geneIds );

    List<Double> findGeneInResultSets( Gene gene, ExpressionAnalysisResultSet resultSet,
            Collection<Long> arrayDesignIds, Integer limit );

    List<DifferentialExpressionValueObject> findInResultSet( ExpressionAnalysisResultSet resultSet, Double threshold,
            Integer maxResultsToReturn, Integer minNumberOfResults );

    /**
     * Given a list of result sets finds the diff expression results that met the given threshold
     */
    Map<ExpressionAnalysisResultSet, List<DifferentialExpressionAnalysisResult>> findInResultSets(
            Collection<ExpressionAnalysisResultSet> resultsAnalyzed, double threshold, Integer limit );

    DifferentialExpressionAnalysis getAnalysis( ExpressionAnalysisResultSet rs );

    Collection<ExperimentalFactor> getExperimentalFactors(
            DifferentialExpressionAnalysisResult differentialExpressionAnalysisResult );

    Map<DifferentialExpressionAnalysisResult, Collection<ExperimentalFactor>> getExperimentalFactors(
            Collection<DifferentialExpressionAnalysisResult> differentialExpressionAnalysisResults );

    Map<Long, ContrastsValueObject> loadContrastDetailsForResults( Collection<Long> ids );

    void thaw( Collection<DifferentialExpressionAnalysisResult> results );

    void thaw( final DifferentialExpressionAnalysisResult result );

    /**
     * Find differential expression for a gene, exceeding a given significance level (using the corrected pvalue field)
     */
    Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> find( Gene gene, double threshold,
            Integer limit );

    Histogram loadPvalueDistribution( Long resultSetId );

}
