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

import ubic.gemma.core.util.math.distribution.Histogram;
import ubic.gemma.model.analysis.expression.diff.*;
import ubic.gemma.model.annotations.MayBeUninitialized;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.FilteringVoEnabledDao;
import ubic.gemma.persistence.service.analysis.AnalysisResultSetDao;
import ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import org.springframework.lang.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @see ExpressionAnalysisResultSet
 */
public interface ExpressionAnalysisResultSetDao extends AnalysisResultSetDao<DifferentialExpressionAnalysisResult, ExpressionAnalysisResultSet>, FilteringVoEnabledDao<ExpressionAnalysisResultSet, DifferentialExpressionAnalysisResultSetValueObject> {

    /**
     * Load an analysis result set with its all of its associated results.
     *
     * @param id the ID of the analysis result set
     * @return the analysis result set with its associated results, or null if not found
     */
    @Nullable
    ExpressionAnalysisResultSet loadWithResultsAndContrasts( Long id );

    /**
     * Load a result set with its analysis and the analysis' experimentAnalyzed fully initialized,
     * in a single round-trip. Avoids the sequential lazy-init chain that thaw() does — the dominant
     * cost on the /datasets/{id}/expressions/differential endpoint over a high-latency DB link.
     */
    @Nullable
    ExpressionAnalysisResultSet loadWithAnalysisAndExperimentAnalyzed( Long id );

    /**
     * Load a slice of an analysis result set.
     * <p>
     * Results are sorted by ascending correct P-value.
     *
     * @param offset    an offset of results to load
     * @param limit     a limit of results to load, or -1 to load all results starting at offset
     * @see #loadWithResultsAndContrasts(Long)
     */
    @Nullable
    ExpressionAnalysisResultSet loadWithResultsAndContrasts( Long id, int offset, int limit );

    /**
     * Load a slice of an analysis result set with a corrected P-value threshold.
     * <p>
     * <b>Important note:</b> when using a threshold, results with null P-values will not be included, thus setting the
     * threshold to {@code 1.0} <b>is not equivalent</b> to {@link #loadWithResultsAndContrasts(Long, int, int)}.
     * @param threshold corrected P-value maximum threshold (inclusive)
     */
    @Nullable
    ExpressionAnalysisResultSet loadWithResultsAndContrasts( Long id, double threshold, int offset, int limit );

    boolean canDelete( DifferentialExpressionAnalysis differentialExpressionAnalysis );

    /**
     * Load an analysis result set with its all of its associated results.
     *
     * @param includeFactorValuesInContrasts include complete FV in the contrasts, only IDs are displayed if false
     * @param queryGenesByResult             query genes by results instead of result set, this is considerably faster if the
     *                                       results are sliced (i.e. from {@link #loadWithResultsAndContrasts(Long, int, int)})
     * @param includeTaxonInGenes            include complete taxon in the contrasts, only the ID is displayed if false
     * @see #loadValueObject(Identifiable)
     * @see #loadResultToGenesMap(ExpressionAnalysisResultSet, boolean)
     */
    DifferentialExpressionAnalysisResultSetValueObject loadValueObjectWithResults( ExpressionAnalysisResultSet resultSet, boolean includeFactorValuesInContrasts, boolean queryGenesByResult, boolean includeTaxonInGenes );

    /**
     * Load a {@link DifferentialExpressionAnalysisResult} to {@link Gene} multi-map.
     * <p>
     * This is much faster than navigating through the probe's alignments, transcripts and then genes as it uses the
     * internal GENE2CS table described in {@link TableMaintenanceUtil#updateGene2CsEntries()}.
     * <p>
     * Note: Not all probes have associated genes, so you should use {@link Map#getOrDefault(Object, Object)} with an
     * empty collection to handle this case.
     * @param queryByResult query by results instead of result set, this is considerably faster if the results are
     *                      sliced (i.e. from {@link #loadWithResultsAndContrasts(Long, int, int)})
     */
    Map<Long, Set<Gene>> loadResultToGenesMap( ExpressionAnalysisResultSet resultSet, boolean queryByResult );

    /**
     * Retrieve result sets associated to a set of {@link BioAssaySet} and external database entries.
     *
     * @param bioAssaySets    related {@link BioAssaySet}, or any if null
     * @param databaseEntries related external identifier associated to the {@link BioAssaySet}, or any if null
     * @param filters         filters for restricting results
     * @param limit           maximum number of results to return
     * @param sort            field and direction by which the collection is ordered
     */
    Slice<DifferentialExpressionAnalysisResultSetValueObject> findByBioAssaySetInAndDatabaseEntryInLimit( @Nullable Collection<BioAssaySet> bioAssaySets, @Nullable Collection<DatabaseEntry> databaseEntries, @Nullable Filters filters, int offset, int limit, @Nullable Sort sort );

