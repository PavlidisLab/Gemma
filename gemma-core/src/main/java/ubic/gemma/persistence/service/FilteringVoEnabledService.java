package ubic.gemma.persistence.service;

import ubic.gemma.model.IdentifiableValueObject;
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
 * Interface VO-enabled service with filtering capabilities.
 */
public interface FilteringVoEnabledService<O extends Identifiable, VO extends IdentifiableValueObject<O>>
        extends BaseVoEnabledService<O, VO>, BaseService<O> {

    /**
     * @see FilteringVoEnabledDao#getFilterableProperties()
     */
    Set<String> getFilterableProperties();

    /**
     * @see FilteringVoEnabledDao#getFilterablePropertyType(String)
     */
    Class<?> getFilterablePropertyType( String property );

    /**
     * @see FilteringVoEnabledDao#getFilterablePropertyDescription(String)
     */
    @Nullable
    String getFilterablePropertyDescription( String property );

    /**
     * @see FilteringVoEnabledDao#getFilterableProperties()
     */
    Filter getFilter( String property, Filter.Operator operator, String value ) throws IllegalArgumentException;

    /**
     * @see FilteringVoEnabledDao#getFilter(String, Filter.Operator, Collection)
     */
    Filter getFilter( String property, Filter.Operator operator, Collection<String> values ) throws IllegalArgumentException;

    /**
     * @see FilteringVoEnabledDao#getSort(String, Sort.Direction)
     */
    Sort getSort( String property, @Nullable Sort.Direction direction ) throws IllegalArgumentException;

    /**
     * @see FilteringVoEnabledDao#loadValueObjectsPreFilter(Filters, Sort, int, int)
     */
    Slice<VO> loadValueObjectsPreFilter( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit );

    /**
     * @see FilteringVoEnabledDao#loadValueObjectsPreFilter(Filters, Sort)
     */
    List<VO> loadValueObjectsPreFilter( @Nullable Filters filters, @Nullable Sort sort );

    long countValueObjectsPreFilter( @Nullable Filters filters );
}