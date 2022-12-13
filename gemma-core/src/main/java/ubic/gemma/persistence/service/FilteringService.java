package ubic.gemma.persistence.service;

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Set;

/**
 * @see FilteringDao
 * @author poirigui
 */
public interface FilteringService<O extends Identifiable> extends BaseService<O> {

    /**
     * @see FilteringDao#getFilterableProperties()
     */
    Set<String> getFilterableProperties();

    /**
     * @see FilteringDao#getFilterablePropertyType(String)
     */
    Class<?> getFilterablePropertyType( String property );

    /**
     * @see FilteringDao#getFilterablePropertyDescription(String)
     */
    @Nullable
    String getFilterablePropertyDescription( String property );

    /**
     * @see FilteringDao#getFilterableProperties()
     */
    ObjectFilter getObjectFilter( String property, ObjectFilter.Operator operator, String value ) throws IllegalArgumentException;

    /**
     * @see FilteringDao#getObjectFilter(String, ObjectFilter.Operator, Collection)
     */
    ObjectFilter getObjectFilter( String property, ObjectFilter.Operator operator, Collection<String> values ) throws IllegalArgumentException;

    /**
     * @see FilteringDao#getSort(String, Sort.Direction)
     */
    Sort getSort( String property, @Nullable Sort.Direction direction ) throws IllegalArgumentException;
}
