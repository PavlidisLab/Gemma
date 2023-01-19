package ubic.gemma.persistence.service;

import org.springframework.context.MessageSourceResolvable;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Interface for filtering-capable services.
 * @see FilteringDao
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
     * @see FilteringDao#getFilterablePropertyAvailableValues(String)
     */
    @Nullable
    List<Object> getFilterablePropertyAvailableValues( String property );

    /**
     * Obtain a list of resolvable {@link MessageSourceResolvable}s to be used for user display purposes.
     * <p>
     * Nullity and the number of elements is guaranteed to match {@link #getFilterablePropertyAvailableValues(String)} (String)},
     * so the two methods can be used jointly.
     *
     * @see #getFilterablePropertyResolvableAvailableValues(String)
     */
    @Nullable
    List<MessageSourceResolvable> getFilterablePropertyResolvableAvailableValues( String property );

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

    /**
     * @see FilteringDao#loadIdsPreFilter(Filters, Sort)
     */
    List<Long> loadIdsPreFilter( @Nullable Filters filters, @Nullable Sort sort );

    /**
     * @see FilteringDao#loadPreFilter(Filters, Sort, int, int)
     */
    Slice<O> loadPreFilter( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit );

    /**
     * @see FilteringDao#loadPreFilter(Filters, Sort)
     */
    List<O> loadPreFilter( @Nullable Filters filters, @Nullable Sort sort );

    /**
     * @see FilteringDao#countPreFilter(Filters)
     */
    long countPreFilter( @Nullable Filters filters );
}
