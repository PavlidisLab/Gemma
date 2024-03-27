package ubic.gemma.core.search;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.util.Version;
import org.hibernate.search.util.impl.PassThroughAnalyzer;
import ubic.gemma.model.common.search.SearchSettings;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utilities for parsing search queries.
 * @author poirigui
 */
@CommonsLog
public class QueryUtils {

    private static final Pattern LUCENE_RESERVED_CHARS = Pattern.compile( "[+\\-&|!(){}\\[\\]^\"~*?:\\\\]" );

    private static final QueryParser QUERY_PARSER = new QueryParser( Version.LUCENE_36, "", new PassThroughAnalyzer( Version.LUCENE_36 ) );

    public static Query parseSafely( SearchSettings settings, QueryParser queryParser ) throws SearchException {
        try {
            return queryParser.parse( settings.getQuery() );
        } catch ( ParseException e ) {
            String strippedQuery = LUCENE_RESERVED_CHARS.matcher( settings.getQuery() ).replaceAll( "\\\\$0" );
            log.warn( String.format( "Failed to parse '%s' after attempting to parse it without special characters '%s': %s",
                    settings.getQuery(), strippedQuery, e.getMessage() ) );
            try {
                return queryParser.parse( strippedQuery );
            } catch ( ParseException e2 ) {
                throw new LuceneSearchException( e );
            }
        }
    }

    /**
     * Extract terms, regardless of their logical organization.
     * <p>
     * Prohibited terms are excluded.
     */
    public static Set<String> extractTerms( SearchSettings settings ) throws SearchException {
        Set<String> terms = new HashSet<>();
        extractTerms( parseSafely( settings, QUERY_PARSER ), terms );
        return terms;
    }

    private static void extractTerms( Query query, Set<String> terms ) {
        if ( query instanceof BooleanQuery ) {
            for ( BooleanClause clause : ( ( BooleanQuery ) query ) ) {
                if ( !clause.isProhibited() ) {
                    extractTerms( clause.getQuery(), terms );
                }
            }
        } else if ( query instanceof TermQuery ) {
            terms.add( ( ( TermQuery ) query ).getTerm().text() );
        }
    }

    /**
     * Extract a DNF (Disjunctive Normal Form) from the query.
     */
    public static Set<Set<String>> extractDnf( SearchSettings settings ) throws SearchException {
        Query q = parseSafely( settings, QUERY_PARSER );
        Set<Set<String>> result = new HashSet<>();
        if ( q instanceof BooleanQuery ) {
            boolean isSimpleAndClause = true;
            for ( BooleanClause clause : ( ( BooleanQuery ) q ) ) {
                isSimpleAndClause &= clause.isRequired() && clause.getQuery() instanceof TermQuery;
                if ( clause.isRequired() || clause.isProhibited() ) {
                    continue; // AND, we ignore
                }
                if ( clause.getQuery() instanceof BooleanQuery ) {
                    Set<String> terms = new HashSet<>();
                    for ( BooleanClause subClause : ( ( BooleanQuery ) clause.getQuery() ) ) {
                        if ( !subClause.isRequired() || subClause.isProhibited() ) {
                            continue; // OR, we ignore
                        }
                        if ( subClause.getQuery() instanceof TermQuery ) {
                            terms.add( ( ( TermQuery ) subClause.getQuery() ).getTerm().text() );
                        }
                    }
                    if ( !terms.isEmpty() ) {
                        result.add( terms );
                    }
                } else if ( clause.getQuery() instanceof TermQuery ) {
                    result.add( Collections.singleton( ( ( TermQuery ) clause.getQuery() ).getTerm().text() ) );
                }
            }
            // check if all the clauses are required, in which case we can just create a nested clause
            if ( isSimpleAndClause ) {
                Set<String> terms = new HashSet<>();
                for ( BooleanClause clause : ( ( BooleanQuery ) q ) ) {
                    terms.add( ( ( TermQuery ) clause.getQuery() ).getTerm().text() );
                }
                if ( !terms.isEmpty() ) {
                    result.add( terms );
                }
            }
        } else if ( q instanceof TermQuery ) {
            result.add( Collections.singleton( ( ( TermQuery ) q ).getTerm().text() ) );
        }
        return result;
    }

    /**
     * Escape the query for a database match.
     * <p>
     * The resulting string is free from character that would usually be used for a free-text match.
     */
    public static String prepareDatabaseQuery( SearchSettings settings ) throws SearchException {
        return rewriteQuery( parseSafely( settings, QUERY_PARSER ), false );
    }

    /**
     * Obtain a query suitable for an inexact match (using a LIKE SQL expression).
     * <p>
     * This query supports wildcards ('*' and '?'), all other special characters are stripped.
     */
    public static String prepareDatabaseQueryForInexactMatch( SearchSettings settings ) throws SearchException {
        return rewriteQuery( parseSafely( settings, QUERY_PARSER ), true );
    }

    private static String rewriteQuery( Query query, boolean replaceWildcards ) {
        if ( query instanceof BooleanQuery ) {
            // pick the first, non-prohibited term
            for ( BooleanClause c : ( BooleanQuery ) query ) {
                if ( !c.isProhibited() ) {
                    return rewriteQuery( c.getQuery(), replaceWildcards );
                }
            }
        } else if ( query instanceof WildcardQuery ) {
            if ( replaceWildcards ) {
                return escapeLike( ( ( WildcardQuery ) query ).getTerm().text() )
                        .replace( '?', '_' )
                        .replace( '*', '%' );
            } else {
                return ( ( WildcardQuery ) query ).getTerm().text();
            }
        } else if ( query instanceof PrefixQuery ) {
            if ( replaceWildcards ) {
                return escapeLike( ( ( PrefixQuery ) query ).getPrefix().text() ) + "%";
            } else {
                return ( ( PrefixQuery ) query ).getPrefix().text();
            }
        } else if ( query instanceof TermQuery ) {
            if ( replaceWildcards ) {
                return escapeLike( ( ( TermQuery ) query ).getTerm().text() );
            } else {
                return ( ( TermQuery ) query ).getTerm().text();
            }
        }
        return "";
    }

    private static String escapeLike( String s ) {
        return s.replaceAll( "[%_\\\\]", "\\\\$0" );
    }

    /**
     * Check if the query is a wildcard query.
     */
    public static boolean isWildcard( SearchSettings settings ) {
        try {
            return isWildcard( QUERY_PARSER.parse( settings.getQuery() ) );
        } catch ( ParseException e ) {
            return false;
        }
    }

    private static boolean isWildcard( Query query ) {
        if ( query instanceof BooleanQuery ) {
            for ( BooleanClause clause : ( ( BooleanQuery ) query ) ) {
                // prohibited clauses are not used for database search
                if ( !clause.isProhibited() ) {
                    return isWildcard( clause.getQuery() );
                }
            }
        }
        return query instanceof WildcardQuery || query instanceof PrefixQuery;
    }
}
