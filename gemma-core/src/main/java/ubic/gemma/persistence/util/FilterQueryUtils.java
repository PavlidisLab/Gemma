package ubic.gemma.persistence.util;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static ubic.gemma.persistence.util.PropertyMappingUtils.formProperty;
import static ubic.gemma.persistence.util.QueryUtils.optimizeParameterList;

/**
 * Utilities for integrating {@link Filter} into {@link org.hibernate.Query}.
 */
public class FilterQueryUtils {

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

            //noinspection StatementWithEmptyBody
            if ( sort.getDirection() == null ) {
                // use default direction
            } else if ( sort.getDirection().equals( Sort.Direction.ASC ) ) {
                ret.append( " asc" );
            } else if ( sort.getDirection().equals( Sort.Direction.DESC ) ) {
                ret.append( " desc" );
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
        StringBuilder disjunction = new StringBuilder();
        if ( filter.getPropertyName().endsWith( ".size" ) ) {
            disjunction.append( "size(" ).append( formProperty( filter ).replaceFirst( "\\.size$", "" ) ).append( ')' ).append( ' ' );
        } else {
            disjunction.append( formProperty( filter ) ).append( ' ' );
        }
        String paramName = formParamName( filter, i );

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
                default:
                    throw new IllegalArgumentException( String.format( "Unsupported operator %s.", filter.getOperator() ) );
            }
            disjunction.append( token );
        }

        disjunction.append( ' ' );
        if ( filter.getRequiredValue() instanceof Subquery ) {
            Subquery s = ( Subquery ) filter.getRequiredValue();
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
            if ( s.getFilter().getObjectAlias() == null ) {
                disjunction.append( rootAlias ).append( "." );
            }
            disjunction.append( formSubClause( s.getFilter(), i ) );
            disjunction.append( ")" );
        } else if ( filter.getRequiredValue() instanceof Collection ) {
            disjunction
                    .append( "(" ).append( ":" ).append( paramName ).append( ")" );
        } else {
            disjunction
                    .append( ":" ).append( paramName );
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
                String paramName = formParamName( subClause, ++i );
                if ( subClause.getOperator().equals( Filter.Operator.inSubquery ) ) {
                    Subquery s = ( Subquery ) requireNonNull( subClause.getRequiredValue() );
                    addRestrictionParameters( query, Filters.by( s.getFilter() ), i - 1 );
                } else if ( subClause.getOperator().equals( Filter.Operator.in ) ) {
                    if ( !( subClause.getRequiredValue() instanceof Collection ) ) {
                        throw new IllegalArgumentException( "Required value must be a non-null collection for the 'in' operator." );
                    }
                    // order is unimportant for this operation, so we can ensure that it is consistent and therefore cacheable
                    //noinspection rawtypes,unchecked
                    query.setParameterList( paramName, optimizeParameterList( ( Collection ) subClause.getRequiredValue() ) );
                } else if ( subClause.getOperator().equals( Filter.Operator.like ) ) {
                    query.setParameter( paramName, escapeLike( ( String ) requireNonNull( subClause.getRequiredValue(), "Required value cannot be null for the 'like' operator." ) ) + "%" );
                } else {
                    query.setParameter( paramName, subClause.getRequiredValue() );
                }
            }
        }
    }

    private static String formParamName( PropertyMapping mapping, int i ) {
        return formProperty( mapping ).replaceAll( "\\W", "_" ) + i;
    }

    private static String escapeLike( String s ) {
        return s.replace( "%", "\\%" )
                .replace( "_", "\\_" );
    }
}
