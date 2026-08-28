package ubic.gemma.persistence.util;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.query.Query;

import org.springframework.lang.Nullable;
import java.util.Collection;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static ubic.gemma.persistence.util.PropertyMappingUtils.formProperty;
import static ubic.gemma.persistence.util.QueryUtils.escapeLike;
import static ubic.gemma.persistence.util.QueryUtils.optimizeParameterList;

/**
 * Utilities for integrating {@link Filter} into {@link org.hibernate.query.Query}.
 */
public class FilterQueryUtils {

    /**
     * Escape character used for {@code like} patterns, paired with an explicit {@code escape} clause.
     * <p>
     * Deliberately not a backslash: whether a backslash escapes anything depends on the server's
     * {@code sql_mode}, and getting that wrong fails in the silent direction (matches nothing).
     */
    private static final char LIKE_ESCAPE_CHAR = '~';

    /**
     * Forms an order by clause for a Hibernate query based on given arguments.
     *
     * @param sort the property and direction the query should be ordered by.
     * @return an order by clause. Empty string if the orderByProperty argument is null or empty.
     */
    public static String formOrderByClause( @Nullable Sort sort ) {
        if ( sort == null || StringUtils.isEmpty( sort.getPropertyName() ) )
            return "";
        StringBuilder ret = new StringBuilder();

        ret.append( " order by " );

        for ( ; sort != null; sort = sort.getAndThen() ) {
            if ( sort.getPropertyName().endsWith( ".size" ) ) {
                // This will crate an order by count clause, stripping the object alias and size suffix
                ret.append( "size(" ).append( formProperty( sort ).replaceFirst( "\\.size$", "" ) ).append( ')' );
            } else {
                ret.append( formProperty( sort ) );
            }

            Sort.Direction direction = sort.getDirection();
            if ( direction == Sort.Direction.ASC ) {
                ret.append( " asc" );
            } else if ( direction == Sort.Direction.DESC ) {
                ret.append( " desc" );
            }
            // direction == null falls through to default direction

            switch ( sort.getNullMode() ) {
                case DEFAULT:
                    break;
                case FIRST:
                    ret.append( " nulls first" );
                    break;
                case LAST:
                    ret.append( " nulls last" );
                    break;
                default:
                    throw new UnsupportedOperationException( "Unsupported null mode " + sort.getNullMode() + "." );
            }

            if ( sort.getAndThen() != null ) {
                ret.append( ", " );
            }
        }

        return ret.toString();
    }

    /**
     * Creates a CNF restriction clause from the given Filters list. FIXME The problem with this: it assumes the join is already there.
     *
     * @param filters A list of filtering properties arrays.
     *                Elements in each array will be in a disjunction (OR) with each other.
     *                Arrays will then be in a conjunction (AND) with each other.
     *                I.e. The filter will be in a conjunctive normal form.
     *                <code>[0 OR 1 OR 2] AND [0 OR 1] AND [0 OR 1 OR 3]</code>
     * @return a string containing the clause, without the leading "WHERE" keyword.
     */
    public static String formRestrictionClause( @Nullable Filters filters ) {
        if ( filters == null || filters.isEmpty() )
            return "";
        int i = 0;
        StringBuilder conjunction = new StringBuilder();
        for ( List<Filter> clause : filters ) {
            if ( clause == null || clause.isEmpty() )
                continue;
            StringBuilder disjunction = new StringBuilder();
            boolean first = true;
            for ( Filter subClause : clause ) {
                if ( subClause == null )
                    continue;
                if ( !first )
                    disjunction.append( " or " );
                disjunction.append( formSubClause( subClause, ++i ) );
                first = false;
            }
            String disjunctionString = disjunction.toString();
            if ( !disjunctionString.isEmpty() ) {
                conjunction.append( " and (" ).append( disjunctionString ).append( ")" );
            }
        }

        return conjunction.toString();
    }

    static String formSubClause( Filter filter, int i ) {
        return formSubClause( filter, i, 0 );
    }

