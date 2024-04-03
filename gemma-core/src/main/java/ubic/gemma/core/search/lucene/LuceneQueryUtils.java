package ubic.gemma.core.search.lucene;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.util.Version;
import org.hibernate.search.util.impl.PassThroughAnalyzer;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.model.common.search.SearchSettings;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utilities for parsing search queries using Lucene.
 * @author poirigui
 */
@CommonsLog
public class LuceneQueryUtils {

    private static final Pattern LUCENE_RESERVED_CHARS = Pattern.compile( "[+\\-&|!(){}\\[\\]^\"~*?:\\\\]" );

    private static QueryParser createQueryParser() {
        return new QueryParser( Version.LUCENE_36, "", new PassThroughAnalyzer( Version.LUCENE_36 ) );
    }

    /**
     * Safely parse the given search settings into a Lucene query, falling back on a query with special characters
     * escaped if necessary.
     */
    public static Query parseSafely( SearchSettings settings, QueryParser queryParser ) throws SearchException {
        String query = settings.getQuery();
        try {
            return queryParser.parse( query );
        } catch ( ParseException e ) {
            String strippedQuery = LUCENE_RESERVED_CHARS.matcher( settings.getQuery() ).replaceAll( "\\\\$0" );
            log.debug( String.format( "Failed to parse '%s': %s.", query, ExceptionUtils.getRootCauseMessage( e ) ), e );
            try {
                return queryParser.parse( strippedQuery );
            } catch ( ParseException e2 ) {
                throw new LuceneParseSearchException(
                        strippedQuery,
                        ExceptionUtils.getRootCauseMessage( e2 ),
                        e2,
                        new LuceneParseSearchException( query, ExceptionUtils.getRootCauseMessage( e ), e ) );
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
        extractTerms( parseSafely( settings, createQueryParser() ), terms );
        return terms;
    }

    private static void extractTerms( Query query, Set<String> terms ) {
        if ( query instanceof BooleanQuery ) {
            for ( BooleanClause clause : ( ( BooleanQuery ) query ) ) {
                if ( !clause.isProhibited() ) {
                    extractTerms( clause.getQuery(), terms );
                }
            }
        } else if ( query instanceof TermQuery && isTermGlobal( ( ( TermQuery ) query ).getTerm() ) ) {
            terms.add( termToString( ( ( TermQuery ) query ).getTerm() ) );
        }
    }

    /**
     * Extract a DNF (Disjunctive Normal Form) from the query.
     * <p>
     * Clauses can be nested (i.e. {@code a OR (d OR (c AND (d AND e))}) as long as {@code OR} and {@code AND} are not
     * interleaved.
     * <p>
     * Prohibited clauses are ignored unless they break the DNF structure, in which case this will return an empty set.
     */
    public static Set<Set<String>> extractDnf( SearchSettings settings ) throws SearchException {
        Query q = parseSafely( settings, createQueryParser() );
        Set<Set<String>> result;
        if ( q instanceof BooleanQuery ) {
            Set<Set<String>> ds = new HashSet<>();
            if ( extractNestedDisjunctions( ( BooleanQuery ) q, ds ) ) {
                result = ds;
            } else {
                result = Collections.emptySet();
            }
        } else if ( q instanceof TermQuery && isTermGlobal( ( ( TermQuery ) q ).getTerm() ) ) {
            result = Collections.singleton( Collections.singleton( termToString( ( ( TermQuery ) q ).getTerm() ) ) );
        } else {
            result = Collections.emptySet();
        }
        return result;
    }

    private static boolean extractNestedDisjunctions( BooleanQuery query, Set<Set<String>> terms ) {
        if ( query.clauses().stream().anyMatch( BooleanClause::isRequired ) ) {
            Set<String> subClause = new HashSet<>();
            terms.add( subClause );
            return extractNestedConjunctions( query, subClause );
        }
        // at this point, all clauses are optional
        for ( BooleanClause clause : query.clauses() ) {
            if ( clause.isProhibited() ) {
                continue;
            }
            assert !clause.isRequired();
            if ( clause.getQuery() instanceof BooleanQuery ) {
                if ( !extractNestedDisjunctions( ( BooleanQuery ) clause.getQuery(), terms ) ) {
                    return false;
                }
            } else if ( clause.getQuery() instanceof TermQuery && isTermGlobal( ( ( TermQuery ) clause.getQuery() ).getTerm() ) ) {
                terms.add( Collections.singleton( termToString( ( ( TermQuery ) clause.getQuery() ).getTerm() ) ) );
            }
        }
        return true;
    }

    /**
     * Extract nested conjunctions from a query and populate their terms in the given set.
     *
     * @return true if all the clauses in the query are conjunctions
     */
    private static boolean extractNestedConjunctions( BooleanQuery query, Set<String> terms ) {
        if ( !query.clauses().stream().allMatch( c -> c.isRequired() || c.isProhibited() ) ) {
            // found a disjunction, this is not a valid nested conjunction
            return false;
        }
        // at this point, all the clauses are required
        for ( BooleanClause clause : query.clauses() ) {
            if ( clause.isProhibited() ) {
                continue;
            }
            if ( clause.getQuery() instanceof BooleanQuery ) {
                if ( !extractNestedConjunctions( ( BooleanQuery ) clause.getQuery(), terms ) ) {
                    return false;
                }
            } else if ( clause.getQuery() instanceof TermQuery && isTermGlobal( ( ( TermQuery ) clause.getQuery() ).getTerm() ) ) {
                terms.add( termToString( ( ( TermQuery ) clause.getQuery() ).getTerm() ) );
            }
        }
        return true;
    }

    /**
     * Escape the query for a database match.
     * @see #prepareDatabaseQuery(SearchSettings, boolean)
     */
    @Nullable
    public static String prepareDatabaseQuery( SearchSettings settings ) throws SearchException {
        return prepareDatabaseQuery( settings, false );
    }

    /**
     * Obtain a query suitable for a database match.
     * <p>
     * This method will return the first global term in the query that is not prohibited. If {@code allowWildcards} is
     * set to true, prefix and wildcard terms will be considered as well.
     * <p>
     * The resulting string is free from character that would usually be used for a free-text match unless
     * {@code allowWildcards} is set to true.
     * <p>
     * @param allowWildcards if true, wildcards are supported (i.e. '*' and '?') and translated to their corresponding
     *                       LIKE SQL syntax (i.e. '%' and '_'), all other special characters are escaped.
     * @return the first suitable term in the query, or null if none of them are applicable for a database query
     */
    @Nullable
    public static String prepareDatabaseQuery( SearchSettings settings, boolean allowWildcards ) throws SearchException {
        return prepareDatabaseQueryInternal( parseSafely( settings, createQueryParser() ), allowWildcards );
    }

    @Nullable
    private static String prepareDatabaseQueryInternal( Query query, boolean allowWildcards ) {
        if ( query instanceof BooleanQuery ) {
            // pick the first, non-prohibited term
            for ( BooleanClause c : ( BooleanQuery ) query ) {
                if ( !c.isProhibited() ) {
                    return prepareDatabaseQueryInternal( c.getQuery(), allowWildcards );
                }
            }
        } else if ( allowWildcards && query instanceof WildcardQuery && isTermGlobal( ( ( WildcardQuery ) query ).getTerm() ) ) {
            return escapeLike( termToString( ( ( WildcardQuery ) query ).getTerm() ) )
                    .replace( '?', '_' )
                    .replace( '*', '%' );
        } else if ( allowWildcards && query instanceof PrefixQuery && isTermGlobal( ( ( PrefixQuery ) query ).getPrefix() ) ) {
            return escapeLike( termToString( ( ( PrefixQuery ) query ).getPrefix() ) ) + "%";
        } else if ( query instanceof TermQuery && isTermGlobal( ( ( TermQuery ) query ).getTerm() ) ) {
            if ( allowWildcards ) {
                return escapeLike( termToString( ( ( TermQuery ) query ).getTerm() ) );
            } else {
                return termToString( ( ( TermQuery ) query ).getTerm() );
            }
        }
        return null;
    }

    @Nullable
    public static URI prepareTermUriQuery( SearchSettings settings ) throws SearchException {
        Query query = parseSafely( settings, createQueryParser() );
        if ( query instanceof TermQuery ) {
            Term term = ( ( TermQuery ) query ).getTerm();
            return tryParseUri( term );
        }
        return null;
    }

    /**
     * Check if a given term is global (i.e. not fielded).
     * <p>
     * This includes the corner case when a term is a URI and would be parsed as a fielded term.
     */
    private static boolean isTermGlobal( Term term ) {
        return term.field().isEmpty() || tryParseUri( term ) != null;
    }

    /**
     * Extract a suitable string from a term, detecting URIs that would be parsed as a fielded term.
     */
    private static String termToString( Term term ) {
        URI uri;
        if ( ( uri = tryParseUri( term ) ) != null ) {
            return uri.toString();
        } else {
            return term.text();
        }
    }

    @Nullable
    private static URI tryParseUri( Term term ) {
        if ( term.text().startsWith( "http://" ) || term.text().startsWith( "https://" ) ) {
            try {
                return new URI( term.text() );
            } catch ( URISyntaxException e ) {
                // ignore, it will be treated as a term term
            }
        } else if ( ( term.field().equals( "http" ) || term.field().equals( "https" ) ) && term.text().startsWith( "//" ) ) {
            try {
                return new URI( term.field() + ":" + term.text() );
            } catch ( URISyntaxException e ) {
                // ignore, it will be treated as a fielded term
            }
        }
        return null;
    }

    private static String escapeLike( String s ) {
        return s.replaceAll( "[%_\\\\]", "\\\\$0" );
    }

    /**
     * Check if the query is a wildcard query.
     */
    public static boolean isWildcard( SearchSettings settings ) {
        try {
            return isWildcard( createQueryParser().parse( settings.getQuery() ) );
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
