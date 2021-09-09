package ubic.gemma.persistence.service;

import org.hibernate.Query;
import org.hibernate.SessionFactory;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Base implementation for {@link FilteringVoEnabledDao}.
 *
 * @param <O>
 * @param <VO>
 * @author poirigui
 */
public abstract class AbstractFilteringVoEnabledDao<O extends Identifiable, VO extends IdentifiableValueObject<O>> extends AbstractVoEnabledDao<O, VO> implements FilteringVoEnabledDao<O, VO> {

    protected AbstractFilteringVoEnabledDao( Class<O> elementClass, SessionFactory sessionFactory ) {
        super( elementClass, sessionFactory );
    }

    /**
     * Produce a query for retrieving value objects after applying a set of filters and a given ordering.
     *
     * Note that if your implementation does not produce a {@link List<O>} when {@link Query#list()} is invoked, you
     * must override {@link #loadValueObjectsPreFilter(int, int, String, boolean, List)}.
     *
     * @param filters
     * @param orderByProperty
     * @param orderDesc
     * @return a {@link Query} that produce a list of {@link O}
     */
    protected abstract Query getLoadValueObjectsQuery( List<ObjectFilter[]> filters, String orderByProperty, boolean orderDesc );

    /**
     * Produce a query that will be used to retrieve the size of {@link #getLoadValueObjectsQuery(List, String, boolean)}.
     * @param filters
     * @return a {@link Query} which must return a single {@link Long} value
     */
    protected abstract Query getCountValueObjectsQuery( List<ObjectFilter[]> filters );

    /**
     * Load value objects according to a set of filters, ordering and offset/limit constraints.
     *
     * Note that {@link #loadValueObject} is used to create the actual value object from the result set.
     *
     * @param offset
     * @param limit
     * @param orderBy
     * @param asc
     * @param filter
     * @return a {@link Slice} of value objects
     */
    @Override
    public Slice<VO> loadValueObjectsPreFilter( int offset, int limit, String orderBy, boolean asc,
            List<ObjectFilter[]> filter ) {
        Query query = this.getLoadValueObjectsQuery( filter, orderBy, !asc );
        Query totalElementsQuery = getCountValueObjectsQuery( filter );

        // setup offset/limit
        if ( limit > 0 )
            query.setMaxResults( limit );
        query.setFirstResult( offset );

        //noinspection unchecked
        List<O> list = query.list();

        List<VO> vos = list.stream()
                .map( this::loadValueObject )
                .collect( Collectors.toList() );

        Long totalElements = ( Long ) totalElementsQuery.uniqueResult();

        return new Slice<>( vos, new Sort( orderBy, asc ? Sort.Direction.ASC : Sort.Direction.DESC ), offset, limit, totalElements );
    }
}
