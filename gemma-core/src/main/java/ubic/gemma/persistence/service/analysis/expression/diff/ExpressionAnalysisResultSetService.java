package ubic.gemma.persistence.service.analysis.expression.diff;

import ubic.gemma.core.util.math.distribution.Histogram;
import ubic.gemma.model.analysis.expression.diff.Baseline;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultSetValueObject;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.annotations.MayBeUninitialized;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.FilteringVoEnabledService;
import ubic.gemma.persistence.service.analysis.AnalysisResultSetService;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.CheckReturnValue;
import org.springframework.lang.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface ExpressionAnalysisResultSetService extends AnalysisResultSetService<DifferentialExpressionAnalysisResult, ExpressionAnalysisResultSet>, FilteringVoEnabledService<ExpressionAnalysisResultSet, DifferentialExpressionAnalysisResultSetValueObject> {

    @Nullable
    ExpressionAnalysisResultSet loadWithAnalysis( Long id );

    @Nullable
    ExpressionAnalysisResultSet loadWithResultsAndContrasts( Long value );

    @Nullable
    ExpressionAnalysisResultSet loadWithResultsAndContrasts( Long value, int offset, int limit );

    @Nullable
    ExpressionAnalysisResultSet loadWithResultsAndContrasts( Long value, double threshold, int offset, int limit );

    long countResults( ExpressionAnalysisResultSet ears );

    long countResults( ExpressionAnalysisResultSet ears, double threshold );

    @CheckReturnValue
    ExpressionAnalysisResultSet thaw( ExpressionAnalysisResultSet e );

    @Nullable
    ExpressionAnalysisResultSet loadWithExperimentAnalyzed( Long id );

    DifferentialExpressionAnalysisResultSetValueObject loadValueObjectWithResults( ExpressionAnalysisResultSet ears, boolean includeFactorValuesInContrasts, boolean queryByResult, boolean includeTaxonInGenes );

    Map<Long, Set<Gene>> loadResultIdToGenesMap( ExpressionAnalysisResultSet ears );

    Slice<DifferentialExpressionAnalysisResultSetValueObject> findByBioAssaySetInAndDatabaseEntryInLimit( @Nullable Collection<BioAssaySet> bioAssaySets, @Nullable Collection<DatabaseEntry> externalIds, @Nullable Filters filters, int offset, int limit, @Nullable Sort sort );

    /**
     * Cursor-paged counterpart to {@link #findByBioAssaySetInAndDatabaseEntryInLimit}. Always
     * sorts by ascending {@code id} — see {@code CURSOR_PAGINATION_STEP1_PLAN.md} step 1i.
     */
    CursorPage<DifferentialExpressionAnalysisResultSetValueObject> findByBioAssaySetInAndDatabaseEntryInByCursor( @Nullable Collection<BioAssaySet> bioAssaySets, @Nullable Collection<DatabaseEntry> externalIds, @Nullable Filters filters, @Nullable Cursor cursor, int limit );

    Baseline getBaseline( ExpressionAnalysisResultSet ears );

    Map<@MayBeUninitialized ExpressionAnalysisResultSet, Baseline> getBaselinesForInteractions( Set<@MayBeUninitialized ExpressionAnalysisResultSet> resultSets, boolean initializeFactorValues );

    Map<Long, Baseline> getBaselinesForInteractionsByIds( Collection<Long> rsIds, boolean initializeFactorValues );

    @Nullable
    Histogram loadPvalueDistribution( ExpressionAnalysisResultSet resulSet );

    /**
     * Bin the raw or corrected p-values for a result set into a uniform histogram over {@code [0, 1]}.
     *
     * @see ExpressionAnalysisResultSetDao#binPvalues(Long, String, int)
     */
    long[] binPvalues( Long resultSetId, String column, int numberOfBins );
}
