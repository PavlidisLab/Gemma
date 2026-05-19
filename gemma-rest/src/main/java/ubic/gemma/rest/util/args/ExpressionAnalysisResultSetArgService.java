package ubic.gemma.rest.util.args;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultSetValueObject;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Filters;

import java.util.Collection;

@Service
public class ExpressionAnalysisResultSetArgService extends AbstractEntityArgService<ExpressionAnalysisResultSet, ExpressionAnalysisResultSetService> {

    @Autowired
    public ExpressionAnalysisResultSetArgService( ExpressionAnalysisResultSetService service ) {
        super( service );
    }

    public ExpressionAnalysisResultSet getEntityWithContrastsAndResults( ExpressionAnalysisResultSetArg analysisResultSet ) {
        return service.loadWithResultsAndContrasts( analysisResultSet.getValue() );
    }

    public ExpressionAnalysisResultSet getEntityWithContrastsAndResults( ExpressionAnalysisResultSetArg analysisResultSet, int offset, int limit ) {
        return service.loadWithResultsAndContrasts( analysisResultSet.getValue(), offset, limit );
    }

    public ExpressionAnalysisResultSet getEntityWithContrastsAndResults( ExpressionAnalysisResultSetArg analysisResultSet, double threshold, int offset, int limit ) {
        return service.loadWithResultsAndContrasts( analysisResultSet.getValue(), threshold, offset, limit );
    }

    /**
     * Cursor-mode counterpart to
     * {@link ExpressionAnalysisResultSetService#findByBioAssaySetInAndDatabaseEntryInLimit}.
     * Always sorts by ascending {@code id} (the primary key, indexed and unique) — see
     * {@code CURSOR_PAGINATION_STEP1_PLAN.md} step 1i. The user's {@code ?filter=} arg still
     * applies; the user's {@code ?sort=} arg is intentionally not honoured in cursor mode
     * because the DAO currently restricts cursors to single-component {@code id}-asc sorts
     * (recce sec. 3.4 — to be lifted in phase B once the index audit is complete).
     */
    public CursorPage<DifferentialExpressionAnalysisResultSetValueObject> getResultSetsByCursor(
            @Nullable Collection<BioAssaySet> bioAssaySets,
            @Nullable Collection<DatabaseEntry> databaseEntries,
            @Nullable Filters filters,
            @Nullable Cursor cursor,
            int limit ) {
        return service.findByBioAssaySetInAndDatabaseEntryInByCursor( bioAssaySets, databaseEntries, filters, cursor, limit );
    }
}
