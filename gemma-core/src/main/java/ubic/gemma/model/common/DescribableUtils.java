package ubic.gemma.model.common;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

public class DescribableUtils {

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
     * Ensure that all the names in the collection of {@link Describable} objects are unique.
     * <p>
     * The check is case-insensitive.
     */
    public static void checkNamesAreUnique( Collection<? extends Describable> describables ) {
        Set<String> newCtaNames = new HashSet<>();
        for ( Describable d : describables ) {
            if ( d.getName() != null && !newCtaNames.add( d.getName() ) ) {
                throw new IllegalArgumentException( d + " has a non-unique name." );
            }
        }
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
}
