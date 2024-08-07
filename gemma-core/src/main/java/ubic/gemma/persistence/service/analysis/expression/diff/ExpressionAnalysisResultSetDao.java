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
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.FilteringVoEnabledDao;
import ubic.gemma.persistence.service.analysis.AnalysisResultSetDao;
import ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
     * @param includeFactorValuesInContrasts
     * @param queryGenesByResult             query genes by results instead of result set, this is considerably faster if the
     *                                       results are sliced (i.e. from {@link #loadWithResultsAndContrasts(Long, int, int)})
     * @param includeTaxonInGenes
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
    Map<Long, List<Gene>> loadResultToGenesMap( ExpressionAnalysisResultSet resultSet, boolean queryByResult );

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
     * Initialize the analysis and subset factor vale.
     */
    void thaw( ExpressionAnalysisResultSet ears );

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
    Map<ExpressionAnalysisResultSet, Baseline> getBaselinesForInteractions( Collection<ExpressionAnalysisResultSet> resultSets, boolean initializeFactorValues );

    /**
     * Retrieve baselines using result set IDs representing factor interactions.
     * @param initializeFactorValues whether to initialize factor values
     */
    Map<Long, Baseline> getBaselinesForInteractionsByIds( Collection<Long> ids, boolean initializeFactorValues );
}
