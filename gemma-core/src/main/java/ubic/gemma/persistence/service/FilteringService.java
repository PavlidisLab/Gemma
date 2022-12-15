package ubic.gemma.persistence.service;

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.Filter;
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
    Filter getFilter( String property, Filter.Operator operator, String value ) throws IllegalArgumentException;

    /**
     * @see FilteringDao#getFilter(String, Filter.Operator, Collection)
     */
    Filter getFilter( String property, Filter.Operator operator, Collection<String> values ) throws IllegalArgumentException;

    /**
     * @see FilteringDao#getSort(String, Sort.Direction)
     */
    Sort getSort( String property, @Nullable Sort.Direction direction ) throws IllegalArgumentException;
}
