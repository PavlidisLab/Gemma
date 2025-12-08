package ubic.gemma.model.common;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Utilities to manipulate {@link Describable} and collections thereof.
 *
 * @author poirigui
 */
@CommonsLog
public class DescribableUtils {

    /**
     * Comparator that sorts {@link Describable} objects by name, case-insensitively, with nulls last.
     */
    public static final Comparator<String> NAME_COMPARATOR = Comparator.nullsLast( String.CASE_INSENSITIVE_ORDER );

    /**
     * Check if two {@link Describable} objects are considered equals by their names.
     */
    public static boolean equalsByName( Describable a, Describable b ) {
        return a.getName() != null && b.getName() != null && a.getName().equalsIgnoreCase( b.getName() );
    }

    /**
     * Create a set of names from a collection of {@link Describable} objects.
     * <p>
     * The resulting set is case-insensitive.
     */
    public static Set<String> getNames( Collection<? extends Describable> describables ) {
        return describables.stream()
                .map( Describable::getName )
                .filter( Objects::nonNull )
                .collect( Collectors.toCollection( () -> new TreeSet<>( String.CASE_INSENSITIVE_ORDER ) ) );
    }

    /**
     * Generate the next available name by appending a number to the given string.
     * <p>
     * The format is "name", "name (2)", "name (3)", etc.
     * <p>
     * The name comparison is case-insensitive.
     */
    public static String getNextAvailableName( Collection<? extends Describable> describables, String name ) {
        Set<String> existingNames = getNames( describables );
        if ( !existingNames.contains( name ) ) {
            return name;
        }
        for ( int i = 2; ; i++ ) {
            String candidate = String.format( "%s (%d)", StringUtils.stripEnd( name, " " ), i );
            if ( !existingNames.contains( candidate ) ) {
                return candidate;
            }
        }
    }

    /**
     * Add all the describables to a given collection.
     * <p>
     * Elements with the same name that are already present in the collection will be ignored.
     * <p>
     * This is as close as it gets to {@link Collection#addAll(Collection)}.
     */
    public static <T extends Describable, S extends T> Collection<S> addAllByName( Collection<T> describables, Collection<S> toAdd ) {
        return addAllByName( describables, toAdd, false, true );
    }

    /**
     * Add all the describables to a given collection.
     * <p>
     * This uses {@link #addByName(Collection, Describable)} and {@link #removeByName(Collection, Describable)} to add
     * and remove elements from the collection.
     *
     * @see #addAllByName(Collection, Collection, BiFunction, BiFunction, boolean, boolean)
     */
    public static <T extends Describable, S extends T> Collection<S> addAllByName( Collection<T> describables, Collection<S> toAdd, boolean replaceExisting, boolean ignoreExisting ) {
        // when replacing existing, no need to track existing names
        Set<String> existingNames = replaceExisting ? Collections.emptySet() : getNames( describables );
        return addAllByName( describables, toAdd,
                // this is faster as it caches the existing names
                ( col, elem ) -> addByName( col, elem, existingNames, ignoreExisting ),
                DescribableUtils::removeByName, replaceExisting, ignoreExisting );
    }