    static String formSubClause( Filter filter, int i, int k ) {
        StringBuilder disjunction = new StringBuilder();
        if ( filter.getPropertyName().endsWith( ".size" ) ) {
            disjunction.append( "size(" ).append( formProperty( filter ).replaceFirst( "\\.size$", "" ) ).append( ')' ).append( ' ' );
        } else {
            disjunction.append( formProperty( filter ) ).append( ' ' );
        }
        String paramName = formParamName( filter, i, k );

        // we need to handle two special cases when comparing to NULL which cannot use == or != operators.
        if ( filter.getOperator().equals( Filter.Operator.eq ) && filter.getRequiredValue() == null ) {
            disjunction.append( "is" );
        } else if ( filter.getOperator().equals( Filter.Operator.notEq ) && filter.getRequiredValue() == null ) {
            disjunction.append( "is not" );
        } else {
            String token;
            switch ( filter.getOperator() ) {
                case eq:
                    token = "=";
                    break;
                case notEq:
                    token = "!=";
                    break;
                case like:
                    token = "like";
                    break;
                case notLike:
                    token = "not like";
                    break;
                case lessThan:
                    token = "<";
                    break;
                case greaterThan:
                    token = ">";
                    break;
                case lessOrEq:
                    token = "<=";
                    break;
                case greaterOrEq:
                    token = ">=";
                    break;
                case in:
                case inSubquery:
                    token = "in";
                    break;
                case notIn:
                case notInSubquery:
                    token = "not in";
                    break;
                default:
                    throw new IllegalArgumentException( String.format( "Unsupported operator %s.", filter.getOperator() ) );
            }
            disjunction.append( token );
        }

        disjunction.append( ' ' );
        if ( filter.getRequiredValue() instanceof Subquery s ) {
            // check if the root alias is declared, otherwise use 'e' as default
            String rootAlias = s.getRootAlias();
            disjunction
                    .append( "(" );
            disjunction
                    .append( "select " ).append( rootAlias ).append( "." ).append( s.getPropertyName() )
                    .append( " from " )
                    .append( s.getEntityName() )
                    .append( " " )
                    .append( rootAlias );
            for ( Subquery.Alias a : s.getAliases() ) {
                if ( a.getPropertyName().isEmpty() ) {
                    continue;
                }
                disjunction.append( " join " );
                if ( a.getObjectAlias() == null ) {
                    disjunction.append( rootAlias ).append( "." );
                } else {
                    disjunction.append( a.getObjectAlias() ).append( "." );
                }
                disjunction.append( a.getPropertyName() ).append( " " ).append( a.getAlias() );
            }
            disjunction.append( " where " );
            // A subquery may carry several conjoined filters, all binding to the SAME element of the
            // relation ("this characteristic has value X and category Y"). Conjunct k > 0 takes a
            // `_k`-suffixed parameter name so k == 0 keeps the exact name it had when a subquery could
            // only hold one filter — addRestrictionParameters mirrors this, and the two must not drift.
            List<Filter> conjuncts = s.getFilters();
            for ( int ck = 0; ck < conjuncts.size(); ck++ ) {
                Filter conjunct = conjuncts.get( ck );
                if ( ck > 0 ) {
                    disjunction.append( " and " );
                }
                if ( conjunct.getObjectAlias() == null ) {
                    disjunction.append( rootAlias ).append( "." );
                }
                disjunction.append( formSubClause( conjunct, i, ck ) );
            }
            disjunction.append( ")" );
        } else if ( filter.getRequiredValue() instanceof Collection ) {
            disjunction
                    .append( "(" ).append( ":" ).append( paramName ).append( ")" );
        } else {
            disjunction
                    .append( ":" ).append( paramName );
            if ( filter.getOperator() == Filter.Operator.like || filter.getOperator() == Filter.Operator.notLike ) {
                // Explicit escape character, so the pattern does not depend on the backslash default
                // being honoured. Under MySQL's NO_BACKSLASH_ESCAPES mode it is not, and an escaped
                // wildcard silently matches nothing — which is what made every filter containing an
                // underscore (`name like 1007_s_at`, i.e. most Affymetrix probe names) come back
                // empty. Must stay in step with the escape character used in addRestrictionParameters.
                disjunction.append( " escape '" ).append( LIKE_ESCAPE_CHAR ).append( "'" );
            }
        }
        return disjunction.toString();
    }

