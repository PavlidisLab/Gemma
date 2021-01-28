package ubic.gemma.persistence.service.analysis.expression.diff;

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSetValueObject;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.persistence.service.BaseService;
import ubic.gemma.persistence.service.BaseVoEnabledService;
import ubic.gemma.persistence.service.analysis.AnalysisResultSetService;

import java.util.Collection;

public interface ExpressionAnalysisResultSetService extends BaseService<ExpressionAnalysisResultSet>, BaseVoEnabledService<ExpressionAnalysisResultSet, ExpressionAnalysisResultSetValueObject>, AnalysisResultSetService<DifferentialExpressionAnalysisResult, ExpressionAnalysisResultSet, ExpressionAnalysisResultSetValueObject> {

    Collection<ExpressionAnalysisResultSet> findByBioAssaySetInAndDatabaseEntryInLimit( Collection<BioAssaySet> bioAssaySets, Collection<DatabaseEntry> externalIds, int limit );
}
