package ubic.gemma.persistence.service;

import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.ObjectFilter;

import java.util.Collection;
import java.util.List;

/**
 * Created by tesarst on 01/06/17.
 * A special case of Service that also provides value object functionality.
 */
@Deprecated
public abstract class AbstractVoEnabledService<O extends Identifiable, VO extends IdentifiableValueObject<O>>
        extends AbstractService<O> implements BaseVoEnabledService<O,VO> {

    private final BaseVoEnabledDao<O, VO> voDao;

    protected AbstractVoEnabledService( BaseVoEnabledDao<O, VO> voDao ) {
        super( voDao );
        this.voDao = voDao;
    }

    @Override
    @Transactional(readOnly = true)
    public VO loadValueObject( O entity ) {
        return entity == null ? null : voDao.loadValueObject( entity );
    }

    @Override
    @Transactional(readOnly = true)
    public List<VO> loadValueObjects( Collection<O> entities ) {
        return entities == null ? null : voDao.loadValueObjects( entities );
    }

    @Override
    @Transactional(readOnly = true)
    public List<VO> loadAllValueObjects() {
        return voDao.loadAllValueObjects();
    }

    @Override
    public List<VO> loadValueObjectsPreFilter( int offset, int limit, String orderBy, boolean asc,
            List<ObjectFilter[]> filter ) {
        return voDao.loadValueObjectsPreFilter( offset, limit, orderBy, asc, filter );
    }

}