    /**
     * Adds all parameters contained in the filters argument to the query by calling {@link Query#setParameter(String, Object)}
     * or {@link Query#setParameterList(String, Collection)} as needed.
     * <p>
     * Use this if you've appended {@link #formRestrictionClause(Filters)} to the query so that the provided filters
     * will be bound.
     * <p>
     * If the {@link Filter#getRequiredValue()} is a {@link Collection}, it will be sorted and duplicates will be
     * excluded.
     *
     * @param query   the query that needs parameters populated.
     * @param filters filters that provide the parameter values.
     */
    public static void addRestrictionParameters( Query query, @Nullable Filters filters ) {
        addRestrictionParameters( query, filters, 0 );
    }

    private static void addRestrictionParameters( Query query, @Nullable Filters filters, int i ) {
        if ( filters == null )
            return;
        for ( List<Filter> clause : filters ) {
            if ( clause == null )
                continue;
            for ( Filter subClause : clause ) {
                if ( subClause == null )
                    continue;
                ++i;
                if ( subClause.getOperator().equals( Filter.Operator.inSubquery ) || subClause.getOperator().equals( Filter.Operator.notInSubquery ) ) {
                    Subquery s = ( Subquery ) requireNonNull( subClause.getRequiredValue() );
                    // Mirrors the `_k` suffixing in formSubClause above; if these two disagree the
                    // query builds fine and then fails at bind time with a missing-parameter error.
                    List<Filter> conjuncts = s.getFilters();
                    for ( int k = 0; k < conjuncts.size(); k++ ) {
                        bindParameter( query, conjuncts.get( k ), formParamName( conjuncts.get( k ), i, k ), i );
                    }
                } else {
                    bindParameter( query, subClause, formParamName( subClause, i, 0 ), i );
                }
            }
        }
    }

    /**
     * Bind one filter's right-hand side under an already-resolved parameter name.
     * <p>
     * A nested subquery recurses rather than binding: its own conjuncts carry the parameters.
     */
    private static void bindParameter( Query query, Filter filter, String paramName, int i ) {
        if ( filter.getOperator().equals( Filter.Operator.inSubquery ) || filter.getOperator().equals( Filter.Operator.notInSubquery ) ) {
            Subquery s = ( Subquery ) requireNonNull( filter.getRequiredValue() );
            List<Filter> conjuncts = s.getFilters();
            for ( int k = 0; k < conjuncts.size(); k++ ) {
                bindParameter( query, conjuncts.get( k ), formParamName( conjuncts.get( k ), i, k ), i );
            }
        } else if ( filter.getOperator().equals( Filter.Operator.in ) || filter.getOperator().equals( Filter.Operator.notIn ) ) {
            if ( !( filter.getRequiredValue() instanceof Collection<?> coll ) ) {
                throw new IllegalArgumentException( "Required value must be a non-null collection for the 'in' operator." );
            }
            // order is unimportant for this operation, so we can ensure that it is consistent and therefore cacheable
            //noinspection rawtypes,unchecked
            query.setParameterList( paramName, optimizeParameterList( (Collection) coll ) );
        } else if ( filter.getOperator().equals( Filter.Operator.like ) || filter.getOperator().equals( Filter.Operator.notLike ) ) {
            query.setParameter( paramName, escapeLike( ( String ) requireNonNull( filter.getRequiredValue(), "Required value cannot be null for the 'like' operator." ), LIKE_ESCAPE_CHAR ) + "%" );
        } else {
            query.setParameter( paramName, filter.getRequiredValue() );
        }
    }

    private static String formParamName( PropertyMapping mapping, int i ) {
        return formParamName( mapping, i, 0 );
    }

    /**
     * @param k position of this filter within its subquery's conjunction; 0 (the only possibility
     *          before conjunctions existed) yields the historical name unchanged.
     */
    private static String formParamName( PropertyMapping mapping, int i, int k ) {
        return formProperty( mapping ).replaceAll( "\\W", "_" ) + i + ( k == 0 ? "" : "_" + k );
    }
}
