package ubic.gemma.persistence.service.analysis.expression.diff;

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSetValueObject;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.BaseVoEnabledService;
import ubic.gemma.persistence.service.FilteringService;
import ubic.gemma.persistence.service.FilteringVoEnabledService;
import ubic.gemma.persistence.service.analysis.AnalysisResultSetService;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ExpressionAnalysisResultSetService extends AnalysisResultSetService<DifferentialExpressionAnalysisResult, ExpressionAnalysisResultSet>, FilteringVoEnabledService<ExpressionAnalysisResultSet, ExpressionAnalysisResultSetValueObject> {

    ExpressionAnalysisResultSet loadWithResultsAndContrasts( Long value );

    void thaw( ExpressionAnalysisResultSet e );

    ExpressionAnalysisResultSet loadWithExperimentAnalyzed( Long id );

    ExpressionAnalysisResultSetValueObject loadValueObjectWithResults( ExpressionAnalysisResultSet ears );

    Map<DifferentialExpressionAnalysisResult, List<Gene>> loadResultToGenesMap( ExpressionAnalysisResultSet ears );

    Slice<ExpressionAnalysisResultSetValueObject> findByBioAssaySetInAndDatabaseEntryInLimit( Collection<BioAssaySet> bioAssaySets, Collection<DatabaseEntry> externalIds, Filters objectFilters, int offset, int limit, Sort sort );
}
