package ubic.gemma.persistence.service.analysis.expression.diff;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.analysis.expression.diff.Baseline;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultSetValueObject;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.AbstractFilteringVoEnabledService;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ExpressionAnalysisResultSetServiceImpl extends AbstractFilteringVoEnabledService<ExpressionAnalysisResultSet, DifferentialExpressionAnalysisResultSetValueObject> implements ExpressionAnalysisResultSetService {

    private final ExpressionAnalysisResultSetDao voDao;

    @Autowired
    public ExpressionAnalysisResultSetServiceImpl( ExpressionAnalysisResultSetDao voDao ) {
        super( voDao );
        this.voDao = voDao;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionAnalysisResultSet loadWithResultsAndContrasts( Long value ) {
        ExpressionAnalysisResultSet result = voDao.loadWithResultsAndContrasts( value );
        return result != null ? thaw( result ) : null;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionAnalysisResultSet loadWithResultsAndContrasts( Long value, int offset, int limit ) {
        ExpressionAnalysisResultSet result = voDao.loadWithResultsAndContrasts( value, offset, limit );
        return result != null ? thaw( result ) : null;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionAnalysisResultSet loadWithResultsAndContrasts( Long value, double threshold, int offset, int limit ) {
        ExpressionAnalysisResultSet result = voDao.loadWithResultsAndContrasts( value, threshold, offset, limit );
        return result != null ? thaw( result ) : null;
    }

    @Override
    @Transactional(readOnly = true)
    public long countResults( ExpressionAnalysisResultSet ears ) {
        return voDao.countResults( ears );
    }

    @Override
    @Transactional(readOnly = true)
    public long countResults( ExpressionAnalysisResultSet ears, double threshold ) {
        return voDao.countResults( ears, threshold );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionAnalysisResultSet thaw( ExpressionAnalysisResultSet ears ) {
        ears = ensureInSession( ears );
        voDao.thaw( ears );
        return ears;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionAnalysisResultSet loadWithExperimentAnalyzed( Long id ) {
        ExpressionAnalysisResultSet ears = voDao.load( id );
        if ( ears != null ) {
            return thaw( ears );
        }
        return ears;
    }

    @Override
    @Transactional(readOnly = true)
    public DifferentialExpressionAnalysisResultSetValueObject loadValueObjectWithResults( ExpressionAnalysisResultSet ears ) {
        return voDao.loadValueObjectWithResults( ears );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, List<Gene>> loadResultIdToGenesMap( ExpressionAnalysisResultSet resultSet ) {
        return voDao.loadResultToGenesMap( resultSet );
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<DifferentialExpressionAnalysisResultSetValueObject> findByBioAssaySetInAndDatabaseEntryInLimit( @Nullable Collection<BioAssaySet> bioAssaySets, @Nullable Collection<DatabaseEntry> externalIds, @Nullable Filters filters, int offset, int limit, @Nullable Sort sort ) {
        return voDao.findByBioAssaySetInAndDatabaseEntryInLimit( bioAssaySets, externalIds,
                filters,
                offset,
                limit,
                sort );
    }

    @Override
    @Transactional(readOnly = true)
    public Baseline getBaseline( ExpressionAnalysisResultSet ears ) {
        return voDao.getBaseline( ears );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionAnalysisResultSet, Baseline> getBaselinesForInteractions( Set<ExpressionAnalysisResultSet> resultSets ) {
        return voDao.getBaselinesForInteractions( resultSets );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Baseline> getBaselinesForInteractionsByIds( Collection<Long> rsIds ) {
        return voDao.getBaselinesForInteractionsByIds( rsIds );
    }
}
