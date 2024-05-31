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
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.FilterCriteriaUtils;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static ubic.gemma.persistence.util.PropertyMappingUtils.formProperty;

/**
 * Partial implementation of {@link FilteringVoEnabledDao} based on the Hibernate {@link Criteria} API.
 *
 * @author poirigui
 * @see FilterCriteriaUtils to obtain {@link org.hibernate.criterion.DetachedCriteria}
 * from a {@link Filters}.
 * @see ubic.gemma.persistence.util.AclCriteriaUtils for utilities to include ACL constraints on the VOs at the
 * database-level.
 */
public abstract class AbstractCriteriaFilteringVoEnabledDao<O extends Identifiable, VO extends IdentifiableValueObject<O>> extends AbstractFilteringVoEnabledDao<O, VO> {

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    /**
     * List of aliases used in the criteria query from {@link #getFilteringCriteria(Filters)}.
     * <p>
     * This is used to resolve properties to specific aliases.
     */
    private List<FilterablePropertyCriteriaAlias> filterablePropertyCriteriaAliases;

    protected AbstractCriteriaFilteringVoEnabledDao( Class<? extends O> elementClass, SessionFactory sessionFactory ) {
        // This is a good default objet alias for Hibernate Criteria since null is used to refer to the root entity.
        super( null, elementClass, sessionFactory );
    }

    @Override
    public void afterPropertiesSet() {
        this.filterablePropertyCriteriaAliases = getFilterablePropertyCriteriaAliases();
        super.afterPropertiesSet();
    }

    /**
     * Obtain a {@link Criteria} for loading VOs.
     *
     * @see FilterCriteriaUtils#formRestrictionClause(Filters) to obtain a {@link org.hibernate.criterion.DetachedCriteria}
     * from a set of filter clauses.
     */
    protected Criteria getFilteringCriteria( @Nullable Filters filters ) {
        return this.getSessionFactory().getCurrentSession()
                .createCriteria( elementClass )
                .add( FilterCriteriaUtils.formRestrictionClause( filters ) );
    }

    @Override
    public List<Long> loadIds( @Nullable Filters filters, @Nullable Sort sort ) {
        StopWatch stopWatch = StopWatch.createStarted();

        Criteria criteria = getFilteringCriteria( filters )
                .setProjection( Projections.distinct( Projections.id() ) );

        if ( sort != null ) {
            addOrder( criteria, sort );
        }

        //noinspection unchecked
        List<Long> result = criteria.list();

        if ( stopWatch.getTime( TimeUnit.MILLISECONDS ) > REPORT_SLOW_QUERY_AFTER_MS ) {
            log.warn( String.format( "Loading %d IDs for %s took %d ms.",
                    result.size(), elementClass.getName(),
                    stopWatch.getTime( TimeUnit.MILLISECONDS ) ) );
        }

        return result;
    }

    @Override
    public List<O> load( @Nullable Filters filters, @Nullable Sort sort ) {
        StopWatch stopWatch = StopWatch.createStarted();

        Criteria criteria = getFilteringCriteria( filters );

        if ( sort != null ) {
            addOrder( criteria, sort );
        }

        criteria.setResultTransformer( Criteria.DISTINCT_ROOT_ENTITY );

        //noinspection unchecked
        List<O> result = criteria.list();

        if ( stopWatch.getTime( TimeUnit.MILLISECONDS ) > REPORT_SLOW_QUERY_AFTER_MS ) {
            log.warn( String.format( "Loading %d entities for %s took %d ms.",
                    result.size(), elementClass.getName(),
                    stopWatch.getTime( TimeUnit.MILLISECONDS ) ) );
        }

        return result;
    }

