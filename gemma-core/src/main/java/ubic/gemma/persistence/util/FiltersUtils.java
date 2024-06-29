package ubic.gemma.persistence.util;

import org.apache.commons.lang3.ArrayUtils;
import ubic.gemma.core.lang.Nullable;

import java.util.List;

/**
 * Utilities for working with {@link Filters} and {@link Filter}.
 */
public class FiltersUtils {

    /**
     * Check if an alias is mentioned in a set of {@link Filter}.
     * <p>
     * This should be used to eliminate parts of an HQL query that are not mentioned in the filters.
     *
     * @return true if any provided alias is mentioned anywhere in the set of filters
     */
    public static boolean containsAnyAlias( @Nullable Filters filters, @Nullable Sort sort, String... aliases ) {
        if ( sort != null && ArrayUtils.contains( aliases, sort.getObjectAlias() ) ) {
            return true;
        }
        if ( filters == null )
            return false;
        for ( List<Filter> clause : filters ) {
            if ( clause == null )
                continue;
            for ( Filter subClause : clause ) {
                if ( subClause == null )
                    continue;
                if ( ArrayUtils.contains( aliases, subClause.getObjectAlias() ) ) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Unnest a filter from a subquery.
     */
    public static Filter unnestSubquery( Filter f ) {
        while ( f.getRequiredValue() instanceof Subquery ) {
            f = ( ( Subquery ) f.getRequiredValue() ).getFilter();
        }
        return f;
    }
}
