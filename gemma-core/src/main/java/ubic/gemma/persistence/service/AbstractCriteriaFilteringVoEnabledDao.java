package ubic.gemma.persistence.service;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.*;

import java.util.List;

/**
 * Partial implementation of {@link FilteringVoEnabledDao} based on the Hibernate {@link Criteria} API.
 *
 * @see ubic.gemma.persistence.util.ObjectFilterCriteriaUtils to obtain {@link org.hibernate.criterion.DetachedCriteria}
 * from a {@link Filters}.
 * @see ubic.gemma.persistence.util.AclCriteriaUtils for utilities to include ACL constraints on the VOs at the
 * database-level.
 */
public abstract class AbstractCriteriaFilteringVoEnabledDao<O extends Identifiable, VO extends IdentifiableValueObject<O>> extends AbstractFilteringVoEnabledDao<O, VO> {

    protected AbstractCriteriaFilteringVoEnabledDao( Class elementClass, SessionFactory sessionFactory ) {
        super( elementClass, sessionFactory );
    }

    /**
     * For the Criterion API, NULL refers to the root entity.
     *
     * Override this if you use a custom alias for the root entity.
     *
     * @return
     */
    @Override
    public String getObjectAlias() {
        return null;
    }

    /**
     * Obtain a {@link Criteria}.
     *
     * @see ObjectFilterCriteriaUtils#formRestrictionClause(Filters) to obtain a {@link org.hibernate.criterion.DetachedCriteria}
     * from a set of filter clauses.
     */
    protected abstract Criteria getLoadValueObjectsCriteria( Filters objectFilters );

    @Override
    public Slice<VO> loadValueObjectsPreFilter( Filters objectFilters, Sort sort, int offset, int limit ) {
        Criteria query = getLoadValueObjectsCriteria( objectFilters );
        Criteria totalElementsQuery = getLoadValueObjectsCriteria( objectFilters );

        // setup offset/limit
        if ( offset > 0 )
            query.setFirstResult( offset );
        if ( limit > 0 )
            query.setMaxResults( limit );

        log.info( "Query: " + query );

        //noinspection unchecked
        List<O> data = query.setResultTransformer( Criteria.DISTINCT_ROOT_ENTITY ).list();

        Long totalElements = ( Long ) totalElementsQuery
                .setProjection( Projections.countDistinct( "id" ) )
                .uniqueResult();

        //noinspection unchecked
        return new Slice<>( super.loadValueObjects( data ), sort, offset, limit, totalElements );
    }

    @Override
    public List<VO> loadValueObjectsPreFilter( Filters objectFilters, Sort sort ) {
        Criteria query = getLoadValueObjectsCriteria( objectFilters );

        //noinspection unchecked
        List<O> data = query.setResultTransformer( Criteria.DISTINCT_ROOT_ENTITY ).list();

        //noinspection unchecked
        return super.loadValueObjects( data );
    }
}
