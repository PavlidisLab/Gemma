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

import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PostAuthorize;

import javax.annotation.CheckReturnValue;
import org.springframework.lang.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * 🔒 The loaders below are ACL-guarded. Before that they were not, and
 * {@code GET /resultSets/{id}} served the complete result set -- experimental design, factor
 * values and per-probe results with genes -- to anonymous callers for experiments that
 * {@code GET /datasets/{id}} correctly hides. The guards mirror
 * {@link DifferentialExpressionAnalysisService}, which has always had them. A result set's ACL
 * parent chain runs result set -> analysis -> experiment, so READ resolves against the owning
 * experiment.
 */
public interface ExpressionAnalysisResultSetService extends AnalysisResultSetService<DifferentialExpressionAnalysisResult, ExpressionAnalysisResultSet>, FilteringVoEnabledService<ExpressionAnalysisResultSet, DifferentialExpressionAnalysisResultSetValueObject> {

    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    @PostAuthorize("returnObject == null or hasPermission(returnObject, 'READ') or hasPermission(returnObject, 'ADMINISTRATION')")
    ExpressionAnalysisResultSet loadWithAnalysis( Long id );

    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    @PostAuthorize("returnObject == null or hasPermission(returnObject, 'READ') or hasPermission(returnObject, 'ADMINISTRATION')")
    ExpressionAnalysisResultSet loadWithResultsAndContrasts( Long value );

    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    @PostAuthorize("returnObject == null or hasPermission(returnObject, 'READ') or hasPermission(returnObject, 'ADMINISTRATION')")
    ExpressionAnalysisResultSet loadWithResultsAndContrasts( Long value, int offset, int limit );

    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    @PostAuthorize("returnObject == null or hasPermission(returnObject, 'READ') or hasPermission(returnObject, 'ADMINISTRATION')")
    ExpressionAnalysisResultSet loadWithResultsAndContrasts( Long value, double threshold, int offset, int limit );

    long countResults( ExpressionAnalysisResultSet ears );

    long countResults( ExpressionAnalysisResultSet ears, double threshold );

    @CheckReturnValue
    ExpressionAnalysisResultSet thaw( ExpressionAnalysisResultSet e );

    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    @PostAuthorize("returnObject == null or hasPermission(returnObject, 'READ') or hasPermission(returnObject, 'ADMINISTRATION')")
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

}
