package ubic.gemma.persistence.util;

import org.hibernate.Hibernate;
import ubic.gemma.model.annotations.MayBeUninitialized;
import ubic.gemma.model.common.Identifiable;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Utilities for {@link ubic.gemma.model.common.Identifiable}.
 *
 * @author poirigui
 */
public class IdentifiableUtils {

    /**
     * Convert a collection of identifiable to their IDs.
     * <p>
     * This function cannot be used on non-persistent entities (i.e. those with null IDs).
     *
     * @param entities entities
     * @return returns a collection of IDs. Avoids using reflection by requiring that the given entities all
     * implement the Identifiable interface.
     */
    public static <T extends @MayBeUninitialized Identifiable> List<Long> getIds( Collection<T> entities ) {
        return entities.stream().map( Identifiable::getRequiredId ).collect( Collectors.toList() );
    }

    /**
     * Given a set of entities, create a map of their ids to the entities.
     * <p>
     * Note: If more than one entity share the same ID, there is no guarantee on which will be kept in the final
     * mapping. If the collection is ordered, the first encountered entity will be kept.
     * <p>
     * This function cannot be used on non-persistent entities (i.e. those with null IDs).
     *
     * @param entities where id is called "id"
     * @param <T>      the type
     * @return the created map
     */
    public static <T extends @MayBeUninitialized Identifiable> Map<Long, T> getIdMap( Collection<T> entities ) {
        Map<Long, T> result = new HashMap<>();
        for ( T entity : entities ) {
            result.putIfAbsent( entity.getRequiredId(), entity );
        }
        return result;
    }

    /**
     * Collect results into an identifiable set.
     * <p>
     * This uses {@link Identifiable#getId()} for comparing elements, making the collection safe for holding proxies
     * unlike a {@link java.util.HashSet} that relies on {@link Object#hashCode()}.
     * <p>
     * This collector cannot be used with non-persistent entities (i.e. those with null IDs).
     *
     * @see Collectors#toSet()
     */
    public static <T extends @MayBeUninitialized Identifiable> Collector<T, ?, Set<T>> toIdentifiableSet() {
        return Collectors.toCollection( () -> new TreeSet<>( Comparator.comparing( i -> requireNonNull( i.getId() ) ) ) );
    }

    /**
     * Collect results into an identifiable map.
     * <p>
     * This uses {@link Identifiable#getId()} for comparing elements, making the collection safe for holding proxies
     * unlike a {@link java.util.HashMap} that relies on {@link Object#hashCode()}.
     * <p>
     * This collector cannot be used with non-persistent entities (i.e. those with null IDs).
     *
     * @see Collectors#toMap
     */
    public static <T, K extends @MayBeUninitialized Identifiable, U> Collector<T, ?, Map<K, U>> toIdentifiableMap( Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper ) {
        return Collectors.toMap( keyMapper, valueMapper, ( a, b ) -> b, () -> new TreeMap<>( Comparator.comparing( i -> requireNonNull( i.getId() ) ) ) );
    }

    /**
     * Converts an identifiable to string, avoiding its initialization of it is a proxy.
     */
    public static <T extends @MayBeUninitialized Identifiable> String toString( T identifiable, Class<T> clazz ) {
        if ( Hibernate.isInitialized( identifiable ) ) {
            return Objects.toString( identifiable );
        } else {
            return clazz.getSimpleName() + ( identifiable.getId() != null ? " Id=" + identifiable.getId() : "" );
        }
    }

    /**
     * Hash identifiables in a proxy-safe way using their IDs.
     * <p>
     * Hashing an entity that does ont have an assigned ID is not allowed as its hash code would change once persisted.
     */
    public static int hash( @MayBeUninitialized Identifiable... identifiables ) {
        Object[] ids = new Long[identifiables.length];
        for ( int i = 0; i < identifiables.length; i++ ) {
            ids[i] = identifiables[i] != null ? requireNonNull( identifiables[i].getId(), "Cannot hash a transient entity, either persist it first or use a different collection type." ) : null;
        }
        return Objects.hash( ids );
    }

    /**
     * Compare two identifiables of the same type without risking initializing them.
     *
     * @return true if they have the same ID or are equal according to {@link Objects#equals(Object, Object)}.
     */
    public static <T extends Identifiable> boolean equals( @Nullable @MayBeUninitialized T a, @Nullable @MayBeUninitialized T b ) {
        if ( a == b ) {
            return true;
        } else if ( a == null ^ b == null ) {
            return false;
        } else if ( a.getId() != null || b.getId() != null ) {
            return Objects.equals( b.getId(), b.getId() );
        } else {
            // both IDs are null, objects can be compared directly
            return Objects.equals( a, b );
        }
    }
}
