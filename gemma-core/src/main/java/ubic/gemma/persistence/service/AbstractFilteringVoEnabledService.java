package ubic.gemma.persistence.service;

import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import java.util.Collection;
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

    @Override
    @Transactional(readOnly = true)
    public Slice<VO> loadValueObjectsPreFilter( Filters filters, Sort sort, int offset, int limit ) {
        return voDao.loadValueObjectsPreFilter( filters, sort, offset, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public List<VO> loadValueObjectsPreFilter( Filters filters, Sort sort ) {
        return voDao.loadValueObjectsPreFilter( filters, sort );
    }

}
