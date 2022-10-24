package ubic.gemma.core.search;

import ubic.gemma.model.common.search.SearchSettings;

public class SearchSettingsUtils {

    /**
     * Escape the query for a database match.
     * <p>
     * The resulting string is free from character that would usually be used for a free-text match such as quotes ("),
     * single quotes ("'"), etc.
     */
    public static String escapeQuery( SearchSettings settings ) {
        return settings.getQuery()
                // also remove wildcards, those are for inexact matches only
                .replaceAll( "[\"'*]", "" )
                .replaceAll( "%", "\\\\%" )
                .replaceAll( "_", "\\\\_" );
    }

    /**
     * Obtain a query suitable for an inexact match (using a LIKE SQL expression).
     */
    public static String escapeQueryForInexactMatch( SearchSettings settings ) {
        return settings.getQuery()
                .replaceAll( "[\"']", "" )
                .replaceAll( "%", "\\\\%" )
                .replaceAll( "_", "\\\\_" );
    }
}
