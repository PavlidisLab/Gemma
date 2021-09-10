package ubic.gemma.persistence.service;

import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.persistence.util.Slice;

import java.util.List;

/**
 * Base implementation for {@link FilteringVoEnabledService}.
 */
public abstract class AbstractFilteringVoEnabledService<O extends Identifiable, VO extends IdentifiableValueObject<O>>
        extends AbstractVoEnabledService<O, VO> implements FilteringVoEnabledService<O, VO> {

    private final FilteringVoEnabledDao<O, VO> voDao;

    protected AbstractFilteringVoEnabledService( FilteringVoEnabledDao<O, VO> voDao ) {
        super( voDao );
        this.voDao = voDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<VO> loadValueObjectsPreFilter( int offset, int limit, String orderBy, boolean asc,
            List<ObjectFilter[]> filter ) {
        return voDao.loadValueObjectsPreFilter( offset, limit, orderBy, asc, filter );
    }
}
