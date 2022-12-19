package ubic.gemma.persistence.service;

import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Base implementation for {@link FilteringVoEnabledService}.
 */
public abstract class AbstractFilteringVoEnabledService<O extends Identifiable, VO extends IdentifiableValueObject<O>>
        extends AbstractService<O> implements FilteringVoEnabledService<O, VO>, BaseService<O> {

    private final FilteringVoEnabledDao<O, VO> voDao;

    protected AbstractFilteringVoEnabledService( FilteringVoEnabledDao<O, VO> voDao ) {
        super( voDao );
        this.voDao = voDao;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> loadIdsPreFilter( @Nullable Filters filters, @Nullable Sort sort ) {
        return voDao.loadIdsPreFilter( filters, sort );
    }

    @Override
    public List<O> loadValuePreFilter( @Nullable Filters filters, @Nullable Sort sort ) {
        return voDao.loadPreFilter( filters, sort );
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<O> loadValuePreFilter( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
        return voDao.loadPreFilter( filters, sort, offset, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public VO loadValueObject( O entity ) {
        return voDao.loadValueObject( entity );
    }

    @Override
    @Transactional(readOnly = true)
    public VO loadValueObjectById( Long entityId ) {
        return voDao.loadValueObjectById( entityId );
    }

    @Override
    @Transactional(readOnly = true)
    public List<VO> loadValueObjects( Collection<O> entities ) {
        return voDao.loadValueObjects( entities );
    }

    @Override
    @Transactional(readOnly = true)
    public List<VO> loadValueObjectsByIds( Collection<Long> entityIds ) {
        return voDao.loadValueObjectsByIds( entityIds );
    }

    @Override
    @Transactional(readOnly = true)
    public List<VO> loadAllValueObjects() {
        return voDao.loadAllValueObjects();
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<VO> loadValueObjectsPreFilter( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
        return voDao.loadValueObjectsPreFilter( filters, sort, offset, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public List<VO> loadValueObjectsPreFilter( @Nullable Filters filters, @Nullable Sort sort ) {
        return voDao.loadValueObjectsPreFilter( filters, sort );
    }

    @Override
    @Transactional(readOnly = true)
    public long countPreFilter( @Nullable Filters filters ) {
        return voDao.countPreFilter( filters );
    }

    public Set<String> getFilterableProperties() {
        return voDao.getFilterableProperties();
    }

    public Class<?> getFilterablePropertyType( String property ) {
        return voDao.getFilterablePropertyType( property );
    }

    @Nullable
    public String getFilterablePropertyDescription( String property ) {
        return voDao.getFilterablePropertyDescription( property );
    }

    public Filter getFilter( String property, Filter.Operator operator, String value ) throws IllegalArgumentException {
        return voDao.getFilter( property, operator, value );
    }

    public Filter getFilter( String property, Filter.Operator operator, Collection<String> values ) throws IllegalArgumentException {
        return voDao.getFilter( property, operator, values );
    }

    public Sort getSort( String property, @Nullable Sort.Direction direction ) throws IllegalArgumentException {
        return voDao.getSort( property, direction );
    }
}
