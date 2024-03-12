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

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultSetValueObject;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.FilteringVoEnabledDao;
import ubic.gemma.persistence.service.TableMaintenanceUtil;
import ubic.gemma.persistence.service.analysis.AnalysisResultSetDao;
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

    @Nullable
    ExpressionAnalysisResultSet loadWithResultsAndContrasts( Long id );

    boolean canDelete( DifferentialExpressionAnalysis differentialExpressionAnalysis );

    /**
     * Load an analysis result set with its all of its associated results.
     * @see #loadValueObject(Identifiable)
     */
    DifferentialExpressionAnalysisResultSetValueObject loadValueObjectWithResults( ExpressionAnalysisResultSet resultSet );

    /**
     * Load a {@link DifferentialExpressionAnalysisResult} to {@link Gene} multi-map.
     * <p>
     * This is much faster than navigating through the probe's alignments, transcripts and then genes as it uses the
     * internal GENE2CS table described in {@link TableMaintenanceUtil#updateGene2CsEntries()}.
     * <p>
     * Note: Not all probes have associated genes, so you should use {@link Map#getOrDefault(Object, Object)} with an
     * empty collection to handle this case.
     */
    Map<Long, List<Gene>> loadResultToGenesMap( ExpressionAnalysisResultSet resultSet );

    /**
     * Retrieve result sets associated to a set of {@link BioAssaySet} and external database entries.
     *
     * @param bioAssaySets related {@link BioAssaySet}, or any if null
     * @param databaseEntries related external identifier associated to the {@link BioAssaySet}, or any if null
     * @param filters filters for restricting results
     * @param limit maximum number of results to return
     * @param sort field and direction by which the collection is ordered
     */
    Slice<DifferentialExpressionAnalysisResultSetValueObject> findByBioAssaySetInAndDatabaseEntryInLimit( @Nullable Collection<BioAssaySet> bioAssaySets, @Nullable Collection<DatabaseEntry> databaseEntries, @Nullable Filters filters, int offset, int limit, @Nullable Sort sort );

    /**
     * Initialize the analysis and subset factor vale.
     */
    void thaw( ExpressionAnalysisResultSet ears );
}
