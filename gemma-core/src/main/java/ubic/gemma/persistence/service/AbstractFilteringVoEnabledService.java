package ubic.gemma.persistence.service;

import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.ObjectFilter;

import java.util.List;

/**
 * Base implementation for {@link FilteringVoEnabledService}.
 */
@Deprecated
public abstract class AbstractFilteringVoEnabledService<O extends Identifiable, VO extends IdentifiableValueObject<O>>
        extends AbstractVoEnabledService<O, VO> implements FilteringVoEnabledService<O, VO> {

    private final FilteringVoEnabledDao<O, VO> voDao;

    protected AbstractFilteringVoEnabledService( FilteringVoEnabledDao<O, VO> voDao ) {
        super( voDao );
        this.voDao = voDao;
    }

    @Override
    public List<VO> loadValueObjectsPreFilter( int offset, int limit, String orderBy, boolean asc,
            List<ObjectFilter[]> filter ) {
        return voDao.loadValueObjectsPreFilter( offset, limit, orderBy, asc, filter );
    }
}
