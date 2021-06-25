package ubic.gemma.persistence.service.analysis.expression.diff;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSetValueObject;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.persistence.service.AbstractVoEnabledService;
import ubic.gemma.persistence.util.ObjectFilter;

import java.util.ArrayList;
import java.util.Collection;

@Service
public class ExpressionAnalysisResultSetServiceImpl extends AbstractVoEnabledService<ExpressionAnalysisResultSet, ExpressionAnalysisResultSetValueObject> implements ExpressionAnalysisResultSetService {

    private final ExpressionAnalysisResultSetDao voDao;

    @Autowired
    public ExpressionAnalysisResultSetServiceImpl( ExpressionAnalysisResultSetDao voDao ) {
        super( voDao );
        this.voDao = voDao;
    }

    @Override
    public ExpressionAnalysisResultSet thaw( ExpressionAnalysisResultSet e ) {
        return voDao.thaw( e );
    }

    @Override
    public ExpressionAnalysisResultSet thawWithoutContrasts( ExpressionAnalysisResultSet ears ) {
        return voDao.thawWithoutContrasts( ears );
    }

    @Override
    public Collection<ExpressionAnalysisResultSet> findByBioAssaySetInAndDatabaseEntryInLimit( Collection<BioAssaySet> bioAssaySets, Collection<DatabaseEntry> externalIds, ArrayList<ObjectFilter[]> objectFilters, int offset, int limit, String orderBy, boolean isAsc ) {
        return voDao.findByBioAssaySetInAndDatabaseEntryInLimit( bioAssaySets, externalIds,
                objectFilters,
                offset,
                limit,
                orderBy,
                isAsc );
    }
}
