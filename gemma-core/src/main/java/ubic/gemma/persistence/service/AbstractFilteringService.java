package ubic.gemma.persistence.service;

import lombok.Value;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.*;

import javax.annotation.Nullable;
import java.util.Collection;

public class AbstractFilteringService<O extends Identifiable> extends AbstractService<O> implements FilteringService<O> {

    private final FilteringDao<O> mainDao;

    protected AbstractFilteringService( FilteringDao<O> mainDao ) {
        super( mainDao );
        this.mainDao = mainDao;
    }

    @Override
    public ObjectFilter getObjectFilter( String property, ObjectFilter.Operator operator, String value ) {
        try {
            ObjectFilterPropertyMeta propertyMeta = getObjectFilterPropertyMeta( property );
            return ObjectFilter.parseObjectFilter( propertyMeta.objectAlias, propertyMeta.propertyName, propertyMeta.propertyType, operator, value );
        } catch ( NoSuchFieldException e ) {
            throw new IllegalArgumentException( "Could not create object filter for " + property + " on " + mainDao.getElementClass().getName() + ".", e );
        }
    }

    @Override
    public ObjectFilter getObjectFilter( String property, ObjectFilter.Operator operator, Collection<String> values ) {
        try {
            ObjectFilterPropertyMeta propertyMeta = getObjectFilterPropertyMeta( property );
            return ObjectFilter.parseObjectFilter( propertyMeta.objectAlias, propertyMeta.propertyName, propertyMeta.propertyType, operator, values );
        } catch ( NoSuchFieldException e ) {
            throw new IllegalArgumentException( "Could not create object filter for " + property + " on " + mainDao.getElementClass().getName() + ".", e );
        }
    }

    @Override
    public Sort getSort( String property, @Nullable Sort.Direction direction ) {
        // this only serves as a pre-condition to ensure that the propertyName exists
        try {
            ObjectFilterPropertyMeta propertyMeta = getObjectFilterPropertyMeta( property );
            return Sort.by( propertyMeta.objectAlias, propertyMeta.propertyName, direction );
        } catch ( NoSuchFieldException e ) {
            throw new IllegalArgumentException( String.format( "Could not resolve propertyName '%s' on %s.", property, mainDao.getElementClass().getName() ), e );
        }
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
        return new ObjectFilterPropertyMeta( mainDao.getObjectAlias(), propertyName, EntityUtils.getDeclaredFieldType( propertyName, mainDao.getElementClass() ) );
    }
}