    /**
     * Cursor-paged counterpart to {@link #findByBioAssaySetInAndDatabaseEntryInLimit}: keyset
     * pagination over the result sets associated to a set of {@link BioAssaySet} and external
     * {@link DatabaseEntry database entries}, always sorted by ascending {@code id}.
     * <p>
     * Single-component {@code id}-asc sort only — see
     * {@code CURSOR_PAGINATION_STEP1_PLAN.md} step 1i (compound sorts deferred pending the
     * indexed-column audit, recce sec. 3.4). When {@code cursor} is non-null the cursor's
     * {@code sortSpec} must equal {@code "+id"} and its key tuple must be a single numeric id.
     *
     * @param bioAssaySets    related {@link BioAssaySet}, or any if null
     * @param databaseEntries related external identifier associated to the {@link BioAssaySet}, or any if null
     * @param filters         filters for restricting results
     * @param cursor          decoded cursor (forward or backward) for keyset pagination, or {@code null} for the first page
     * @param limit           maximum number of results to return
     */
    CursorPage<DifferentialExpressionAnalysisResultSetValueObject> findByBioAssaySetInAndDatabaseEntryInByCursor( @Nullable Collection<BioAssaySet> bioAssaySets, @Nullable Collection<DatabaseEntry> databaseEntries, @Nullable Filters filters, @Nullable Cursor cursor, int limit );

    /**
     * Initialize the analysis and subset factor vale.
     */
    void thaw( ExpressionAnalysisResultSet ears );

    /**
     * Batch counterpart to {@link #thaw(ExpressionAnalysisResultSet)} for a whole page of result sets.
     * <p>
     * Initializes the same set of lazy associations as the single-element form (analysis,
     * {@code analysis.experimentAnalyzed}, {@code analysis.subsetFactorValue} (+EF),
     * {@code experimentalFactors}, {@code baselineGroup} (+EF)) but issues a small fixed
     * number of queries across the whole collection instead of 5-7 sequential
     * {@code Hibernate.initialize} round-trips per row. Idempotent and safe to call with an
     * empty or {@code null} collection (no-op).
     */
    void thawAll( Collection<ExpressionAnalysisResultSet> ears );

    /**
     * Count the number of results in a given result set.
     */
    long countResults( ExpressionAnalysisResultSet ears );

    /**
     * Count the number of results in a given result set below a given corrected P-value threshold.
     */
    long countResults( ExpressionAnalysisResultSet ears, double threshold );

    /**
     * Retrieve the baseline for the given result set.
     * <p>
     * Factor values are always initialized.
     * @return a baseline, or null if none could be determined for the given result set
     */
    @Nullable
    Baseline getBaseline( ExpressionAnalysisResultSet ears );

    /**
     * Retrieve baselines for all the given result sets representing factor interactions.
     * @param initializeFactorValues whether to initialize factor values
     */
    Map<@MayBeUninitialized ExpressionAnalysisResultSet, Baseline> getBaselinesForInteractions( Collection<@MayBeUninitialized ExpressionAnalysisResultSet> resultSets, boolean initializeFactorValues );

    /**
     * Retrieve baselines using result set IDs representing factor interactions.
     * @param initializeFactorValues whether to initialize factor values
     */
    Map<Long, Baseline> getBaselinesForInteractionsByIds( Collection<Long> ids, boolean initializeFactorValues );

    /**
     * Batch-fetch the {@code experimentalFactors} + {@code baselineGroup} associations of
     * the given result sets in two queries (one per association, to avoid {@code MultipleBagFetchException}
     * and Cartesian explosion on a multi-row join), and return a map keyed by result-set id.
     * <p>
     * Consumed by the {@code findByExperimentIds} VO enrichment path so the per-row
     * {@link DiffExResultSetSummaryValueObject} ctor can skip three sequential
     * {@code Hibernate.initialize} round-trips on a warm cache.
     * <p>
     * Returns an empty map if {@code ids} is empty. Result-sets missing from the result
     * (e.g. concurrently deleted) are simply absent from the map; the caller should
     * fall back to the lazy-load path for those.
     *
     * @param ids the result-set ids to prefetch
     * @return map from result-set id to {@link DiffExResultSetSummaryValueObject.Prefetch}
     */
    Map<Long, DiffExResultSetSummaryValueObject.Prefetch> getPrefetchForVo( Collection<Long> ids );

    /**
     * Obtain the stored histogram of the P-value distribution for a given result set.
     * <p>
     * Read straight out of the {@code PVALUE_DISTRIBUTION} row hanging off the result set; nothing is
     * aggregated. {@code DifferentialExpressionAnalyzerServiceImpl#addPvalueDistribution} writes it at
     * analysis time with 100 fixed-width bins over {@code [0, 1]} of the RAW p-values, so bin
     * {@code i} covers {@code (i/100, (i+1)/100]} and bin 0 also holds {@code 0.0}. There is no stored
     * corrected-p-value histogram.
     *
     * @return the stored histogram, or {@code null} when {@code PVALUE_DISTRIBUTION_FK} is null
     */
    @Nullable
    Histogram loadPvalueDistribution( ExpressionAnalysisResultSet resultSet );

}
