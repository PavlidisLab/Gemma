package ubic.gemma.persistence.service;

import org.apache.commons.lang3.NotImplementedException;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.EntityUtils;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import java.util.*;
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

    @Override
    public ObjectFilter getObjectFilter( String property, ObjectFilter.Operator operator, String value ) throws ObjectFilterException {
        try {
            return ObjectFilter.parseObjectFilter( getObjectAlias(), property, EntityUtils.getDeclaredFieldType( property, elementClass ), operator, value );
        } catch ( NoSuchFieldException e ) {
            throw new ObjectFilterException( "Could not create an object filter for " + property + ".", e );
        }
    }

    @Override
    public ObjectFilter getObjectFilter( String property, ObjectFilter.Operator operator, Collection<String> values ) throws ObjectFilterException {
        try {
            return ObjectFilter.parseObjectFilter( getObjectAlias(), property, EntityUtils.getDeclaredFieldType( property, elementClass ), operator, values );
        } catch ( NoSuchFieldException e ) {
            throw new ObjectFilterException( "Could not create an object filter for " + property + " using a collection.", e );
        }
    }

    /**
     * Produce a query for retrieving value objects after applying a set of filters and a given ordering.
     *
     * Note that if your implementation does not produce a {@link List<O>} when {@link Query#list()} is invoked, you
     * must override {@link FilteringVoEnabledDao#loadValueObjectsPreFilter(Filters, Sort, int, int)}.
     *
     * @return a {@link Query} that produce a list of {@link O}
     */
    protected abstract Query getLoadValueObjectsQuery( Filters filters, Sort sort );

    /**
     * Produce a query that will be used to retrieve the size of {@link #getLoadValueObjectsQuery(Filters, Sort)}.
     * @param filters
     * @return a {@link Query} which must return a single {@link Long} value
     */
    protected Query getCountValueObjectsQuery( Filters filters ) {
        throw new NotImplementedException( "Counting " + elementClass + " is not supported." );
    }

    /**
     * Process a result from {@link #getLoadValueObjectsQuery(Filters, Sort)}.
     *
     * By default, it will cast the result into a {@link O} and then apply {@link #loadValueObject(Identifiable)} to
     * obtain a value object.
     *
     * @return a value object, or null, and it will be ignored when constructing the {@link Slice} in {@link FilteringVoEnabledDao#loadValueObjectsPreFilter(Filters, Sort, int, int)}
     */
    protected VO processLoadValueObjectsQueryResult( Object result ) {
        return loadValueObject( ( O ) result );
    }

    @Override
    public Slice<VO> loadValueObjectsPreFilter( Filters filters, Sort sort, int offset, int limit ) {
        Query query = this.getLoadValueObjectsQuery( filters, sort );
        Query totalElementsQuery = getCountValueObjectsQuery( filters );

        // setup offset/limit
        if ( offset > 0 )
            query.setFirstResult( offset );
        if ( limit > 0 )
            query.setMaxResults( limit );

        //noinspection unchecked
        List<?> list = query.list();

        List<VO> vos = list.stream()
                .map( this::processLoadValueObjectsQueryResult )
                .filter( Objects::nonNull )
                .collect( Collectors.toList() );

        Long totalElements = ( Long ) totalElementsQuery.uniqueResult();

        return new Slice<>( vos, sort, offset, limit, totalElements );
    }

    @Override
    public List<VO> loadValueObjectsPreFilter( Filters filters, Sort sort ) {
        Query query = this.getLoadValueObjectsQuery( filters, sort );

        //noinspection unchecked
        List<?> list = query.list();

        return list.stream()
                .map( this::processLoadValueObjectsQueryResult )
                .filter( Objects::nonNull )
                .collect( Collectors.toList() );
    }
}
