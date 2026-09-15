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

import ubic.gemma.model.analysis.expression.diff.Baseline;
import ubic.gemma.model.analysis.expression.diff.ContrastsValueObject;
import ubic.gemma.model.analysis.expression.diff.DiffExResultSetSummaryValueObject;
import ubic.gemma.model.analysis.expression.diff.DiffExprGeneSearchResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionValueObject;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.BioAssaySetValueObject;
import ubic.gemma.model.genome.Gene;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Read-only retrieval service for {@link DifferentialExpressionAnalysisResult}.
 * <p>
 * Phase 3 of the {@link DifferentialExpressionResultService} decomposition (strangler
 * fig). This service houses the row-level result read cluster previously implemented
 * directly on the {@code DifferentialExpressionResultServiceImpl} facade:
 * {@code findByExperimentAnalyzed}, {@code findByGene} (x2), {@code findByGeneAndExperimentAnalyzedIds},
 * {@code findByGeneAndExperimentAnalyzed} (x2), {@code findGeneResultsByResultSetIdsAndGeneIds},
 * {@code findByResultSet}, and {@code findContrastsByAnalysisResultIds}. All methods
 * delegate to {@link DifferentialExpressionResultDao} and orchestrate no write-side
 * collaborators.
 * <p>
 * Distinct from {@link DifferentialExpressionAnalysisReadService}, which is the
 * analysis-level decomp; this service is the row-level result decomp.
 * <p>
 * Callers should generally keep using {@link DifferentialExpressionResultService} as
 * the facade -- the facade delegates to this service. Direct injection is appropriate
 * where a class is logically read-only (REST endpoints, CLIs, browser controllers,
 * intra-core readers).
 * <p>
 * ACL / {@code @Secured} annotations live on {@link DifferentialExpressionResultService}
 * (the caller-facing facade interface); enforcement happens at the facade proxy boundary,
 * so this interface is intentionally unsecured.
 *
 * @see DifferentialExpressionResultService
 */
public interface DifferentialExpressionResultReadService {

    /**
     * @see DifferentialExpressionResultDao#findByExperimentAnalyzed(Collection, boolean, double, int)
     */
    Map<BioAssaySetValueObject, List<DifferentialExpressionValueObject>> findByExperimentAnalyzed(
            Collection<? extends BioAssaySet> experimentsAnalyzed, boolean includeSubSets, double threshold, int limit );

    /**
     * @see DifferentialExpressionResultDao#findByGene(Gene, boolean, boolean)
     */
    Map<BioAssaySetValueObject, List<DifferentialExpressionValueObject>> findByGene( Gene gene, boolean useGene2Cs, boolean keepNonSpecificProbes );

    /**
     * @see DifferentialExpressionResultDao#findByGene(Gene, boolean, boolean, double, int)
     */
    Map<BioAssaySetValueObject, List<DifferentialExpressionValueObject>> findByGene( Gene gene, boolean useGene2Cs, boolean keepNonSpecificProbes, double threshold, int limit );

    /**
     * @see DifferentialExpressionResultDao#findByGeneAndExperimentAnalyzed(Gene, Collection, boolean, Map, Map, Map, double, boolean, boolean, boolean)
     */
    List<DifferentialExpressionAnalysisResult> findByGeneAndExperimentAnalyzedIds( Gene gene, boolean useGene2Cs, boolean keepNonSpecific, Collection<Long> experimentAnalyzedIds, boolean includeSubSets, Map<DifferentialExpressionAnalysisResult, Long> sourceExperimentIdMap, Map<DifferentialExpressionAnalysisResult, Long> experimentAnalyzedIdMap, Map<DifferentialExpressionAnalysisResult, Baseline> baselineMap, double threshold, boolean initializeFactorValues );

    /**
     * @see DifferentialExpressionResultDao#findByGeneAndExperimentAnalyzed(Gene, boolean, boolean, Collection, boolean)
     */
    Map<BioAssaySetValueObject, List<DifferentialExpressionValueObject>> findByGeneAndExperimentAnalyzed( Gene gene,
            boolean useGene2Cs, boolean keepNonSpecificProbes, Collection<? extends BioAssaySet> experimentsAnalyzed, boolean includeSubSets );

    /**
     * @see DifferentialExpressionResultDao#findByGeneAndExperimentAnalyzed(Gene, boolean, boolean, Collection, boolean, double, int)
     */
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
    List<DifferentialExpressionValueObject> findByResultSet( ExpressionAnalysisResultSet ar, double threshold,
            int maxResultsToReturn, int minNumberOfResults );

    /**
     * @see DifferentialExpressionResultDao#findContrastsByAnalysisResultIds(Collection)
     */
    Map<Long, ContrastsValueObject> findContrastsByAnalysisResultIds( Collection<Long> ids );
}
