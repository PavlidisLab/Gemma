package ubic.gemma.persistence.service.analysis.expression.diff;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSetValueObject;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.AbstractVoEnabledService;
import ubic.gemma.persistence.service.ObjectFilterException;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class ExpressionAnalysisResultSetServiceImpl extends AbstractVoEnabledService<ExpressionAnalysisResultSet, ExpressionAnalysisResultSetValueObject> implements ExpressionAnalysisResultSetService {

    private final ExpressionAnalysisResultSetDao voDao;

    @Autowired
    public ExpressionAnalysisResultSetServiceImpl( ExpressionAnalysisResultSetDao voDao ) {
        super( voDao );
        this.voDao = voDao;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionAnalysisResultSet loadWithResultsAndContrasts( Long value ) {
        return voDao.loadWithResultsAndContrasts( value );
    }

    @Override
    @Transactional(readOnly = true)
    public void thaw( ExpressionAnalysisResultSet e ) {
        voDao.thaw( e );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionAnalysisResultSet loadWithExperimentAnalyzed( Long id ) {
        ExpressionAnalysisResultSet ears = voDao.load( id );
        if ( ears != null ) {
            voDao.thaw( ears );
        }
        return ears;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionAnalysisResultSetValueObject loadValueObjectWithResults( ExpressionAnalysisResultSet ears ) {
        return voDao.loadValueObjectWithResults( ears );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<DifferentialExpressionAnalysisResult, List<Gene>> loadResultToGenesMap( ExpressionAnalysisResultSet resultSet ) {
        return voDao.loadResultToGenesMap( resultSet );
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<ExpressionAnalysisResultSetValueObject> findByBioAssaySetInAndDatabaseEntryInLimit( Collection<BioAssaySet> bioAssaySets, Collection<DatabaseEntry> externalIds, Filters objectFilters, int offset, int limit, Sort sort ) {
        return voDao.findByBioAssaySetInAndDatabaseEntryInLimit( bioAssaySets, externalIds,
                objectFilters,
                offset,
                limit,
                sort );
    }

    @Override
    public String getObjectAlias() {
        return voDao.getObjectAlias();
    }

    @Override
    public ObjectFilter getObjectFilter( String property, ObjectFilter.Operator operator, String value ) throws ObjectFilterException {
        return voDao.getObjectFilter( property, operator, value );
    }

    @Override
    public ObjectFilter getObjectFilter( String property, ObjectFilter.Operator operator, Collection<String> values ) throws ObjectFilterException {
        return voDao.getObjectFilter( property, operator, values );
    }
}
