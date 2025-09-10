package ubic.gemma.persistence.service;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.security.access.ConfigAttribute;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.*;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Interface for filtering-capable services.
 * @see FilteringDao
 */
public interface FilteringService<O extends Identifiable> extends BaseReadOnlyService<O> {

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
     * @see FilteringDao#getFilterablePropertyAllowedValues(String)
     */
    @Nullable
    List<Object> getFilterablePropertyAllowedValues( String property );

    /**
     * Obtain a list of resolvable {@link MessageSourceResolvable}s to be used for user display purposes.
     * <p>
     * Nullity and the number of elements is guaranteed to match {@link #getFilterablePropertyAllowedValues(String)} (String)},
     * so the two methods can be used jointly.
     *
     * @see #getFilterablePropertyResolvableAllowedValuesLabels(String)
     */
    @Nullable
    List<MessageSourceResolvable> getFilterablePropertyResolvableAllowedValuesLabels( String property );

    /**
     * @see FilteringDao#getFilterablePropertyIsUsingSubquery(String)
     */
    boolean getFilterablePropertyIsUsingSubquery( String property );

    /**
     * Obtain the Spring Security config attributes for a given property.
     * @return the config attributes, or null if no security check should be performed
     */
    @Nullable
    Collection<ConfigAttribute> getFilterablePropertyConfigAttributes( String property );

    /**
     * @see FilteringDao#getFilterableProperties()
     */
    Filter getFilter( String property, Filter.Operator operator, String value ) throws IllegalArgumentException;

    Filter getFilter( String property, Filter.Operator operator, String value, SubqueryMode subqueryMode ) throws IllegalArgumentException;

    /**
     * @see FilteringDao#getFilter(String, Filter.Operator, Collection)
     */
    Filter getFilter( String property, Filter.Operator operator, Collection<String> values ) throws IllegalArgumentException;

    Filter getFilter( String property, Filter.Operator operator, Collection<String> values, SubqueryMode subqueryMode ) throws IllegalArgumentException;

    /**
     * @see FilteringDao#getFilter(String, Class, Filter.Operator, Object)
     */
    <T> Filter getFilter( String property, Class<T> propertyType, Filter.Operator operator, T value );

    /**
     * @see FilteringDao#getFilter(String, Class, Filter.Operator, Collection)
     */
    <T> Filter getFilter( String property, Class<T> propertyType, Filter.Operator operator, Collection<T> parsedValues );

    /**
     * @see FilteringDao#getSort(String, Sort.Direction, Sort.NullMode)
     */
    Sort getSort( String property, @Nullable Sort.Direction direction, Sort.NullMode last ) throws IllegalArgumentException;

    /**
     * @see FilteringDao#loadIds(Filters, Sort)
     */
    List<Long> loadIds( @Nullable Filters filters, @Nullable Sort sort );

    /**
     * @see FilteringDao#load(Filters, Sort, int, int)
     */
    Slice<O> load( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit );

    /**
     * @see FilteringDao#load(Filters, Sort)
     */
    List<O> load( @Nullable Filters filters, @Nullable Sort sort );

    /**
     * @see FilteringDao#count(Filters)
     */
    long count( @Nullable Filters filters );
}
