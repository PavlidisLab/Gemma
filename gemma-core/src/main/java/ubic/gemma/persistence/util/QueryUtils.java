package ubic.gemma.persistence.util;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.util.Assert;
import ubic.gemma.core.util.ListUtils;
import ubic.gemma.model.common.Identifiable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utilities for {@link org.hibernate.Query}.
 * @author poirigui
 */
@CommonsLog
public class QueryUtils {

    /**
     * Largest parameter list size for which {@link #optimizeParameterList(Collection)} should be used. Past this size,
     * no padding will be performed.
     */
    private static final int MAX_PARAMETER_LIST_SIZE = 2048;

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
                    sortedList.size() ) );
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
                .distinct()
                .collect( Collectors.toList() );
        if ( sortedList.size() > MAX_PARAMETER_LIST_SIZE ) {
            log.warn( String.format( "Optimizing a large parameter list of size %d may have a negative impact on performance, use batchIdentifiableParameterList() instead.",
                    sortedList.size() ) );
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
    public static <T extends Comparable<T>> Iterable<Collection<T>> batchParameterList( Collection<T> list, int batchSize ) {
        Assert.isTrue( batchSize == -1 || batchSize > 0, "Batch size must be strictly positive or equal to -1." );
        if ( list.isEmpty() ) {
            return Collections.emptyList();
        }
        List<T> sortedList = list.stream()
                .sorted( Comparator.nullsLast( Comparator.naturalOrder() ) )
                .distinct()
                .collect( Collectors.toList() );
        return batch( sortedList, batchSize );
    }

    public static <T extends Identifiable> Iterable<Collection<T>> batchIdentifiableParameterList( Collection<T> list, int batchSize ) {
        Assert.isTrue( batchSize == -1 || batchSize > 0, "Batch size must be strictly positive or equal to -1." );
        if ( list.isEmpty() ) {
            return Collections.emptyList();
        }
        List<T> sortedList = list.stream()
                .sorted( Comparator.comparing( Identifiable::getId, Comparator.nullsLast( Comparator.naturalOrder() ) ) )
                .distinct()
                .collect( Collectors.toList() );
        return batch( sortedList, batchSize );
    }

    private static <T> Iterable<Collection<T>> batch( List<T> list, int batchSize ) {
        if ( batchSize == -1 ) {
            return Collections.singletonList( list );
        }
        int numberOfBatches = ( list.size() / batchSize ) + ( list.size() % batchSize > 0 ? 1 : 0 );
        int size = numberOfBatches * batchSize;
        List<T> paddedList = ( List<T> ) ListUtils.pad( list, list.get( list.size() - 1 ), size );
        return () -> new Iterator<Collection<T>>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return i < numberOfBatches;
            }

            @Override
            public List<T> next() {
                try {
                    return paddedList.subList( i * batchSize, ( i + 1 ) * batchSize );
                } finally {
                    i += 1;
                }
            }
        };
    }
}
