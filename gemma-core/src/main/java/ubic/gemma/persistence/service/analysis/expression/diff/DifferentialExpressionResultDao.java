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

import ubic.gemma.model.analysis.expression.diff.*;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.BaseDao;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult
 */
public interface DifferentialExpressionResultDao extends BaseDao<DifferentialExpressionAnalysisResult> {

    /**
     * Find differential expression results for a given gene, grouped by experiment.
     *
     * @param useGene2Cs            use the GENE2CS table, which is faster but may be inaccurate
     * @param keepNonSpecificProbes keep non-specific probes
     * @return a map of a collection of {@link DifferentialExpressionAnalysisResult}s keyed by {@link BioAssaySet}.
     */
    Map<BioAssaySet, List<DifferentialExpressionAnalysisResult>> findByGene( Gene gene, boolean useGene2Cs, boolean keepNonSpecificProbes );

    /**
     * Find differential expression for a gene, exceeding a given significance level (using the corrected pvalue field)
     *
     * @param gene                  gene
     * @param useGene2Cs            use the GENE2CS table, which is faster but may be inaccurate
     * @param keepNonSpecificProbes keep non-specific probes (i.e. probes that map to more than one gene)
     * @param threshold             threshold
     * @param limit                 limit
     * @return map to diff exp VOs
     */
    Map<BioAssaySet, List<DifferentialExpressionAnalysisResult>> findByGene( Gene gene, boolean useGene2Cs, boolean keepNonSpecificProbes, double threshold, int limit );

    /**
     * Given a list of experiments and a threshold value finds all the probes that met the cut-off in the given
     * experiments
     *
     * @param experimentsAnalyzed ees
     * @param includeSubSets      if true, include the subsets of the experiments analyzed
     * @param threshold           threshold
     * @param limit               limit
     * @return map to diff ex VOs
     */
    Map<BioAssaySet, List<DifferentialExpressionAnalysisResult>> findByExperimentAnalyzed( Collection<Long> experimentAnalyzedIds, boolean includeSubSets, double threshold, int limit );

    /**
     * Retrieve differential expression results for a given gene across all the given datasets.
     * <p>
     * Results are grouped by result set. If a gene maps to more than one probe, the result with the lowest corrected
     * P-value is selected.
     *
     * @param gene                    a specific gene to retrieve differential expression for
     * @param experimentAnalyzedIds   list of IDs of experiments or experiment subsets to consider
     * @param includeSubsets          include results from experiment subsets
     * @param sourceExperimentIdMap   a mapping of results to source experiment ID
     * @param experimentAnalyzedIdMap a mapping of results to experiment analyzed ID
     * @param baselineMap             a mapping of results to baselines
     * @param threshold               a maximum threshold on the corrected P-value, between 0 and 1 inclusively
     * @param useGene2Cs              use the GENE2CS table, which is faster but may be inaccurate
     * @param keepNonSpecificProbes   whether to keep probes that map to more than one gene
     * @param initializeFactorValues  whether to initialize factor values in contrasts and baselines, note that their
     *                                experimental factors will not be initialized
     * @return differential expression results, grouped by analyzed experiment ID
     */
    List<DifferentialExpressionAnalysisResult> findByGeneAndExperimentAnalyzed(
            Gene gene,
            Collection<Long> experimentAnalyzedIds,
            boolean includeSubsets,
            @Nullable Map<DifferentialExpressionAnalysisResult, Long> sourceExperimentIdMap,
            @Nullable Map<DifferentialExpressionAnalysisResult, Long> experimentAnalyzedIdMap,
            @Nullable Map<DifferentialExpressionAnalysisResult, Baseline> baselineMap,
            double threshold,
            boolean useGene2Cs, boolean keepNonSpecificProbes,
            boolean initializeFactorValues );

    /**
     * Find differential expression results for a given gene and set of experiments, grouped by experiment.
     *
     * @param gene                  gene to retrieve differential expression for
     * @param useGene2Cs            use the GENE2CS table, which is faster but may be inaccurate
     * @param keepNonSpecificProbes keep non-specific probes (i.e. probes that map to more than one gene)
     * @param experimentAnalyzedIds IDs of experiments or experiment subsets to consider
     * @param includeSubSets        include subsets of the analyzed experiments
     * @return a map of a collection of {@link DifferentialExpressionAnalysisResult}s keyed by
     * {@link BioAssaySet}.
     */
    Map<BioAssaySet, List<DifferentialExpressionAnalysisResult>> findByGeneAndExperimentAnalyzed(
            Gene gene, boolean useGene2Cs, boolean keepNonSpecificProbes, Collection<Long> experimentAnalyzedIds, boolean includeSubSets );

    /**
     * Find differential expression for a gene in given data sets, exceeding a given significance level (using the
     * corrected pvalue field)
     *
     * @param gene                  gene
     * @param useGene2Cs            use the GENE2CS table, which is faster but may be inaccurate
     * @param keepNonSpecificProbes keep non-specific probes
     * @param experimentsAnalyzed   restrict results to analysis of these experiments
     * @param includeSubSets        if true, include the subsets of the experiments analyzed
     * @param threshold             threshold
     * @param limit                 limit
     * @return map to diff ex VOs
     */
    Map<BioAssaySet, List<DifferentialExpressionAnalysisResult>> findByGeneAndExperimentAnalyzed(
            Gene gene, boolean useGene2Cs, boolean keepNonSpecificProbes, Collection<Long> experimentsAnalyzed, boolean includeSubSets, double threshold, int limit );

    /**
     * Retrieve differential expression results in bulk. This is an important method for the differential expression
     * interfaces.
     *
     * @param geneIds    gene ids
     * @param resultSets result sets
     * @return map of resultset IDs to map of gene id to differential expression results.
     */
    Map<Long, Map<Long, DiffExprGeneSearchResult>> findDiffExAnalysisResultIdsInResultSets(
            Collection<DiffExResultSetSummaryValueObject> resultSets, Collection<Long> geneIds );

    List<DifferentialExpressionValueObject> findByResultSet( ExpressionAnalysisResultSet resultSet, double threshold,
            int maxResultsToReturn, int minNumberOfResults );

    /**
     * Find contrast results by analysis result IDs.
     *
     * @param ids ids
     * @return map of result to contrasts value object.
     */
    Map<Long, ContrastsValueObject> loadContrastDetailsForResults( Collection<Long> ids );
}
