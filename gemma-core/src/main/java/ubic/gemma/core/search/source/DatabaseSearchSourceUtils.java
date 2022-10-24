package ubic.gemma.core.search.source;

import ubic.gemma.model.common.search.SearchSettings;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DatabaseSearchSourceUtils {

    /**
     * List of reserved characters for Lucene.
     * <p>
     * See <a href="https://lucene.apache.org/core/3_6_2/queryparsersyntax.html">Apache Lucene - Query Parser Syntax</a>
     * for more details about special characters.
     */
    private static final String LUCENE_SPECIAL_CHARACTERS = Arrays.stream( "+ - && || ! ( ) { } [ ] ^ \" ~ * ? : \\".split( " " ) )
            .map( Pattern::quote )
            .collect( Collectors.joining( "|" ) );

    /**
     * Essentially the same as {@link #LUCENE_SPECIAL_CHARACTERS}, but excluding those that are supported.
     */
    private static final String LUCENE_SPECIAL_CHARACTERS_BUT_WILDCARDS = Arrays.stream( "+ - && || ! ( ) { } [ ] ^ \" ~ : \\".split( " " ) )
            .map( Pattern::quote )
            .collect( Collectors.joining( "|" ) );

    /**
     * Escape the query for a database match.
     * <p>
     * The resulting string is free from character that would usually be used for a free-text match.
     */
    public static String prepareDatabaseQuery( SearchSettings settings ) {
        return settings.getQuery()
                // also remove wildcards, those are for inexact matches only
                .replaceAll( LUCENE_SPECIAL_CHARACTERS, "" );
    }

    /**
     * Obtain a query suitable for an inexact match (using a LIKE SQL expression).
     * <p>
     * This query supports wildcards ('*' and '?'), all other special characters are stripped.
     */
    public static String prepareDatabaseQueryForInexactMatch( SearchSettings settings ) {
        return settings.getQuery()
                .replaceAll( LUCENE_SPECIAL_CHARACTERS_BUT_WILDCARDS, "" )
                .replaceAll( "%", "\\\\%" )
                .replaceAll( "_", "\\\\_" )
                .replace( SearchSettings.WILDCARD_CHAR, '%' )
                .replace( SearchSettings.SINGLE_WILDCARD_CHAR, '_' );
    }
}
