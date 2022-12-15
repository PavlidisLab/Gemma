package ubic.gemma.persistence.service;

import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * Base implementation for {@link FilteringVoEnabledService}.
 */
public abstract class AbstractFilteringVoEnabledService<O extends Identifiable, VO extends IdentifiableValueObject<O>>
        extends AbstractFilteringService<O> implements FilteringVoEnabledService<O, VO> {

    private final FilteringVoEnabledDao<O, VO> voDao;

    protected AbstractFilteringVoEnabledService( FilteringVoEnabledDao<O, VO> voDao ) {
        super( voDao );
        this.voDao = voDao;
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
}
