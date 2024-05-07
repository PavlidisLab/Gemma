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

import org.springframework.security.access.annotation.Secured;
import ubic.basecode.math.distribution.Histogram;
import ubic.gemma.model.analysis.expression.diff.*;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.BaseReadOnlyService;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Main entry point to retrieve differential expression data.
 *
 * @author kelsey
 */
@SuppressWarnings("unused") // Possible external use
public interface DifferentialExpressionResultService extends BaseReadOnlyService<DifferentialExpressionAnalysisResult> {

    /**
     * @see DifferentialExpressionResultDao#findByGeneAndExperimentAnalyzed(Gene, Collection, boolean, Map)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    Map<Long, DifferentialExpressionAnalysisResult> findByGeneAndExperimentAnalyzed( Gene gene, Collection<Long> experimentAnalyzedIds, Map<DifferentialExpressionAnalysisResult, Long> bioAssaySetIdMap );

    /**
     * Given a list of experiments and a threshold value finds all the probes that met the cut off in the given
     * experiments
     *
     * @param experimentsAnalyzed ees
     * @param threshold           threshold
     * @param limit               limit
     * @return map to diff ex VOs
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_MAP_READ" })
    Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> find(
            Collection<Long> experimentsAnalyzed, double threshold, int limit );

    /**
     * Returns a map of a collection of {@link DifferentialExpressionAnalysisResult}s keyed by
     * {@link ExpressionExperiment}.
     *
     * @param gene gene
     * @return map to diff ex VOs
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_MAP_READ" })
    Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> find( Gene gene );

    /**
     * Returns a map of a collection of {@link DifferentialExpressionAnalysisResult}s keyed by
     * {@link ExpressionExperiment}.
     *
     * @param experimentsAnalyzed ees
     * @param gene                gene
     * @return map to diff ex VOs
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_MAP_READ" })
    Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> find( Gene gene,
            Collection<Long> experimentsAnalyzed );

    /**
     * Find differential expression for a gene in given data sets, exceeding a given significance level (using the
     * corrected pvalue field)
     *
     * @param experimentsAnalyzed ees
     * @param threshold           threshold
     * @param limit               limit
     * @param gene                gene
     * @return map to diff ex VOs
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_MAP_READ" })
    Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> find( Gene gene,
            Collection<Long> experimentsAnalyzed, double threshold, int limit );

    /**
     * Find differential expression for a gene, exceeding a given significance level (using the corrected pvalue field)
     *
     * @param gene      gene
     * @param threshold threshold
     * @param limit     limit
     * @return map to diff ex VOs
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_MAP_READ" })
    Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> find( Gene gene, double threshold,
            int limit );

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

    List<DifferentialExpressionValueObject> findInResultSet( ExpressionAnalysisResultSet ar, Double threshold,
            int maxResultsToReturn, int minNumberOfResults );

    /**
     * @param ids ids
     * @return map of result to contrasts value object.
     */
    Map<Long, ContrastsValueObject> loadContrastDetailsForResults( Collection<Long> ids );

    Histogram loadPvalueDistribution( Long analysisResultSetId );
}
