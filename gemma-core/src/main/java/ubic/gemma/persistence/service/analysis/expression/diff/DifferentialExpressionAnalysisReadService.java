/*
 * The Gemma project.
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.persistence.service.analysis.expression.diff;

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisValueObject;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject;
import ubic.gemma.model.genome.Gene;

import javax.annotation.CheckReturnValue;
import org.springframework.lang.Nullable;
import java.util.Collection;
import java.util.Map;

/**
 * Read-only retrieval service for {@link DifferentialExpressionAnalysis}.
 * <p>
 * Phase 3 of the {@link DifferentialExpressionAnalysisService} decomposition (strangler
 * fig). This service houses the read-side cluster previously implemented directly on the
 * {@code DifferentialExpressionAnalysisServiceImpl} facade: {@code loadWithExperimentAnalyzed},
 * {@code findByName}, {@code findByFactor}, {@code findExperimentsWithAnalyses},
 * {@code findByExperimentAndAnalysisId}, the {@code thaw} variants, {@code canDelete},
 * {@code findByExperimentIds}, {@code findByExperiment}, {@code findByExperiments},
 * and {@code getExperimentsWithAnalysis}. All methods delegate to the relevant DAOs
 * with no orchestration of write-side collaborators.
 * <p>
 * Callers should generally keep using {@link DifferentialExpressionAnalysisService} as
 * the facade -- the facade delegates to this service. Direct injection is appropriate
 * where a class would otherwise create a Spring construction cycle through the heavier
 * facade.
 * <p>
 * ACL / {@code @Secured} annotations live on {@link DifferentialExpressionAnalysisService}
 * (the caller-facing facade interface); enforcement happens at the facade proxy boundary,
 * so this interface is intentionally unsecured.
 *
 * @see DifferentialExpressionAnalysisService
 */
public interface DifferentialExpressionAnalysisReadService {

    /**
     * Load an analysis by ID and eagerly initialize its {@code experimentAnalyzed}.
     */
    @Nullable
    DifferentialExpressionAnalysis loadWithExperimentAnalyzed( Long id );

    /**
     * @see DifferentialExpressionAnalysisDao#findByName(String)
     */
    Collection<DifferentialExpressionAnalysis> findByName( String name );

    /**
     * @see DifferentialExpressionAnalysisDao#findByFactor(ExperimentalFactor)
     */
    Collection<DifferentialExpressionAnalysis> findByFactor( ExperimentalFactor ef );

    /**
     * @see DifferentialExpressionAnalysisDao#findExperimentsWithAnalyses(Gene)
     */
    Collection<BioAssaySet> findExperimentsWithAnalyses( Gene gene );

    /**
     * @see DifferentialExpressionAnalysisDao#findByExperimentAndAnalysisId(ExpressionExperiment, boolean, Long)
     */
    @Nullable
    DifferentialExpressionAnalysis findByExperimentAndAnalysisId( ExpressionExperiment expressionExperiment, boolean includeSubSets, Long analysisId );

    /**
     * Thaw a collection of {@link DifferentialExpressionAnalysis} for full traversal.
     */
    @CheckReturnValue
    Collection<DifferentialExpressionAnalysis> thaw( Collection<DifferentialExpressionAnalysis> expressionAnalyses );

    /**
     * Thaw a single {@link DifferentialExpressionAnalysis} for full traversal.
     */
    @CheckReturnValue
    DifferentialExpressionAnalysis thaw( DifferentialExpressionAnalysis differentialExpressionAnalysis );

    /**
     * Thaw a single {@link DifferentialExpressionAnalysis} including all its result sets,
     * results and contrasts.
     */
    @CheckReturnValue
    DifferentialExpressionAnalysis thawFully( DifferentialExpressionAnalysis differentialExpressionAnalysis );

    /**
     * @return {@code true} if the analysis can be deleted (no other entity is keeping it
     * around).
     */
    boolean canDelete( DifferentialExpressionAnalysis differentialExpressionAnalysis );

    /**
     * @see DifferentialExpressionAnalysisService#findByExperimentIds(Collection, boolean, boolean)
     */
    Map<ExpressionExperimentDetailsValueObject, Collection<DifferentialExpressionAnalysisValueObject>> findByExperimentIds(
            Collection<Long> experimentIds, boolean includeSubSets, boolean includeAssays );

    /**
     * @see DifferentialExpressionAnalysisDao#findByExperiment(ExpressionExperiment, boolean)
     */
    Collection<DifferentialExpressionAnalysis> findByExperiment( ExpressionExperiment experimentAnalyzed, boolean includeSubSets );

    /**
     * @see DifferentialExpressionAnalysisDao#findByExperiments(Collection, boolean)
     */
    Map<ExpressionExperiment, Collection<DifferentialExpressionAnalysis>> findByExperiments( Collection<ExpressionExperiment> experiments, boolean includeSubSets );

    /**
     * @see DifferentialExpressionAnalysisDao#getExperimentsWithAnalysis(Collection, boolean)
     */
    Collection<Long> getExperimentsWithAnalysis( Collection<Long> experimentIds, boolean includeSubSets );
}
