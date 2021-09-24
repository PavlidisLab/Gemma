package ubic.gemma.persistence.service.analysis.expression.diff;

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSetValueObject;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.persistence.service.FilteringVoEnabledDao;
import ubic.gemma.persistence.service.FilteringVoEnabledService;
import ubic.gemma.persistence.service.analysis.AnalysisResultSetService;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.persistence.util.Slice;

import java.util.Collection;
import java.util.List;

public interface ExpressionAnalysisResultSetService extends AnalysisResultSetService<DifferentialExpressionAnalysisResult, ExpressionAnalysisResultSet>, FilteringVoEnabledService<ExpressionAnalysisResultSet, ExpressionAnalysisResultSetValueObject> {

    ExpressionAnalysisResultSet thaw( ExpressionAnalysisResultSet e );

    ExpressionAnalysisResultSet thawWithoutContrasts( ExpressionAnalysisResultSet ears );

    Slice<ExpressionAnalysisResultSetValueObject> findByBioAssaySetInAndDatabaseEntryInLimit( Collection<BioAssaySet> bioAssaySets, Collection<DatabaseEntry> externalIds, List<ObjectFilter[]> objectFilters, int offset, int limit, String field, boolean isAsc );
}
