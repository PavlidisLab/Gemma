package ubic.gemma.persistence.util;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.stream.Streams;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.util.Assert;
import ubic.gemma.core.util.ListUtils;
import ubic.gemma.model.common.Identifiable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
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
     * Default fetch size to use when streaming results with {@link #stream(Query, int)}
     */
    public static final int DEFAULT_FETCH_SIZE = 30;

    /**
     * @see Query#list()
     */
    public static <T> List<T> list( Query query ) {
        //noinspection unchecked
        return ( List<T> ) query.list();
    }

    /**
     * @see Query#uniqueResult()
     */
    public static <T> T uniqueResult( Query query ) {
        //noinspection unchecked
        return ( T ) query.uniqueResult();
    }

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
     * Stream the result of a query with the given fetch size.
     * <p>
     * This uses offset/limit under the hood because MySQL JDBC does not support scrolling with {@link Query#scroll()}.
     */
    public static <T> Stream<T> stream( Query query, int fetchSize ) {
        return Streams.of( new QueryOrCriteriaIterator<>( query, fetchSize ) );
    }

    public static <T> Stream<T> stream( Query query ) {
        return stream( query, DEFAULT_FETCH_SIZE );
    }

    public static <T> Stream<T> stream( Criteria criteria, int fetchSize ) {
        return Streams.of( new QueryOrCriteriaIterator<>( criteria, fetchSize ) );
    }

    public static <T> Stream<T> stream( Criteria criteria ) {
        return stream( criteria, DEFAULT_FETCH_SIZE );
    }

    private static class QueryOrCriteriaIterator<T> implements Iterator<T> {

        private final Object queryOrCriteria;
        private final int fetchSize;

        private int offset;
        private List<T> results;

        public QueryOrCriteriaIterator( Object queryOrCriteria, int fetchSize ) {
            Assert.isTrue( queryOrCriteria instanceof Query || queryOrCriteria instanceof Criteria );
            Assert.isTrue( fetchSize >= 1 );
            this.queryOrCriteria = queryOrCriteria;
            this.fetchSize = fetchSize;
        }

        @Override
        public boolean hasNext() {
            fetchResultsIfNecessary();
            return ( offset % fetchSize ) < results.size();
        }

        @Override
        public T next() {
            fetchResultsIfNecessary();
            try {
                return results.get( offset % fetchSize );
            } catch ( IndexOutOfBoundsException e ) {
                throw new NoSuchElementException();
            } finally {
                offset++;
            }
        }

        private void fetchResultsIfNecessary() {
            // either at the first record, or at the end of the current batch
            if ( ( offset == 0 && results == null ) || ( offset > 0 && offset % fetchSize == 0 ) ) {
                if ( queryOrCriteria instanceof Query ) {
                    //noinspection unchecked
                    results = ( ( Query ) queryOrCriteria )
                            .setFirstResult( offset )
                            .setMaxResults( fetchSize )
                            .list();
                } else {
                    //noinspection unchecked
                    results = ( ( Criteria ) queryOrCriteria )
                            .setFirstResult( offset )
                            .setMaxResults( fetchSize )
                            .list();
                }
            }
        }
    }

    /**
     * Safely create a {@link Stream} from either the current or a new {@link Session}.
     * @param streamFactory a function that produces the stream from a given {@link Session}. It may return null, in
     *                      which case the session will be closed immediately
     */
    public static <T> Stream<T> createStream( SessionFactory sessionFactory, Function<Session, Stream<T>> streamFactory, boolean createNewSession ) {
        Session session;
        if ( createNewSession ) {
            session = sessionFactory.openSession();
            try {
                Stream<T> stream = streamFactory.apply( session );
                if ( stream != null ) {
                    return stream.onClose( session::close );
                } else {
                    session.close();
                    return null;
                }
            } catch ( Exception e ) {
                session.close();
                throw e;
            }
        } else {
            return streamFactory.apply( sessionFactory.getCurrentSession() );
        }
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
