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
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisValueObject;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.BaseService;
import ubic.gemma.persistence.service.analysis.SingleExperimentAnalysisService;

import javax.annotation.CheckReturnValue;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author kelsey
 */
@SuppressWarnings("unused") // Possible external use
public interface DifferentialExpressionAnalysisService extends BaseService<DifferentialExpressionAnalysis>, SingleExperimentAnalysisService<DifferentialExpressionAnalysis> {

    /**
     * @param par       result set
     * @param threshold for corrected pvalue. Results may not be accurate for 'unreasonable' thresholds.
     * @return number of downregulated elements
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Integer countDownregulated( ExpressionAnalysisResultSet par, double threshold );

    /**
     * @param ears      result sets
     * @param threshold (double) for corrected pvalue. Results may not be accurate for 'unreasonable' thresholds.
     * @return an integer count of all the probes that met the given threshold in the given expressionAnalysisResultSet
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Integer countProbesMeetingThreshold( ExpressionAnalysisResultSet ears, double threshold );

    /**
     * @param par       result set
     * @param threshold for corrected pvalue. Results may not be accurate for 'unreasonable' thresholds.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Integer countUpregulated( ExpressionAnalysisResultSet par, double threshold );

    @Secured({ "GROUP_USER" })
    DifferentialExpressionAnalysis create( DifferentialExpressionAnalysis analysis );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<DifferentialExpressionAnalysis> find( ubic.gemma.model.genome.Gene gene,
            ExpressionAnalysisResultSet resultSet, double threshold );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    Collection<DifferentialExpressionAnalysis> findByFactor( ExperimentalFactor ef );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    Map<Long, Collection<DifferentialExpressionAnalysis>> findByExperimentIds( Collection<Long> investigationIds );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<DifferentialExpressionAnalysis> findByTaxon( Taxon taxon );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<BioAssaySet> findExperimentsWithAnalyses( ubic.gemma.model.genome.Gene gene );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ", "AFTER_ACL_COLLECTION_READ" })
    Collection<DifferentialExpressionAnalysis> getAnalyses( BioAssaySet expressionExperiment );

    /**
     * @param expressionExperiments ees
     * @return quite deeply thawed analyses (not the results themselves, but metadata)
     */
    Map<ExpressionExperiment, Collection<DifferentialExpressionAnalysis>> getAnalyses(
            Collection<? extends BioAssaySet> expressionExperiments );

    @CheckReturnValue
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_COLLECTION_READ" })
    Collection<DifferentialExpressionAnalysis> thaw( Collection<DifferentialExpressionAnalysis> expressionAnalyses );

    @CheckReturnValue
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    DifferentialExpressionAnalysis thaw( DifferentialExpressionAnalysis differentialExpressionAnalysis );

    @CheckReturnValue
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    DifferentialExpressionAnalysis thawFully( DifferentialExpressionAnalysis differentialExpressionAnalysis );

    @Override
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void update( DifferentialExpressionAnalysis o );

    /**
     * @param differentialExpressionAnalysis analysis
     * @return Is the analysis deleteable, or is it tied up with another entity that keeps it from being removed.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    boolean canDelete( DifferentialExpressionAnalysis differentialExpressionAnalysis );

    /**
     * Given a set of ids, find experiments or experimentsubsets that have differential expression analyses. Subsets are
     * handled two ways: if the ID given is of a subset, or if the ID is of an experiment that has subsets. In the
     * latter case, the return value will contain experiments that were not explicitly queried for.
     *
     * @param ids of experiments or experimentsubsets.
     * @return map of bioassayset (valueobjects) to analyses (valueobjects) for each.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_MAP_READ" })
    Map<ExpressionExperimentDetailsValueObject, List<DifferentialExpressionAnalysisValueObject>> getAnalysesByExperiment(
            Collection<Long> ids );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_MAP_READ" })
    Map<ExpressionExperimentDetailsValueObject, List<DifferentialExpressionAnalysisValueObject>> getAnalysesByExperiment(
            Collection<Long> ids, int offset, int limit );

    /**
     * Remove analyses using the given factor.
     * @return the number of analysis removed
     */
    int removeForExperimentalFactor( ExperimentalFactor experimentalFactor );

    int removeForExperimentalFactors( Collection<ExperimentalFactor> experimentalFactors );
}