    /**
     * Add all the describables to a given collection.
     *
     * @param describables    collection to which describables will be added. Note that the collection is only altered
     *                        through the supplied {@code addFunc} and {@code removeFunc}.
     * @param toAdd           describables to add to the collection. The function returns the element that was actually
     *                        added
     * @param addFunc         function to add an element to the collection.
     * @param removeFunc      function to remove an element from the collection by name. This is never called with a
     *                        describable that has a {@code null} name.
     * @param replaceExisting if true, existing describables with the same name will be replaced.
     * @param ignoreExisting  if true, existing describables with the same name will be ignored.
     * @param <T>             type of describable.
     * @return a collection of elements that were actually added to the collection.
     * @throws IllegalArgumentException if the added describable have duplicated names or if a describable with the same
     *                                  name exists and neither {@code replaceExisting} nor {@code ignoreExisting} is
     *                                  true. Note that {@code null} names are not considered for uniqueness.
     */
    public static <T extends Describable, S extends T> Collection<S> addAllByName(
            Collection<T> describables,
            Collection<S> toAdd,
            BiFunction<Collection<T>, S, S> addFunc,
            BiFunction<Collection<T>, T, Boolean> removeFunc,
            boolean replaceExisting, boolean ignoreExisting ) {
        if ( toAdd.isEmpty() ) {
            return Collections.emptyList();
        }
        String what = toAdd.iterator().next().getClass().getSimpleName();
        checkNamesAreUnique( toAdd );
        Set<String> existingNames = getNames( describables );
        // use a list to preserve order and also not assume uniqueness, etc. of the toAdd collection
        Collection<S> added = new ArrayList<>( toAdd.size() );
        for ( S d : toAdd ) {
            if ( d.getName() != null && existingNames.contains( d.getName() ) ) {
                if ( replaceExisting ) {
                    log.info( "Replacing existing a " + what + " with name " + d.getName() + "." );
                    if ( !removeFunc.apply( describables, d ) ) {
                        throw new IllegalStateException( "Failed to remove " + d + " from collection." );
                    }
                } else if ( ignoreExisting ) {
                    log.warn( "Collection already contains " + what + " with name " + d.getName() + ", ignoring. Specify replaceExisting to replace it." );
                    continue;
                } else {
                    throw new IllegalArgumentException( "Collection already contains a " + what + " with name " + d.getName() + ". Specify ignoreExisting to ignore it." );
                }
            }
            S e;
            if ( ( e = addFunc.apply( describables, d ) ) != null ) {
                added.add( e );
            } else {
                throw new IllegalStateException( "Failed to add " + d + " to collection." );
            }
        }
        return added;
    }

    /**
     * Add a {@link Describable} to a collection.
     * <p>
     * Elements with the same name that are already present in the collection will be ignored.
     * <p>
     * This is as close as it gets to {@link Collection#add(Object)}.
     *
     * @return the added element, or null if it was ignored.
     */
    @Nullable
    public static <T extends Describable, S extends T> S addByName( Collection<T> describables, S element ) {
        return addByName( describables, element, getNames( describables ), true );
    }

    @Nullable
    private static <T extends Describable, S extends T> S addByName( Collection<T> describables, S element, Set<String> existingNames, boolean ignoreExisting ) {
        if ( element.getName() != null && existingNames.contains( element.getName() ) ) {
            if ( ignoreExisting ) {
                log.warn( " Collection already has an element with name " + element.getName() + ", ignoring." );
                return null;
            } else {
                throw new IllegalArgumentException( "Collection already has an element with name " + element.getName() + "." );
            }
        }
        if ( describables.add( element ) ) {
            return element;
        } else {
            throw new IllegalStateException( "Failed to add " + element + " to collection." );
        }
    }

    /**
     * Remove all {@link Describable} objects from the collection that have the same name as any of those in the
     * toRemove collection.
     * <p>
     * This has an O(n+m) complexity, unlike repeatedly calling {@link #removeByName(Collection, Describable)} which is
     * O(n*m).
     */
    public static <T extends Describable, S extends T> boolean removeAllByName( Collection<T> describables, Collection<S> toRemove ) {
        Set<String> namesToRemove = getNames( toRemove );
        return describables.removeIf( c -> namesToRemove.contains( c.getName() ) );
    }

    /**
     * Remove one (or more if any duplicates) {@link Describable} from the collection by name.
     * <p>
     * This is as close as it gets to {@link Collection#remove(Object)}.
     */
    public static <T extends Describable, S extends T> boolean removeByName( Collection<T> describables, S element ) {
        return removeByName( describables, element.getName() );
    }

    /**
     * Remove one (or more if any duplicates) {@link Describable} from the collection by name.
     * <p>
     * The name comparison is case-insensitive.
     */
    public static boolean removeByName( Collection<? extends Describable> describables, String name ) {
        Assert.notNull( name, "Name to remove cannot be null" );
        return describables.removeIf( c -> name.equalsIgnoreCase( c.getName() ) );
    }

    /**
     * Ensure that all the names in the collection of {@link Describable} objects are unique.
     * <p>
     * The check is case-insensitive.
     */
    private static void checkNamesAreUnique( Collection<? extends Describable> describables ) {
        Set<String> newCtaNames = new HashSet<>( describables.size() );
        for ( Describable d : describables ) {
            if ( d.getName() != null && !newCtaNames.add( d.getName() ) ) {
                throw new IllegalArgumentException( d + " has a non-unique name." );
            }
        }
    }
}
