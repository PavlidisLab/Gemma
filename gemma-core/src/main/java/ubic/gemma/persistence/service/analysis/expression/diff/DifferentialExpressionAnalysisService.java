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
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.analysis.AnalysisService;
import ubic.gemma.persistence.service.common.auditAndSecurity.SecurableBaseService;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

/**
 * Service interface for {@link DifferentialExpressionAnalysis}.
 * <p>
 * The methods here have two flavours: by experiments, which is always an {@link ExpressionExperiment} and by experiment
 * analyzed which is either a {@link ExpressionExperiment} or {@link ExpressionExperimentSubSet}.
 *
 * @author kelsey
 * @see DifferentialExpressionAnalysisDao
 */
public interface DifferentialExpressionAnalysisService extends AnalysisService<DifferentialExpressionAnalysis>, SecurableBaseService<DifferentialExpressionAnalysis> {

    @Nullable
    DifferentialExpressionAnalysis loadWithExperimentAnalyzed( Long id );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<DifferentialExpressionAnalysis> findByName( String name );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    Collection<DifferentialExpressionAnalysis> findByFactor( ExperimentalFactor ef );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<BioAssaySet> findExperimentsWithAnalyses( Gene gene );

    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ", "AFTER_ACL_READ" })
    DifferentialExpressionAnalysis findByExperimentAndAnalysisId( ExpressionExperiment expressionExperiment, boolean includeSubSets, Long analysisId );

    @CheckReturnValue
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_COLLECTION_READ" })
    Collection<DifferentialExpressionAnalysis> thaw( Collection<DifferentialExpressionAnalysis> expressionAnalyses );

    @CheckReturnValue
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    DifferentialExpressionAnalysis thaw( DifferentialExpressionAnalysis differentialExpressionAnalysis );

    @CheckReturnValue
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    DifferentialExpressionAnalysis thawFully( DifferentialExpressionAnalysis differentialExpressionAnalysis );

    /**
     * @param differentialExpressionAnalysis analysis
     * @return Is the analysis deleteable, or is it tied up with another entity that keeps it from being removed.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    boolean canDelete( DifferentialExpressionAnalysis differentialExpressionAnalysis );

    /**
     * Given a set of ids, find experiments or subsets that have differential expression analyses.
     * <p>
     * Subsets are handled two ways: if the ID given is of a subset, or if the ID is of an experiment that has subsets.
     * In the latter case, the return value will contain experiments that were not explicitly queried for.
     *
     * @param experimentIds  of {@link ubic.gemma.model.expression.experiment.ExpressionExperiment}
     * @param includeSubSets include analysis of subsets of the requested experiments
     * @param includeAssays  include assay information in the value objects
     * @return map of detailed EE VOs to analyses VOs for each, subset analyses are mapped to their source experiment
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_MAP_READ" })
    Map<ExpressionExperimentDetailsValueObject, Collection<DifferentialExpressionAnalysisValueObject>> findByExperimentIds(
            Collection<Long> experimentIds, boolean includeSubSets, boolean includeAssays );

    /**
     * Remove analyses using the given factor.
     *
     * @return the number of analysis removed
     */
    int removeForExperimentalFactor( ExperimentalFactor experimentalFactor );

    int removeForExperimentalFactors( Collection<ExperimentalFactor> experimentalFactors );

    /**
     * @param experimentAnalyzed experiment analyzed
     * @param includeSubSets     include analyses for its {@link ExpressionExperimentSubSet}
     * @return find all the analyses that involved the given investigation
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<DifferentialExpressionAnalysis> findByExperiment( ExpressionExperiment experimentAnalyzed, boolean includeSubSets );

    /**
     * @param experiments    investigations
     * @param includeSubSets include analyses for their {@link ExpressionExperimentSubSet}
     * @return analyses grouped by experiment, subset analyses are mapped to their source experiment
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_COLLECTION_READ", "AFTER_ACL_MAP_READ" })
    Map<ExpressionExperiment, Collection<DifferentialExpressionAnalysis>> findByExperiments( Collection<ExpressionExperiment> experiments, boolean includeSubSets );

    /**
     * Obtain IDs of experiments that have analyses.
     * <p>
     * Not secured: for internal use only
     *
     * @param experimentIds  starting list of {@link ExpressionExperiment} IDs
     * @param includeSubSets include IDs of their subsets that have analyses
     * @return the ones which have an analysis
     */
    Collection<Long> getExperimentsWithAnalysis( Collection<Long> experimentIds, boolean includeSubSets );

    /**
     * Removes all analyses for the given experiment
     *
     * @param ee             the expriment to remove all analyses for
     * @param includeSubSets also delete analyses of its {@link ExpressionExperimentSubSet}.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void removeForExperiment( ExpressionExperiment ee, boolean includeSubSets );

    /**
     * Remove all analysis for the given experiment or subset.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void removeForExperimentAnalyzed( BioAssaySet experimentAnalyzed );
}
