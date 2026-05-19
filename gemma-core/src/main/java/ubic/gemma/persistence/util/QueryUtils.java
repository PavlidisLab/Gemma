package ubic.gemma.persistence.util;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.stream.Streams;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.util.Assert;
import ubic.gemma.core.util.ListUtils;
import ubic.gemma.model.common.Identifiable;

import org.springframework.lang.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utilities for Hibernate {@link Query}.
 * <p>
 * Pre-phase-2 this class also accepted legacy {@code org.hibernate.Criteria}
 * inputs and tinkered with the MySQL JDBC driver to enable cursor-based
 * streaming. Hibernate 6 removed the Criteria API and the Hibernate-internal
 * accessors that streaming hack relied on. Streaming now uses
 * {@link Query#scroll(ScrollMode)} when supported and an offset/limit
 * fallback otherwise. Cursor-fetch tuning can be re-added if needed.
 */
@CommonsLog
public class QueryUtils {

    /**
     * Largest parameter list size for which {@link #optimizeParameterList(Collection)} should be used.
     */
    public static final int MAX_PARAMETER_LIST_SIZE = 2048;

    public static <T> List<T> list( Query<?> query ) {
        //noinspection unchecked
        return ( List<T> ) query.list();
    }

    /**
     * Apply {@link Query#setMaxResults} only when {@code maxResults} is positive.
     * <p>
     * Hibernate 5+ rejects {@code setMaxResults(<0)}; many Gemma DAOs pass {@code -1} to mean "no limit".
     */
    public static Query<?> setMaxResultsIfPositive( Query<?> query, int maxResults ) {
        if ( maxResults > 0 ) {
            query.setMaxResults( maxResults );
        }
        return query;
    }

    public static <T> T uniqueResult( Query<?> query ) {
        //noinspection unchecked
        return ( T ) query.uniqueResult();
    }

    /**
     * Optimize a given parameter list by sorting, removing duplicates and padding to the next power of two.
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
     * Optimize a collection of {@link Identifiable} entities by ID.
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

    public static <S extends Comparable<S>, T> List<T> listByBatch( Query<?> query, String batchParam, Collection<S> list, int batchSize ) {
        return listByBatch( query, batchParam, list, batchSize, -1 );
    }

    public static <S extends Comparable<S>, T> List<T> listByBatch( Query<?> query, String batchParam, Collection<S> list, int batchSize, int maxResults ) {
        List<T> result = new ArrayList<>( list.size() );
        for ( List<S> batch : batchParameterList( list, batchSize ) ) {
            int remainingToFetch = calculateRemainingToFetch( result, maxResults );
            if ( remainingToFetch == 0 ) {
                break;
            }
            query.setParameterList( batchParam, batch );
            if ( remainingToFetch > 0 ) {
                query.setMaxResults( remainingToFetch );
            }
            //noinspection unchecked
            result.addAll( ( List<T> ) query.list() );
        }
        return result;
    }

    public static <S extends Identifiable, T> List<T> listByIdentifiableBatch( Query<?> query, String batchParam, Collection<S> list, int batchSize ) {
        return listByIdentifiableBatch( query, batchParam, list, batchSize, -1 );
    }

    public static <S extends Identifiable, T> List<T> listByIdentifiableBatch( Query<?> query, String batchParam, Collection<S> list, int batchSize, int maxResults ) {
        List<T> result = new ArrayList<>( list.size() );
        for ( List<S> batch : batchIdentifiableParameterList( list, batchSize ) ) {
            int remainingToFetch = calculateRemainingToFetch( result, maxResults );
            if ( remainingToFetch == 0 ) {
                break;
            }
            query.setParameterList( batchParam, batch );
            if ( remainingToFetch > 0 ) {
                query.setMaxResults( remainingToFetch );
            }
            //noinspection unchecked
            result.addAll( ( List<T> ) query.list() );
        }
        return result;
    }

    private static int calculateRemainingToFetch( List<?> result, int maxResults ) {
        if ( maxResults > 0 ) {
            return result.size() < maxResults ? maxResults - result.size() : 0;
        }
        return -1;
    }

    public static <S extends Comparable<S>, T> Stream<T> streamByBatch( Query<?> query, String batchParam, Collection<S> list, int batchSize ) {
        //noinspection unchecked
        return batchParameterList( list, batchSize ).stream()
                .map( batch -> ( List<T> ) query.setParameterList( batchParam, batch ).list() )
                .flatMap( List::stream );
    }

    public static <S extends Identifiable, T> Stream<T> streamByIdentifiableBatch( Query<?> query, String batchParam, Collection<S> list, int batchSize ) {
        //noinspection unchecked
        return batchIdentifiableParameterList( list, batchSize ).stream()
                .map( batch -> ( List<T> ) query.setParameterList( batchParam, batch ).list() )
                .flatMap( List::stream );
    }

    /**
     * Stream the result of a query with the given fetch size.
     * <p>
     * Uses {@link Query#scroll(ScrollMode)} when possible, otherwise falls back to offset/limit pagination.
     * The {@code useCursorFetchIfSupported} and {@code isQueryStateless} hints are accepted for source
     * compatibility but no longer dispatch on driver-specific tricks.
     */
    public static <T> Stream<T> stream( Query<?> query, Class<T> resultType, int fetchSize, boolean useCursorFetchIfSupported, boolean isQueryStateless ) {
        Assert.isTrue( fetchSize > 0, "Fetch size must be one or greater." );
        try {
            query.setFetchSize( fetchSize );
            ScrollableResults<?> sr = query.scroll( ScrollMode.FORWARD_ONLY );
            return Streams.<T>of( new ScrollableResultsIterator<>( sr, resultType.isArray() ) )
                    .onClose( sr::close );
        } catch ( Exception e ) {
            log.debug( "Falling back to offset/limit-based streaming: " + e.getMessage() );
            return Streams.of( new QueryOffsetLimitIterator<>( query, fetchSize ) );
        }
    }

    private static class QueryOffsetLimitIterator<T> implements Iterator<T> {

        private final Query<?> query;
        private final int fetchSize;
        private int offset;
        private List<T> results;

        public QueryOffsetLimitIterator( Query<?> query, int fetchSize ) {
            Assert.isTrue( fetchSize >= 1 , "expected true");
            this.query = query;
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
            if ( ( offset == 0 && results == null ) || ( offset > 0 && offset % fetchSize == 0 ) ) {
                //noinspection unchecked
                results = ( List<T> ) query
                        .setFirstResult( offset )
                        .setMaxResults( fetchSize )
                        .list();
            }
        }
    }

    private static class ScrollableResultsIterator<T> implements Iterator<T> {

        private final ScrollableResults<?> results;
        private final boolean isArray;
        private T _next;

        private ScrollableResultsIterator( ScrollableResults<?> results, boolean isArray ) {
            this.results = results;
            this.isArray = isArray;
        }

        @Override
        public boolean hasNext() {
            fetchNextIfNecessary();
            return _next != null;
        }

        @Override
        public T next() {
            fetchNextIfNecessary();
            if ( _next == null ) {
                throw new NoSuchElementException();
            }
            try {
                return _next;
            } finally {
                _next = null;
            }
        }

        private void fetchNextIfNecessary() {
            if ( _next == null && results.next() ) {
                //noinspection unchecked
                _next = ( T ) results.get();
            }
        }
    }

    /**
     * Safely create a {@link Stream} from either the current or a new {@link Session}.
     */
    @Nullable
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

    public static <S extends Comparable<S>> int executeUpdateByBatch( Query<?> query, String batchParam, Collection<S> list, int batchSize ) {
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
