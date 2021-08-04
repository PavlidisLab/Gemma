package ubic.gemma.persistence.service.analysis.expression.diff;

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSetValueObject;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.persistence.service.BaseService;
import ubic.gemma.persistence.service.BaseVoEnabledService;
import ubic.gemma.persistence.service.analysis.AnalysisResultSetService;
import ubic.gemma.persistence.util.ObjectFilter;

import java.util.ArrayList;
import java.util.Collection;

public interface ExpressionAnalysisResultSetService extends AnalysisResultSetService<DifferentialExpressionAnalysisResult, ExpressionAnalysisResultSet> {

    ExpressionAnalysisResultSet thaw( ExpressionAnalysisResultSet e );

    ExpressionAnalysisResultSet thawWithoutContrasts( ExpressionAnalysisResultSet ears );

    Collection<ExpressionAnalysisResultSet> findByBioAssaySetInAndDatabaseEntryInLimit( Collection<BioAssaySet> bioAssaySets, Collection<DatabaseEntry> externalIds, ArrayList<ObjectFilter[]> objectFilters, int offset, int limit, String field, boolean isAsc );
}
