package ubic.gemma.persistence.service;

import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.*;

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
    public ObjectFilter getObjectFilter( String property, ObjectFilter.Operator operator, String value ) {
        try {
            return ObjectFilter.parseObjectFilter( voDao.getObjectAlias(), property, EntityUtils.getDeclaredFieldType( property, voDao.getElementClass() ), operator, value );
        } catch ( NoSuchFieldException e ) {
            throw new IllegalArgumentException( "Could not create object filter for " + property + " on " + voDao.getElementClass().getName() + ".", e );
        }
    }

    @Override
    public ObjectFilter getObjectFilter( String property, ObjectFilter.Operator operator, Collection<String> values ) {
        try {
            return ObjectFilter.parseObjectFilter( voDao.getObjectAlias(), property, EntityUtils.getDeclaredFieldType( property, voDao.getElementClass() ), operator, values );
        } catch ( NoSuchFieldException e ) {
            throw new IllegalArgumentException( "Could not create object filter for " + property + " on " + voDao.getElementClass().getName() + ".", e );
        }
    }

    @Override
    public Sort getSort( String property, Sort.Direction direction ) {
        // this only serves as a pre-condition to ensure that the property exists
        try {
            EntityUtils.getDeclaredField( voDao.getElementClass(), property );
            return Sort.by( voDao.getObjectAlias(), property, direction );
        } catch ( NoSuchFieldException e ) {
            throw new IllegalArgumentException( "Could not resolve property " + property + " on " + voDao.getElementClass().getName() + ".", e );
        }
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