    @Override
    public Slice<O> load( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
        StopWatch stopWatch = StopWatch.createStarted();
        StopWatch queryStopWatch = StopWatch.create();
        StopWatch countingStopWatch = StopWatch.create();

        Criteria criteria = getFilteringCriteria( filters );
        Criteria totalElementsQuery = getFilteringCriteria( filters );

        // setup sorting
        if ( sort != null ) {
            addOrder( criteria, sort );
        }

        // setup offset/limit
        if ( offset > 0 )
            criteria.setFirstResult( offset );
        if ( limit > 0 )
            criteria.setMaxResults( limit );

        queryStopWatch.start();
        //noinspection unchecked
        List<O> results = criteria
                .setResultTransformer( Criteria.DISTINCT_ROOT_ENTITY )
                .list();
        queryStopWatch.stop();

        countingStopWatch.start();
        Long totalElements;
        if ( limit > 0 && ( results.isEmpty() || results.size() == limit ) ) {
            totalElements = ( Long ) totalElementsQuery.setProjection( Projections.countDistinct( getIdentifierPropertyName() ) ).uniqueResult();
        } else {
            totalElements = offset + ( long ) results.size();
        }
        countingStopWatch.stop();

        if ( stopWatch.getTime( TimeUnit.MILLISECONDS ) > REPORT_SLOW_QUERY_AFTER_MS ) {
            log.warn( String.format( "Loading and counting %d entities for %s took %d ms (querying: %d, counting: %d).",
                    totalElements, elementClass.getName(),
                    stopWatch.getTime( TimeUnit.MILLISECONDS ), queryStopWatch.getTime( TimeUnit.MILLISECONDS ),
                    countingStopWatch.getTime( TimeUnit.MILLISECONDS ) ) );
        }

        return new Slice<>( results, sort, offset, limit, totalElements );
    }

    @Override
    public Slice<VO> loadValueObjects( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
        StopWatch stopWatch = StopWatch.createStarted();
        StopWatch countingStopWatch = StopWatch.create();
        StopWatch postProcessingStopWatch = StopWatch.create();

        Criteria query = getFilteringCriteria( filters );
        Criteria totalElementsQuery = getFilteringCriteria( filters );

        // setup sorting
        if ( sort != null ) {
            addOrder( query, sort );
        }

        // setup offset/limit
        if ( offset > 0 )
            query.setFirstResult( offset );
        if ( limit > 0 )
            query.setMaxResults( limit );

        // setup transformer
        query.setResultTransformer( Criteria.DISTINCT_ROOT_ENTITY );

        postProcessingStopWatch.start();
        //noinspection unchecked
        List<VO> results = doLoadValueObjects( query.list() );
        postProcessingStopWatch.stop();

        countingStopWatch.start();
        Long totalElements;
        if ( limit >= 0 && results.size() >= limit ) {
            totalElements = ( Long ) totalElementsQuery
                    .setProjection( Projections.countDistinct( getIdentifierPropertyName() ) )
                    .uniqueResult();
        } else {
            totalElements = ( long ) results.size();
        }
        countingStopWatch.stop();

        stopWatch.stop();

        if ( stopWatch.getTime( TimeUnit.MILLISECONDS ) > REPORT_SLOW_QUERY_AFTER_MS ) {
            log.warn( String.format( "Loading and counting %d VOs for %s took %d ms (querying: %d, counting: %d, post-processing: %d).",
                    totalElements, elementClass.getName(),
                    stopWatch.getTime( TimeUnit.MILLISECONDS ),
                    stopWatch.getTime( TimeUnit.MILLISECONDS ) - postProcessingStopWatch.getTime( TimeUnit.MILLISECONDS ) - countingStopWatch.getTime( TimeUnit.MILLISECONDS ),
                    countingStopWatch.getTime( TimeUnit.MILLISECONDS ), postProcessingStopWatch.getTime( TimeUnit.MILLISECONDS ) ) );
        }

        return new Slice<>( results, sort, offset, limit, totalElements );
    }

