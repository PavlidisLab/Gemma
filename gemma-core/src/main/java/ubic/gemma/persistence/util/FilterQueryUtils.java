package ubic.gemma.persistence.util;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Utilities for integrating {@link Filter} into {@link org.hibernate.Query}.
 */
public class FilterQueryUtils {

    /**
     * Form a property name with an alias.
     * <p>
     * The alias is omitted if set to null.
     *
     * @param alias        an alias, or null
     * @param propertyName a property name within the defined entity
     * @return a well-formed property name suitable for an HQL query
     */
    public static String formPropertyName( @Nullable String alias, String propertyName ) {
        return alias == null ? propertyName : alias + "." + propertyName.replaceFirst( "\\.size$", "" );
    }

    /**
     * Form an SQL/HQL parameter name for binding a query.
     */
    public static String formParamName( @Nullable String objectAlias, String propertyName ) {
        if ( objectAlias != null ) {
            return objectAlias + "_" + propertyName.replaceAll( "\\.", "_" );
        } else {
            return propertyName.replaceAll( "\\.", "_" );
        }
    }

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

        ret.append( " order by" );

        if ( sort.getPropertyName().endsWith( ".size" ) ) {
            // This will crate an order by count clause, stripping the object alias and size suffix
            ret.append( " size(" ).append( formPropertyName( sort.getObjectAlias(), sort.getPropertyName() ) ).append( ')' );
        } else {
            ret.append( " " );
            ret.append( formPropertyName( sort.getObjectAlias(), sort.getPropertyName() ) );
        }

        //noinspection StatementWithEmptyBody
        if ( sort.getDirection() == null ) {
            // use default direction
        } else if ( sort.getDirection().equals( Sort.Direction.ASC ) ) {
            ret.append( " asc" );
        } else if ( sort.getDirection().equals( Sort.Direction.DESC ) ) {
            ret.append( " desc" );
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
        for ( Filter[] clause : filters ) {
            if ( clause == null || clause.length == 0 )
                continue;
            StringBuilder disjunction = new StringBuilder();
            boolean first = true;
            for ( Filter subClause : clause ) {
                if ( subClause == null )
                    continue;
                if ( !first )
                    disjunction.append( " or " );

                if ( subClause.getPropertyName().endsWith( ".size" ) ) {
                    disjunction.append( "size(" ).append( formPropertyName( subClause.getObjectAlias(), subClause.getPropertyName() ) ).append( ')' ).append( " " );
                } else {
                    disjunction
                            .append( formPropertyName( subClause.getObjectAlias(), subClause.getPropertyName() ) ).append( " " );
                }
                String paramName = formParamName( subClause.getObjectAlias(), subClause.getPropertyName() ) + ( ++i );

                // we need to handle two special cases when comparing to NULL which cannot use == or != operators.
                if ( subClause.getOperator().equals( Filter.Operator.eq ) && subClause.getRequiredValue() == null ) {
                    disjunction.append( "is" );
                } else if ( subClause.getOperator().equals( Filter.Operator.notEq ) && subClause.getRequiredValue() == null ) {
                    disjunction.append( "is not" );
                } else {
                    String token;
                    switch ( subClause.getOperator() ) {
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
                            token = "in";
                            break;
                        default:
                            throw new IllegalArgumentException( String.format( "Unsupported operator %s.", subClause.getOperator() ) );
                    }
                    disjunction.append( token );
                }

                disjunction.append( " " );
                if ( subClause.getRequiredValue() instanceof Collection<?> ) {
                    disjunction
                            .append( "(" ).append( ":" ).append( paramName ).append( ")" );
                } else {
                    disjunction
                            .append( ":" ).append( paramName );
                }
                first = false;
            }
            String disjunctionString = disjunction.toString();
            if ( !disjunctionString.isEmpty() ) {
                conjunction.append( " and (" ).append( disjunctionString ).append( ")" );
            }
        }

        return conjunction.toString();
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
        if ( filters == null )
            return;
        int i = 0;
        for ( Filter[] clause : filters ) {
            if ( clause == null )
                continue;
            for ( Filter subClause : clause ) {
                if ( subClause == null )
                    continue;
                String paramName = formParamName( subClause.getObjectAlias(), subClause.getPropertyName() ) + ( ++i );
                if ( subClause.getOperator().equals( Filter.Operator.in ) ) {
                    // order is unimportant for this operation, so we can ensure that it is consistent and therefore cacheable
                    query.setParameterList( paramName, Objects.requireNonNull( ( Collection<?> ) subClause.getRequiredValue(), "Required value cannot be null for the 'in' operator." )
                            .stream().sorted().distinct().collect( Collectors.toList() ) );
                } else if ( subClause.getOperator().equals( Filter.Operator.like ) ) {
                    query.setParameter( paramName, escapeLike( ( String ) Objects.requireNonNull( subClause.getRequiredValue(), "Required value cannot be null for the 'like' operator." ) ) + "%" );
                } else {
                    query.setParameter( paramName, subClause.getRequiredValue() );
                }
            }
        }
    }

    static String escapeLike( String s ) {
        return s.replace( "%", "\\%" )
                .replace( "_", "\\_" );
    }
}
