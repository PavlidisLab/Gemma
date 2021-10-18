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
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.*;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.BaseService;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Main entry point to retrieve differential expression data.
 *
 * @author kelsey
 */
@SuppressWarnings("unused") // Possible external use
public interface DifferentialExpressionResultService extends BaseService<DifferentialExpressionAnalysisResult> {

    /**
     * Given a list of experiments and a threshold value finds all the probes that met the cut off in the given
     * experiments
     *
     * @param experimentsAnalyzed ees
     * @param threshold           threshold
     * @param limit               limit
     * @return map to diff ex VOs
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> find(
            Collection<Long> experimentsAnalyzed, double threshold, Integer limit );

    /**
     * Returns a map of a collection of {@link DifferentialExpressionAnalysisResult}s keyed by
     * {@link ExpressionExperiment}.
     *
     * @param gene gene
     * @return map to diff ex VOs
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> find( Gene gene );

    /**
     * Returns a map of a collection of {@link DifferentialExpressionAnalysisResult}s keyed by
     * {@link ExpressionExperiment}.
     *
     * @param experimentsAnalyzed ees
     * @param gene                gene
     * @return map to diff ex VOs
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
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
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> find( Gene gene,
            Collection<Long> experimentsAnalyzed, double threshold, Integer limit );

    /**
     * Find differential expression for a gene, exceeding a given significance level (using the corrected pvalue field)
     *
     * @param gene      gene
     * @param threshold threshold
     * @param limit     limit
     * @return map to diff ex VOs
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> find( Gene gene, double threshold,
            Integer limit );

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

    List<Double> findGeneInResultSet( Gene gene, ExpressionAnalysisResultSet resultSet, Collection<Long> arrayDesignIds,
            Integer limit );

    List<DifferentialExpressionValueObject> findInResultSet( ExpressionAnalysisResultSet ar, Double threshold,
            Integer maxResultsToReturn, Integer minNumberOfResults );

    /**
     * Given a list of result sets finds the diff expression results that met the given threshold
     *
     * @param threshold threshold
     * @param limit     limit
     * @return map to diff ex VOs
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_MAP_READ" })
    Map<ExpressionAnalysisResultSet, List<DifferentialExpressionAnalysisResult>> findInResultSets(
            Collection<ExpressionAnalysisResultSet> resultsAnalyzed, double threshold, Integer limit );

    /**
     * Fetch the analysis associated with a result set.
     *
     * @param rs result set
     * @return diff ex.
     */
    DifferentialExpressionAnalysis getAnalysis( ExpressionAnalysisResultSet rs );

    Map<DifferentialExpressionAnalysisResult, Collection<ExperimentalFactor>> getExperimentalFactors(
            Collection<DifferentialExpressionAnalysisResult> differentialExpressionAnalysisResults );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExperimentalFactor> getExperimentalFactors(
            DifferentialExpressionAnalysisResult differentialExpressionAnalysisResult );

    ExpressionAnalysisResultSet loadAnalysisResultSet( Long analysisResultSetId );

    /**
     * @param ids ids
     * @return map of result to contrasts value object.
     */
    Map<Long, ContrastsValueObject> loadContrastDetailsForResults( Collection<Long> ids );

    Histogram loadPvalueDistribution( Long analysisResultSetId );

    void thaw( Collection<DifferentialExpressionAnalysisResult> results );

    void thaw( DifferentialExpressionAnalysisResult result );

    void thaw( ExpressionAnalysisResultSet resultSet );

    @Secured({ "GROUP_ADMIN" })
    void update( ExpressionAnalysisResultSet resultSet );

}
