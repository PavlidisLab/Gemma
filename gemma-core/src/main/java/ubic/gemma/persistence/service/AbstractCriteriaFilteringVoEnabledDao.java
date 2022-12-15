package ubic.gemma.persistence.service;

import lombok.Value;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.internal.CriteriaImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.FilterCriteriaUtils;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static ubic.gemma.persistence.util.FilterQueryUtils.formPropertyName;

/**
 * Partial implementation of {@link FilteringVoEnabledDao} based on the Hibernate {@link Criteria} API.
 *
 * @see FilterCriteriaUtils to obtain {@link org.hibernate.criterion.DetachedCriteria}
 * from a {@link Filters}.
 * @see ubic.gemma.persistence.util.AclCriteriaUtils for utilities to include ACL constraints on the VOs at the
 * database-level.
 *
 * @author poirigui
 */
public abstract class AbstractCriteriaFilteringVoEnabledDao<O extends Identifiable, VO extends IdentifiableValueObject<O>> extends AbstractFilteringVoEnabledDao<O, VO> {

    protected AbstractCriteriaFilteringVoEnabledDao( Class<? extends O> elementClass, SessionFactory sessionFactory ) {
        // This is a good default objet alias for Hibernate Criteria since null is used to refer to the root entity.
        super( null, elementClass, sessionFactory );
    }

    /**
     * Obtain a {@link Criteria} for loading VOs.
     *
     * @see FilterCriteriaUtils#formRestrictionClause(Filters) to obtain a {@link org.hibernate.criterion.DetachedCriteria}
     * from a set of filter clauses.
     */
    protected Criteria getLoadValueObjectsCriteria( @Nullable Filters filters ) {
        return this.getSessionFactory().getCurrentSession()
                .createCriteria( elementClass )
                .add( FilterCriteriaUtils.formRestrictionClause( filters ) );
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
            addOrder( query, sort );
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
    public List<VO> loadValueObjectsPreFilter( @Nullable Filters filters, @Nullable Sort sort ) {
        StopWatch stopWatch = StopWatch.createStarted();
        StopWatch queryStopWatch = StopWatch.create();
        StopWatch postProcessingStopWatch = StopWatch.create();

        Criteria query = getLoadValueObjectsCriteria( filters );

        if ( sort != null ) {
            addOrder( query, sort );
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

    @Value
    protected static class FilterablePropertyAlias {
        String propertyName;
        String alias;
    }

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    /**
     * Unfortunately, because of how criteria API works, you have to explicitly list all aliases.
     * TODO: infer this from the criteria.
     */
    protected List<FilterablePropertyAlias> getFilterablePropertyAliases() {
        // FIXME: unfortunately, this requires a session...
        Criteria criteria = new TransactionTemplate( platformTransactionManager ).execute( ( ts ) -> getLoadValueObjectsCriteria( Filters.empty() ) );
        if ( criteria instanceof CriteriaImpl ) {
            //noinspection unchecked
            Iterator<CriteriaImpl.Subcriteria> it = ( ( CriteriaImpl ) criteria ).iterateSubcriteria();
            List<FilterablePropertyAlias> result = new ArrayList<>();
            while ( it.hasNext() ) {
                CriteriaImpl.Subcriteria sc = it.next();
                result.add( new FilterablePropertyAlias( sc.getPath(), sc.getAlias() ) );
            }
            return result;
        }
        return Collections.emptyList();
    }

    @Override
    protected FilterablePropertyMeta getFilterablePropertyMeta( String propertyName ) throws IllegalArgumentException {
        FilterablePropertyMeta meta = super.getFilterablePropertyMeta( propertyName );
        List<FilterablePropertyAlias> aliases = getFilterablePropertyAliases();
        // substitute longest path first
        aliases.sort( Comparator.comparing( a -> a.propertyName.length(), Comparator.reverseOrder() ) );
        for ( FilterablePropertyAlias alias : aliases ) {
            if ( propertyName.startsWith( alias.propertyName + "." ) ) {
                propertyName = propertyName.replaceFirst( "^" + Pattern.quote( alias.propertyName + "." ), "" );
                return new FilterablePropertyMeta( alias.alias, propertyName, meta.getPropertyType(), meta.getDescription() );
            }
        }
        return meta;
    }

    private static void addOrder( Criteria query, Sort sort ) {
        String propertyName = formPropertyName( sort.getObjectAlias(), sort.getPropertyName() );
        // handle .size ordering
        if ( propertyName.endsWith( ".size" ) ) {
            // FIXME: find a workaround for sorting by collection size (see https://github.com/PavlidisLab/Gemma/issues/520)
            throw new UnsupportedOperationException( "Ordering by collection size is not supported for the Criteria API." );
        }
        query.addOrder( sort.getDirection() == Sort.Direction.DESC ? Order.desc( propertyName ) : Order.asc( propertyName ) );
    }
}
