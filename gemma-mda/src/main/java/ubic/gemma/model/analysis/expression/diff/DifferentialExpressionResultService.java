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

import org.springframework.security.access.annotation.Secured;

import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

/**
 * @author kelsey
 * @version $Id$
 */
public interface DifferentialExpressionResultService {

    /**
     * Given a list of experiments and a threshold value finds all the probes that met the cut off in the given
     * experiments
     * 
     * @param experimentsAnalyzed
     * @param threshold
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_MAP_READ" })
    public Map<BioAssaySet, List<DifferentialExpressionAnalysisResult>> find(
            Collection<BioAssaySet> experimentsAnalyzed, double threshold, Integer limit );

    /**
     * Returns a map of a collection of {@link DifferentialExpressionAnalysisResult}s keyed by
     * {@link ExpressionExperiment}.
     * 
     * @param gene
     * @return Map<ExpressionExperiment, Collection<DifferentialExpressionAnalysisResult>>
     */

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_MAP_READ" })
    public Map<BioAssaySet, List<DifferentialExpressionAnalysisResult>> find( ubic.gemma.model.genome.Gene gene );

    /**
     * Returns a map of a collection of {@link DifferentialExpressionAnalysisResult}s keyed by
     * {@link ExpressionExperiment}.
     * 
     * @param gene
     * @param experimentsAnalyzed
     * @return Map<ExpressionExperiment, Collection<DifferentialExpressionAnalysisResult>>
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_MAP_READ" })
    public Map<BioAssaySet, List<DifferentialExpressionAnalysisResult>> find( ubic.gemma.model.genome.Gene gene,
            Collection<BioAssaySet> experimentsAnalyzed );

    /**
     * Find differential expression for a gene in given data sets, exceeding a given significance level (using the
     * corrected pvalue field)
     * 
     * @param gene
     * @param experimentsAnalyzed
     * @param threshold
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_MAP_READ" })
    public Map<BioAssaySet, List<DifferentialExpressionAnalysisResult>> find( ubic.gemma.model.genome.Gene gene,
            Collection<BioAssaySet> experimentsAnalyzed, double threshold, Integer limit );

    /**
     * Find differential expression for a gene, exceeding a given significance level (using the corrected pvalue field)
     * 
     * @param gene
     * @param threshold
     * @param limit
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_MAP_READ" })
    public Map<BioAssaySet, List<DifferentialExpressionAnalysisResult>> find( ubic.gemma.model.genome.Gene gene,
            double threshold, Integer limit );

    public List<Double> findGeneInResultSet( Gene gene, ExpressionAnalysisResultSet resultSet,
            Collection<Long> arrayDesignIds, Integer limit );

    /**
     * Retrieve differential expression results in bulk. This is an important method for the differential expression
     * interfaces.
     * 
     * @param resultSets
     * @param genes * @return map of resultset IDs to map of gene id to differential expression results.
     */
    public Map<Long, Map<Long, DiffExprGeneSearchResult>> findDifferentialExpressionAnalysisResultIdsInResultSet(
            Map<ExpressionAnalysisResultSet, Collection<Long>> resultSetIdsToArrayDesignsUsed, Collection<Long> geneIds );

    /**
     * Given a list of result sets finds the diff expression results that met the given threshold
     * 
     * @param resultsAnalyzed
     * @param threshold
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_MAP_READ" })
    public Map<ExpressionAnalysisResultSet, List<DifferentialExpressionAnalysisResult>> findInResultSets(
            Collection<ExpressionAnalysisResultSet> resultsAnalyzed, double threshold, Integer limit );

    /**
     * 
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    public Map<DifferentialExpressionAnalysisResult, Collection<ExperimentalFactor>> getExperimentalFactors(
            Collection<DifferentialExpressionAnalysisResult> differentialExpressionAnalysisResults );

    /**
     * 
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExperimentalFactor> getExperimentalFactors(
            DifferentialExpressionAnalysisResult differentialExpressionAnalysisResult );

    public ExpressionAnalysisResultSet loadAnalysisResult( Long analysisResultId );

    public void thaw( Collection<DifferentialExpressionAnalysisResult> results );

    public Collection<DifferentialExpressionAnalysisResult> load( Collection<Long> ids );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public ExpressionAnalysisResultSet thaw( ExpressionAnalysisResultSet resultSet );

    public void thaw( DifferentialExpressionAnalysisResult result );

    /**
     * Does not thaw the collection of probes (just the factor information)
     * 
     * @param resultSet
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public void thawLite( ExpressionAnalysisResultSet resultSet );

    // FIXME not used?
    public Integer countNumberOfDifferentiallyExpressedProbes( long resultSetId, double threshold );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public List<DifferentialExpressionAnalysisResult> findInResultSet( ExpressionAnalysisResultSet ar,
            Double threshold, Integer maxResultsToReturn, Integer minNumberOfResults );

    // this is here for 'cleanup' purposes, usually we would consider these immutable.
    @Secured({ "GROUP_ADMIN" })
    public void update( ExpressionAnalysisResultSet resultSet );

}
