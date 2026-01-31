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
import ubic.gemma.model.analysis.expression.diff.*;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.BioAssaySetValueObject;
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
public interface DifferentialExpressionResultService extends BaseReadOnlyService<DifferentialExpressionAnalysisResult> {

    /**
     * @see DifferentialExpressionResultDao#findByExperimentAnalyzed(Collection, boolean, double, int)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_MAP_READ" })
    Map<BioAssaySetValueObject, List<DifferentialExpressionValueObject>> findByExperimentAnalyzed(
            Collection<? extends BioAssaySet> experimentsAnalyzed, boolean includeSubSets, double threshold, int limit );

    /**
     * @see DifferentialExpressionResultDao#findByGene(Gene, boolean, boolean)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_MAP_READ" })
    Map<BioAssaySetValueObject, List<DifferentialExpressionValueObject>> findByGene( Gene gene, boolean useGene2Cs, boolean keepNonSpecificProbes );

    /**
     * @see DifferentialExpressionResultDao#findByGene(Gene, boolean, boolean, double, int)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_MAP_READ" })
    Map<BioAssaySetValueObject, List<DifferentialExpressionValueObject>> findByGene( Gene gene, boolean useGene2Cs, boolean keepNonSpecificProbes, double threshold, int limit );

    /**
     * @see DifferentialExpressionResultDao#findByGeneAndExperimentAnalyzed(Gene, Collection, boolean, Map, Map, Map, double, boolean, boolean, boolean)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT_COLLECTION_READ" })
    List<DifferentialExpressionAnalysisResult> findByGeneAndExperimentAnalyzedIds( Gene gene, boolean useGene2Cs, boolean keepNonSpecific, Collection<Long> experimentAnalyzedIds, boolean includeSubSets, Map<DifferentialExpressionAnalysisResult, Long> sourceExperimentIdMap, Map<DifferentialExpressionAnalysisResult, Long> experimentAnalyzedIdMap, Map<DifferentialExpressionAnalysisResult, Baseline> baselineMap, double threshold, boolean initializeFactorValues );

    /**
     * @see DifferentialExpressionResultDao#findByGeneAndExperimentAnalyzed(Gene, boolean, boolean, Collection, boolean)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_MAP_READ" })
    Map<BioAssaySetValueObject, List<DifferentialExpressionValueObject>> findByGeneAndExperimentAnalyzed( Gene gene,
            boolean useGene2Cs, boolean keepNonSpecificProbes, Collection<? extends BioAssaySet> experimentsAnalyzed, boolean includeSubSets );

    /**
     * @see DifferentialExpressionResultDao#findByGeneAndExperimentAnalyzed(Gene, boolean, boolean, Collection, boolean, double, int)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_MAP_READ" })
    Map<BioAssaySetValueObject, List<DifferentialExpressionValueObject>> findByGeneAndExperimentAnalyzed( Gene gene,
            boolean useGene2Cs, boolean keepNonSpecificProbes, Collection<? extends BioAssaySet> experimentsAnalyzed, boolean includeSubSets, double threshold, int limit );

    /**
     * @see DifferentialExpressionResultDao#findGeneResultsByResultSetIdsAndGeneIds(Collection, Collection)
     */
    Map<Long, Map<Long, DiffExprGeneSearchResult>> findGeneResultsByResultSetIdsAndGeneIds(
            Collection<DiffExResultSetSummaryValueObject> resultSets, Collection<Long> geneIds );

    /**
     * @see DifferentialExpressionResultDao#findByResultSet(ExpressionAnalysisResultSet, double, int, int)
     */
    // results will belong to the result set, so it's faster to check ACL on it than to on each result
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    List<DifferentialExpressionValueObject> findByResultSet( ExpressionAnalysisResultSet ar, double threshold,
            int maxResultsToReturn, int minNumberOfResults );

    /**
     * @see DifferentialExpressionResultDao#findContrastsByAnalysisResultIds(Collection)
     */
    Map<Long, ContrastsValueObject> findContrastsByAnalysisResultIds( Collection<Long> ids );
}
