package ubic.gemma.persistence.service.analysis.expression.diff;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSetValueObject;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.persistence.service.AbstractVoEnabledService;

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
    public Collection<ExpressionAnalysisResultSet> findByBioAssaySetInAndDatabaseEntryInLimit( Collection<BioAssaySet> bioAssaySets, Collection<DatabaseEntry> externalIds, int limit ) {
        return voDao.findByBioAssaySetInAndDatabaseEntryInLimit( bioAssaySets, externalIds, limit );
    }
}
