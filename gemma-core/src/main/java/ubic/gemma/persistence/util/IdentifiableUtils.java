package ubic.gemma.persistence.util;

import ubic.gemma.model.common.Identifiable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Utilities for {@link ubic.gemma.model.common.Identifiable}.
 * @author poirigui
 */
public class IdentifiableUtils {

    /**
     * Collect results into an identifiable set.
     * <p>
     * This uses {@link Identifiable#getId()} for comparing elements, making the collection safe for holding proxies
     * unlike a {@link java.util.HashSet} that relies on {@link Object#hashCode()}.
     * @see Collectors#toSet()
     */
    public static <T extends Identifiable> Collector<T, ?, Set<T>> toIdentifiableSet() {
        return Collectors.toCollection( () -> new TreeSet<>( Comparator.comparing( Identifiable::getId ) ) );
    }

    /**
     * Collect results into an identifiable map.
     * <p>
     * This uses {@link Identifiable#getId()} for comparing elements, making the collection safe for holding proxies
     * unlike a {@link java.util.HashMap} that relies on {@link Object#hashCode()}.
     * @see Collectors#toMap
     */
    public static <T, K extends Identifiable, U> Collector<T, ?, Map<K, U>> toIdentifiableMap( Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper ) {
        return Collectors.toMap( keyMapper, valueMapper, ( a, b ) -> b, () -> new TreeMap<>( Comparator.comparing( Identifiable::getId ) ) );
    }
}
