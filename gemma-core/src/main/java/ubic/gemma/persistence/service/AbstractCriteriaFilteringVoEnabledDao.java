package ubic.gemma.persistence.service;

import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.ObjectFilterCriteriaUtils;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Partial implementation of {@link FilteringVoEnabledDao} based on the Hibernate {@link Criteria} API.
 *
 * @see ubic.gemma.persistence.util.ObjectFilterCriteriaUtils to obtain {@link org.hibernate.criterion.DetachedCriteria}
 * from a {@link Filters}.
 * @see ubic.gemma.persistence.util.AclCriteriaUtils for utilities to include ACL constraints on the VOs at the
 * database-level.
 *
 * @author poirigui
 */
@ParametersAreNonnullByDefault
public abstract class AbstractCriteriaFilteringVoEnabledDao<O extends Identifiable, VO extends IdentifiableValueObject<O>> extends AbstractFilteringVoEnabledDao<O, VO> {

    protected AbstractCriteriaFilteringVoEnabledDao( Class<? extends O> elementClass, SessionFactory sessionFactory ) {
        // This is a good default objet alias for Hibernate Criteria since null is used to refer to the root entity.
        super( null, elementClass, sessionFactory );
    }

    /**
     * Obtain a {@link Criteria} for loading VOs.
     *
     * @see ObjectFilterCriteriaUtils#formRestrictionClause(Filters) to obtain a {@link org.hibernate.criterion.DetachedCriteria}
     * from a set of filter clauses.
     */
    protected Criteria getLoadValueObjectsCriteria( @Nullable Filters filters ) {
        return this.getSessionFactory().getCurrentSession()
                .createCriteria( elementClass, getObjectAlias() )
                .add( ObjectFilterCriteriaUtils.formRestrictionClause( filters ) );
    }

    @Override
    public Slice<VO> loadValueObjectsPreFilter( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
        StopWatch stopWatch = StopWatch.createStarted();
        StopWatch queryStopWatch = StopWatch.create();
        StopWatch countingStopWatch = StopWatch.create();
        StopWatch postProcessingStopWatch = StopWatch.create();

        Criteria query = getLoadValueObjectsCriteria( filters );
        Criteria totalElementsQuery = getLoadValueObjectsCriteria( filters );

        // setup sorting
        if ( sort != null ) {
            query.addOrder( getOrderFromSort( sort ) );
        }

        // setup offset/limit
        if ( offset > 0 )
            query.setFirstResult( offset );
        if ( limit > 0 )
            query.setMaxResults( limit );

        queryStopWatch.start();
        //noinspection unchecked
        List<O> data = query.setResultTransformer( Criteria.DISTINCT_ROOT_ENTITY ).list();
        queryStopWatch.stop();

        countingStopWatch.start();
        Long totalElements = ( Long ) totalElementsQuery
                .setProjection( Projections.countDistinct( "id" ) )
                .uniqueResult();
        countingStopWatch.stop();

        postProcessingStopWatch.start();
        List<VO> results = doLoadValueObjects( data );
        postProcessingStopWatch.stop();

        stopWatch.stop();

        if ( stopWatch.getTime( TimeUnit.MILLISECONDS ) > REPORT_SLOW_QUERY_AFTER_MS ) {
            log.info( String.format( "Loading and counting VOs took %d ms (querying: %d, counting: %d, post-processing: %d).",
                    stopWatch.getTime( TimeUnit.MILLISECONDS ), queryStopWatch.getTime( TimeUnit.MILLISECONDS ),
                    countingStopWatch.getTime( TimeUnit.MILLISECONDS ), postProcessingStopWatch.getTime( TimeUnit.MILLISECONDS ) ) );
        }

        return new Slice<>( results, sort, offset, limit, totalElements );
    }

    @Override
    public List<VO> loadValueObjectsPreFilter( @Nullable Filters objectFilters, @Nullable Sort sort ) {
        StopWatch stopWatch = StopWatch.createStarted();
        StopWatch queryStopWatch = StopWatch.create();
        StopWatch postProcessingStopWatch = StopWatch.create();

        Criteria query = getLoadValueObjectsCriteria( objectFilters );

        if ( sort != null ) {
            query.addOrder( getOrderFromSort( sort ) );
        }

        queryStopWatch.start();
        //noinspection unchecked
        List<O> data = query
                .setResultTransformer( Criteria.DISTINCT_ROOT_ENTITY )
                .list();
        queryStopWatch.stop();

        postProcessingStopWatch.start();
        List<VO> results = this.doLoadValueObjects( data );
        postProcessingStopWatch.stop();

        stopWatch.stop();

        if ( stopWatch.getTime() > REPORT_SLOW_QUERY_AFTER_MS ) {
            log.info( String.format( "Loading VOs for %s took %d ms (querying: %d ms, post-processing: %d ms).",
                    elementClass.getName(), stopWatch.getTime( TimeUnit.MILLISECONDS ), queryStopWatch.getTime( TimeUnit.MILLISECONDS ),
                    postProcessingStopWatch.getTime( TimeUnit.MILLISECONDS ) ) );
        }

        return results;
    }

    private Order getOrderFromSort( Sort sort ) {
        return sort.getDirection() == Sort.Direction.DESC ? Order.desc( sort.getPropertyName() ) : Order.asc( sort.getPropertyName() );
    }
}
