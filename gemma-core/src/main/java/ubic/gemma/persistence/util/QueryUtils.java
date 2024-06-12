package ubic.gemma.persistence.util;

import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.Query;
import org.springframework.util.Assert;
import ubic.gemma.core.util.ListUtils;
import ubic.gemma.model.common.Identifiable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utilities for {@link org.hibernate.Query}.
 * @author poirigui
 */
@CommonsLog
public class QueryUtils {

    /**
     * Largest parameter list size for which {@link #optimizeParameterList(Collection)} should be used. Past this size,
     * no padding will be performed and a warning will be emitted.
     */
    public static final int MAX_PARAMETER_LIST_SIZE = 2048;

    /**
     * Optimize a given parameter list by sorting, removing duplicates and padding to the next power of two.
     * <p>
     * This is a temporary solution until we update to Hibernate 5.2.18 which introduced {@code hibernate.query.in_clause_parameter_padding}.
     * <a href="https://thorben-janssen.com/parameter-padding/">Read more about this topic</a>.
     */
    public static <T extends Comparable<T>> Collection<T> optimizeParameterList( Collection<T> list ) {
        if ( list.size() < 2 ) {
            return list;
        }
        List<T> sortedList = list.stream()
                .sorted( Comparator.nullsLast( Comparator.naturalOrder() ) )
                .distinct()
                .collect( Collectors.toList() );
        if ( sortedList.size() > MAX_PARAMETER_LIST_SIZE ) {
            log.warn( String.format( "Optimizing a large parameter list of size %d may have a negative impact on performance, use batchParameterList() instead.",
                    sortedList.size() ), new Throwable() );
            return list;
        }
        return ListUtils.padToNextPowerOfTwo( sortedList, sortedList.get( sortedList.size() - 1 ) );
    }

    /**
     * Optimize a collection of {@link Identifiable} entities.
     * @see #optimizeParameterList(Collection)
     */
    public static <T extends Identifiable> Collection<T> optimizeIdentifiableParameterList( Collection<T> list ) {
        if ( list.size() < 2 ) {
            return list;
        }
        List<T> sortedList = list.stream()
                .sorted( Comparator.comparing( Identifiable::getId, Comparator.nullsLast( Comparator.naturalOrder() ) ) )
                .filter( distinctById() )
                .collect( Collectors.toList() );
        if ( sortedList.size() > MAX_PARAMETER_LIST_SIZE ) {
            log.warn( String.format( "Optimizing a large parameter list of size %d may have a negative impact on performance, use batchIdentifiableParameterList() instead.",
                    sortedList.size() ), new Throwable() );
            return list;
        }
        return ListUtils.padToNextPowerOfTwo( sortedList, sortedList.get( sortedList.size() - 1 ) );
    }

    /**
     * Partition a parameter list into a collection of batches of a given size.
     * <p>
     * It is recommended to use a power of two in case the same query is also prepared via
     * {@link #optimizeParameterList(Collection)}. This will make it so that the execution plan can be reused.
     */
    public static <T extends Comparable<T>> List<List<T>> batchParameterList( Collection<T> list, int batchSize ) {
        Assert.isTrue( batchSize == -1 || batchSize > 0, "Batch size must be strictly positive or equal to -1." );
        Assert.isTrue( batchSize <= MAX_PARAMETER_LIST_SIZE, "The batch size must not exceed " + MAX_PARAMETER_LIST_SIZE + "." );
        if ( list.isEmpty() ) {
            return Collections.emptyList();
        }
        List<T> sortedList = list.stream()
                .sorted( Comparator.nullsLast( Comparator.naturalOrder() ) )
                .distinct()
                .collect( Collectors.toList() );
        return ListUtils.batch( sortedList, batchSize );
    }

    public static <T extends Identifiable> List<List<T>> batchIdentifiableParameterList( Collection<T> list, int batchSize ) {
        Assert.isTrue( batchSize == -1 || batchSize > 0, "Batch size must be strictly positive or equal to -1." );
        Assert.isTrue( batchSize <= MAX_PARAMETER_LIST_SIZE, "The batch size must not exceed " + MAX_PARAMETER_LIST_SIZE + "." );
        if ( list.isEmpty() ) {
            return Collections.emptyList();
        }
        List<T> sortedList = list.stream()
                .sorted( Comparator.comparing( Identifiable::getId, Comparator.nullsLast( Comparator.naturalOrder() ) ) )
                .filter( distinctById() )
                .collect( Collectors.toList() );
        return ListUtils.batch( sortedList, batchSize );
    }

