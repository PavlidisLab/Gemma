package ubic.gemma.persistence.util;

import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nullable;

/**
 * Utilities for working with {@link Filters} and {@link ObjectFilter}.
 */
public class FiltersUtils {

    /**
     * Check if an alias is mentioned in a set of {@link ObjectFilter}.
     *
     * This should be used to eliminate parts of an HQL query that are not mentioned in the filters.
     *
     * @return true if any provided alias is mentioned anywhere in the set of filters
     */
    public static boolean containsAnyAlias( @Nullable Filters filters, String... aliases ) {
        if ( filters == null )
            return false;
        for ( ObjectFilter[] filter : filters ) {
            if ( filter == null )
                continue;
            for ( ObjectFilter f : filter ) {
                if ( f == null )
                    continue;
                if ( ArrayUtils.contains( aliases, f.getObjectAlias() ) ) {
                    return true;
                }
            }
        }
        return false;
    }
}