    @Override
    public List<VO> loadValueObjects( @Nullable Filters filters, @Nullable Sort sort ) {
        StopWatch stopWatch = StopWatch.createStarted();
        StopWatch postProcessingStopWatch = StopWatch.create();

        Criteria query = getFilteringCriteria( filters );

        if ( sort != null ) {
            addOrder( query, sort );
        }

        // setup transformer
        query.setResultTransformer( Criteria.DISTINCT_ROOT_ENTITY );

        postProcessingStopWatch.start();
        //noinspection unchecked
        List<VO> results = doLoadValueObjects( query.list() );
        postProcessingStopWatch.stop();

        stopWatch.stop();

        if ( stopWatch.getTime() > REPORT_SLOW_QUERY_AFTER_MS ) {
            log.warn( String.format( "Loading %d VOs for %s took %d ms (querying: %d ms, post-processing: %d ms).",
                    results.size(), elementClass.getName(), stopWatch.getTime( TimeUnit.MILLISECONDS ),
                    stopWatch.getTime( TimeUnit.MILLISECONDS ) - postProcessingStopWatch.getTime( TimeUnit.MILLISECONDS ),
                    postProcessingStopWatch.getTime( TimeUnit.MILLISECONDS ) ) );
        }

        return results;
    }

    @Override
    public long count( @Nullable Filters filters ) {
        StopWatch timer = StopWatch.createStarted();
        Long ret = ( Long ) getFilteringCriteria( filters )
                .setProjection( Projections.countDistinct( getIdentifierPropertyName() ) )
                .uniqueResult();
        timer.stop();
        if ( timer.getTime() > REPORT_SLOW_QUERY_AFTER_MS ) {
            log.warn( String.format( "Counting %d entities for %s took %d ms.",
                    ret, elementClass.getName(), timer.getTime( TimeUnit.MILLISECONDS ) ) );
        }
        return ret;
    }

    @Override
    protected FilterablePropertyMeta getFilterablePropertyMeta( String propertyName ) throws IllegalArgumentException {
        FilterablePropertyMeta meta = super.getFilterablePropertyMeta( propertyName );
        // the .size is not actually part of the property name, so don't account for it when substituting aliases
        String propNameWithoutSize = propertyName.replaceFirst( "\\.size$", "" );
        for ( FilterablePropertyCriteriaAlias alias : filterablePropertyCriteriaAliases ) {
            if ( propNameWithoutSize.startsWith( alias.propertyName + "." ) ) {
                propertyName = propertyName.replaceFirst( "^" + Pattern.quote( alias.propertyName + "." ), "" );
                return meta
                        .withObjectAlias( alias.alias )
                        .withPropertyName( propertyName );
            }
        }
        return meta;
    }

    @Value
    private static class FilterablePropertyCriteriaAlias {
        String propertyName;
        String alias;
    }

    private List<FilterablePropertyCriteriaAlias> getFilterablePropertyCriteriaAliases() {
        // FIXME: unfortunately, this requires a session...
        Criteria criteria = new TransactionTemplate( platformTransactionManager ).execute( ( ts ) -> getFilteringCriteria( Filters.empty() ) );
        if ( criteria instanceof CriteriaImpl ) {
            //noinspection unchecked
            Iterator<CriteriaImpl.Subcriteria> it = ( ( CriteriaImpl ) criteria ).iterateSubcriteria();
            List<FilterablePropertyCriteriaAlias> result = new ArrayList<>();
            while ( it.hasNext() ) {
                CriteriaImpl.Subcriteria sc = it.next();
                result.add( new FilterablePropertyCriteriaAlias( sc.getPath(), sc.getAlias() ) );
            }
            // substitute longest paths first
            result.sort( Comparator.comparing( a -> a.propertyName.length(), Comparator.reverseOrder() ) );
            return result;
        }
        return Collections.emptyList();
    }

    private static void addOrder( Criteria query, Sort sort ) {
        for ( ; sort != null; sort = sort.getAndThen() ) {
            String property = formProperty( sort );
            // handle .size ordering
            if ( property.endsWith( ".size" ) ) {
                // FIXME: find a workaround for sorting by collection size (see https://github.com/PavlidisLab/Gemma/issues/520)
                throw new UnsupportedOperationException( "Ordering by collection size is not supported for the Criteria API." );
            }
            query.addOrder( sort.getDirection() == Sort.Direction.DESC ? Order.desc( property ) : Order.asc( property ) );
        }
    }
}