    /**
     * @see #listByBatch(Query, String, Collection, int, int)
     */
    public static <S extends Comparable<S>, T> List<T> listByBatch( Query query, String batchParam, Collection<S> list, int batchSize ) {
        return listByBatch( query, batchParam, list, batchSize, -1 );
    }

    /**
     * List the results of a query by a fixed batch size.
     * @param query      the query
     * @param batchParam a parameter of the query for batching
     * @param list       a collection of values for the batch parameters to retrieve
     * @param batchSize  the number of elements to fetch in each batch
     * @param maxResults maximum number of results to return, or -1 to ignore
     */
    public static <S extends Comparable<S>, T> List<T> listByBatch( Query query, String batchParam, Collection<S> list, int batchSize, int maxResults ) {
        List<T> result = new ArrayList<>( list.size() );
        for ( List<S> batch : batchParameterList( list, batchSize ) ) {
            int remainingToFetch = calculateRemainingToFetch( result, maxResults );
            if ( remainingToFetch == 0 ) {
                break;
            }
            query.setParameterList( batchParam, batch );
            query.setMaxResults( remainingToFetch );
            //noinspection unchecked
            result.addAll( query.list() );
        }
        return result;
    }

    public static <S extends
            Identifiable, T> List<T> listByIdentifiableBatch( Query query, String batchParam, Collection<S> list,
            int batchSize ) {
        return listByIdentifiableBatch( query, batchParam, list, batchSize, -1 );
    }

    public static <S extends Identifiable, T> List<T> listByIdentifiableBatch( Query query, String batchParam, Collection<S> list, int batchSize, int maxResults ) {
        List<T> result = new ArrayList<>( list.size() );
        for ( List<S> batch : batchIdentifiableParameterList( list, batchSize ) ) {
            int remainingToFetch = calculateRemainingToFetch( result, maxResults );
            if ( remainingToFetch == 0 ) {
                break;
            }
            query.setParameterList( batchParam, batch );
            query.setMaxResults( remainingToFetch );
            //noinspection unchecked
            result.addAll( query.list() );
        }
        return result;
    }

    private static int calculateRemainingToFetch( List<?> result, int maxResults ) {
        if ( maxResults > 0 ) {
            if ( result.size() < maxResults ) {
                return maxResults - result.size();
            } else {
                return 0;
            }
        } else {
            return -1;
        }
    }

    /**
     * @see #streamByBatch(Query, String, Collection, int)
     */
    public static <S extends Comparable<S>, T> Stream<T> streamByBatch( Query query, String batchParam, Collection<S> list, int batchSize, Class<T> clazz ) {
        return streamByBatch( query, batchParam, list, batchSize );
    }

    /**
     * Stream the results of a query by a fixed batch size.
     * @see #listByBatch(Query, String, Collection, int)
     */
    public static <S extends Comparable<S>, T> Stream<T> streamByBatch( Query query, String batchParam, Collection<S> list, int batchSize ) {
        //noinspection unchecked
        return batchParameterList( list, batchSize ).stream()
                .map( batch -> ( List<T> ) query.setParameterList( batchParam, batch ).list() )
                .flatMap( List::stream );
    }

    /**
     * Execute an update query by a fixed batch size.
     * @see Query#executeUpdate()
     * @return the sum of all performed update executions
     */
    public static <S extends Comparable<S>> int executeUpdateByBatch( Query query, String batchParam, Collection<S> list, int batchSize ) {
        int updated = 0;
        for ( List<S> batch : batchParameterList( list, batchSize ) ) {
            updated += query.setParameterList( batchParam, batch ).executeUpdate();
        }
        return updated;
    }

    public static String escapeLike( String s ) {
        return s.replaceAll( "[%_\\\\]", "\\\\$0" );
    }

    private static <T extends Identifiable> Predicate<T> distinctById() {
        Set<Long> seenIds = ConcurrentHashMap.newKeySet();
        AtomicBoolean seenNullId = new AtomicBoolean( false );
        return i -> i.getId() == null ? seenNullId.compareAndSet( false, true ) : seenIds.add( i.getId() );
    }
}
