package ubic.gemma.persistence.service;

import lombok.Value;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.*;

import javax.annotation.Nullable;
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
            ObjectFilterPropertyMeta propertyMeta = getObjectFilterPropertyMeta( property );
            return ObjectFilter.parseObjectFilter( propertyMeta.objectAlias, propertyMeta.propertyName, propertyMeta.propertyType, operator, value );
        } catch ( NoSuchFieldException e ) {
            throw new IllegalArgumentException( "Could not create object filter for " + property + " on " + voDao.getElementClass().getName() + ".", e );
        }
    }

    @Override
    public ObjectFilter getObjectFilter( String property, ObjectFilter.Operator operator, Collection<String> values ) {
        try {
            ObjectFilterPropertyMeta propertyMeta = getObjectFilterPropertyMeta( property );
            return ObjectFilter.parseObjectFilter( propertyMeta.objectAlias, propertyMeta.propertyName, propertyMeta.propertyType, operator, values );
        } catch ( NoSuchFieldException e ) {
            throw new IllegalArgumentException( "Could not create object filter for " + property + " on " + voDao.getElementClass().getName() + ".", e );
        }
    }

    @Override
    public Sort getSort( String property, @Nullable Sort.Direction direction ) {
        // this only serves as a pre-condition to ensure that the propertyName exists
        try {
            ObjectFilterPropertyMeta propertyMeta = getObjectFilterPropertyMeta( property );
            return Sort.by( propertyMeta.objectAlias, propertyMeta.propertyName, direction );
        } catch ( NoSuchFieldException e ) {
            throw new IllegalArgumentException( String.format( "Could not resolve propertyName '%s' on %s.", property, voDao.getElementClass().getName() ), e );
        }
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

    @Value
    protected static class ObjectFilterPropertyMeta {
        String objectAlias;
        String propertyName;
        Class<?> propertyType;
    }

    /**
     * Obtain various meta-information used to infer what to use in a {@link ObjectFilter} or {@link Sort}.
     *
     * This is used by {@link #getObjectFilter(String, ObjectFilter.Operator, String)} and {@link #getSort(String, Sort.Direction)}.
     *
     * @throws NoSuchFieldException if no such propertyName exists in {@link O}
     * @see #getObjectFilter(String, ObjectFilter.Operator, String)
     * @see #getObjectFilter(String, ObjectFilter.Operator, Collection)
     * @see #getSort(String, Sort.Direction)
     */
    protected ObjectFilterPropertyMeta getObjectFilterPropertyMeta( String propertyName ) throws NoSuchFieldException {
        EntityUtils.getDeclaredField( voDao.getElementClass(), propertyName );
        return new ObjectFilterPropertyMeta( voDao.getObjectAlias(), propertyName, EntityUtils.getDeclaredFieldType( propertyName, voDao.getElementClass() ) );
    }
}
