package ubic.gemma.core.search.lucene;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import ubic.gemma.core.ontology.OntologyUtils;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.persistence.util.QueryUtils;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Utilities for parsing search queries using Lucene.
 * <p>
 * Restored for HS-7 search restoration Step 3 (see SEARCH_RECCE.md). This is a
 * subset of the pre-strip {@code LuceneQueryUtils}: we keep {@link #parseSafely},
 * {@link #escape}, {@link #extractTerms}, {@link #prepareDatabaseQuery}, and
 * {@link #isWildcard} — the surface that {@code DatabaseSearchSource} and
 * {@code HibernateSearchSource} actually call. DNF extraction (used by ontology
 * search) is deferred along with the ontology source.
 *
 * @author poirigui
 */
@CommonsLog
public class LuceneQueryUtils {

    private static final Pattern LUCENE_RESERVED_CHARS = Pattern.compile( "[+\\-&|!(){}\\[\\]^\"~*?:\\\\]" );

    private static QueryParser createQueryParser() {
        // KeywordAnalyzer emits the input as a single token — equivalent to HS 5's
        // PassThroughAnalyzer.INSTANCE but freshly instantiated, so we avoid the
        // shared-state shutdown issues that bit the pre-strip code.
        return new QueryParser( "", new KeywordAnalyzer() );
    }

    /**
     * Safely parse the given search settings into a Lucene query, falling back on a query with special characters
     * escaped if necessary.
     */
    public static Query parseSafely( SearchSettings settings, QueryParser queryParser, @Nullable Consumer<Throwable> issueReporter ) throws SearchException {
        return parseSafely( settings.getQuery(), queryParser, issueReporter );
    }

    /**
     * Safely parse the given search query into a Lucene query, falling back on a query with special characters
     * escaped if necessary.
     *
     * @param report a consumer for potential {@link ParseException} when attempting to parse the query, ignored if null
     */
    public static Query parseSafely( String query, QueryParser queryParser, @Nullable Consumer<Throwable> report ) throws SearchException {
        try {
            return queryParser.parse( query );
        } catch ( ParseException e ) {
            String strippedQuery = escape( query );
            String m = String.format( "Failed to parse '%s': %s, it will be reattempted stripped from Lucene special characters as '%s'.",
                    query, ExceptionUtils.getRootCauseMessage( e ), strippedQuery );
            if ( report != null ) {
                log.debug( m, e );
                report.accept( e );
            } else {
                log.warn( m, e );
            }
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
     * Escape all reserved Lucene characters in the given query.
     */
    public static String escape( String query ) {
        return LUCENE_RESERVED_CHARS.matcher( query ).replaceAll( "\\\\$0" );
    }

    /**
     * Extract terms, regardless of their logical organization. Prohibited terms are excluded.
     */
    public static Set<String> extractTerms( SearchSettings settings, @Nullable Consumer<Throwable> issueReporter ) throws SearchException {
        Set<String> terms = new LinkedHashSet<>();
        extractTerms( parseSafely( settings, createQueryParser(), issueReporter ), terms );
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

    public static Set<Set<String>> extractTermsDnf( SearchSettings settings, @Nullable Consumer<Throwable> issueReporter ) throws SearchException {
        return extractTermsDnf( settings, false, issueReporter );
    }

    /**
     * Extract a DNF (Disjunctive Normal Form) from the terms of a query.
     * <p>
     * Clauses can be nested (i.e. {@code a OR (d OR (c AND (d AND e))}) as long as {@code OR} and {@code AND} are not
     * interleaved. Prohibited clauses are ignored unless they break the DNF structure, in which case this returns an
     * empty set.
     *
     * @param allowWildcards allow {@link PrefixQuery} and {@link WildcardQuery} clauses
     */
    public static Set<Set<String>> extractTermsDnf( SearchSettings settings, boolean allowWildcards, @Nullable Consumer<Throwable> issueReporter ) throws SearchException {
        Query q = parseSafely( settings, createQueryParser(), issueReporter );
        if ( q instanceof BooleanQuery ) {
            Set<Set<String>> ds = new LinkedHashSet<>();
            if ( extractNestedDisjunctions( ( BooleanQuery ) q, ds, allowWildcards ) ) {
                return ds;
            }
            return Collections.emptySet();
        } else if ( allowWildcards && q instanceof PrefixQuery && isTermGlobal( ( ( PrefixQuery ) q ).getPrefix() ) ) {
            return Collections.singleton( Collections.singleton( termToString( ( ( PrefixQuery ) q ).getPrefix() ) + "*" ) );
        } else if ( allowWildcards && q instanceof WildcardQuery && isTermGlobal( ( ( WildcardQuery ) q ).getTerm() ) ) {
            return Collections.singleton( Collections.singleton( termToString( ( ( WildcardQuery ) q ).getTerm() ) ) );
        } else if ( q instanceof TermQuery && isTermGlobal( ( ( TermQuery ) q ).getTerm() ) ) {
            return Collections.singleton( Collections.singleton( termToString( ( ( TermQuery ) q ).getTerm() ) ) );
        } else {
            return Collections.emptySet();
        }
    }

    private static boolean extractNestedDisjunctions( BooleanQuery query, Set<Set<String>> terms, boolean allowWildcards ) {
        if ( query.clauses().stream().anyMatch( BooleanClause::isRequired ) ) {
            Set<String> subClause = new LinkedHashSet<>();
            terms.add( subClause );
            return extractNestedConjunctions( query, subClause );
        }
        // at this point, all clauses are optional
        for ( BooleanClause clause : query.clauses() ) {
            if ( clause.isProhibited() ) {
                continue;
            }
            Query q = clause.getQuery();
            if ( q instanceof BooleanQuery ) {
                if ( !extractNestedDisjunctions( ( BooleanQuery ) q, terms, allowWildcards ) ) {
                    return false;
                }
            } else if ( allowWildcards && q instanceof PrefixQuery && isTermGlobal( ( ( PrefixQuery ) q ).getPrefix() ) ) {
                terms.add( Collections.singleton( termToString( ( ( PrefixQuery ) q ).getPrefix() ) + "*" ) );
            } else if ( allowWildcards && q instanceof WildcardQuery && isTermGlobal( ( ( WildcardQuery ) q ).getTerm() ) ) {
                terms.add( Collections.singleton( termToString( ( ( WildcardQuery ) q ).getTerm() ) ) );
            } else if ( q instanceof TermQuery && isTermGlobal( ( ( TermQuery ) q ).getTerm() ) ) {
                terms.add( Collections.singleton( termToString( ( ( TermQuery ) q ).getTerm() ) ) );
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
            return false;
        }
        for ( BooleanClause clause : query.clauses() ) {
            if ( clause.isProhibited() ) {
                continue;
            }
            Query q = clause.getQuery();
            if ( q instanceof BooleanQuery ) {
                if ( !extractNestedConjunctions( ( BooleanQuery ) q, terms ) ) {
                    return false;
                }
            } else if ( q instanceof TermQuery && isTermGlobal( ( ( TermQuery ) q ).getTerm() ) ) {
                terms.add( termToString( ( ( TermQuery ) q ).getTerm() ) );
            }
        }
        return true;
    }

    /**
     * @see #prepareDatabaseQuery(SearchSettings, boolean, Consumer)
     */
    @Nullable
    public static String prepareDatabaseQuery( SearchSettings settings, @Nullable Consumer<Throwable> issueReporter ) throws SearchException {
        return prepareDatabaseQuery( settings, false, issueReporter );
    }

    /**
     * Obtain a query suitable for a database match.
     * <p>
     * This method returns the first global term in the query that is not prohibited. If {@code allowWildcards} is set
     * to true, prefix and wildcard terms will be considered as well and translated to SQL LIKE syntax.
     */
    @Nullable
    public static String prepareDatabaseQuery( SearchSettings settings, boolean allowWildcards, @Nullable Consumer<Throwable> issueReporter ) throws SearchException {
        return prepareDatabaseQueryInternal( parseSafely( settings, createQueryParser(), issueReporter ), allowWildcards );
    }

    @Nullable
    public static String prepareDatabaseQuery( String query, boolean allowWildcards ) throws SearchException {
        return prepareDatabaseQueryInternal( parseSafely( query, createQueryParser(), null ), allowWildcards );
    }

    @Nullable
    private static String prepareDatabaseQueryInternal( Query query, boolean allowWildcards ) {
        if ( query instanceof BooleanQuery ) {
            for ( BooleanClause c : ( BooleanQuery ) query ) {
                if ( !c.isProhibited() ) {
                    return prepareDatabaseQueryInternal( c.getQuery(), allowWildcards );
                }
            }
        } else if ( allowWildcards && query instanceof WildcardQuery && isTermGlobal( ( ( WildcardQuery ) query ).getTerm() ) ) {
            String s = termToString( ( ( WildcardQuery ) query ).getTerm() );
            return QueryUtils.escapeLike( s )
                    .replace( '?', '_' )
                    .replace( '*', '%' );
        } else if ( allowWildcards && query instanceof PrefixQuery && isTermGlobal( ( ( PrefixQuery ) query ).getPrefix() ) ) {
            String s = termToString( ( ( PrefixQuery ) query ).getPrefix() );
            return QueryUtils.escapeLike( s ) + "%";
        } else if ( query instanceof TermQuery && isTermGlobal( ( ( TermQuery ) query ).getTerm() ) ) {
            if ( allowWildcards ) {
                String s = termToString( ( ( TermQuery ) query ).getTerm() );
                return QueryUtils.escapeLike( s );
            } else {
                return termToString( ( ( TermQuery ) query ).getTerm() );
            }
        }
        return null;
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
                if ( !clause.isProhibited() ) {
                    return isWildcard( clause.getQuery() );
                }
            }
        }
        return query instanceof WildcardQuery || query instanceof PrefixQuery;
    }

    /**
     * Quote the given Lucene query to be used for an exact match.
     */
    public static String quote( String query ) {
        query = query.replaceAll( "\"", "\\\\\"" );
        if ( query.contains( " " ) ) {
            query = "\"" + query + "\"";
        }
        return query;
    }

    @Nullable
    public static URI prepareTermUriQuery( SearchSettings settings, @Nullable Consumer<Throwable> issueReporter ) throws SearchException {
        Query query = parseSafely( settings, createQueryParser(), issueReporter );
        if ( query instanceof TermQuery ) {
            return tryParseUri( ( ( TermQuery ) query ).getTerm() );
        }
        return null;
    }

    /**
     * Check if a given term is global (not field-qualified). Includes URI and ontology-term corner cases.
     */
    private static boolean isTermGlobal( Term term ) {
        return term.field().isEmpty() || isOntologyTerm( term ) || tryParseUri( term ) != null;
    }

    /**
     * Extract a suitable string from a term, detecting URIs that would be parsed as a fielded term.
     */
    private static String termToString( Term term ) {
        URI uri;
        if ( isOntologyTerm( term ) ) {
            return term.field() + ":" + term.text();
        } else if ( ( uri = tryParseUri( term ) ) != null ) {
            return uri.toString();
        } else {
            return term.text();
        }
    }

    private static boolean isOntologyTerm( Term term ) {
        return OntologyUtils.isTermId( term.field() + ":" + term.text(), true );
    }

    @Nullable
    private static URI tryParseUri( Term term ) {
        if ( term.text().startsWith( "http://" ) || term.text().startsWith( "https://" ) ) {
            try {
                return new URI( term.text() );
            } catch ( URISyntaxException e ) {
                // ignore — treated as a plain term
            }
        } else if ( ( term.field().equals( "http" ) || term.field().equals( "https" ) ) && term.text().startsWith( "//" ) ) {
            try {
                return new URI( term.field() + ":" + term.text() );
            } catch ( URISyntaxException e ) {
                // ignore — treated as a fielded term
            }
        }
        return null;
    }
}
