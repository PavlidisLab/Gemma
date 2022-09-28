package ubic.gemma.persistence.util;

import com.google.common.base.Strings;
import org.hibernate.Query;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utilities for integrating {@link ObjectFilter} into {@link org.hibernate.Query}.
 */
@ParametersAreNonnullByDefault
public class ObjectFilterQueryUtils {

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
        return alias == null ? propertyName : alias + "." + propertyName;
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
    public static String formOrderByClause( Sort sort ) {
        if ( Strings.isNullOrEmpty( sort.getPropertyName() ) )
            return "";
        StringBuilder ret = new StringBuilder();

        ret.append( " order by" );

        if ( sort.getPropertyName().endsWith( ".size" ) ) {
            // This will crate an order by count clause, stripping the object alias and size suffix
            ret.append( " count(distinct " ).append( formPropertyName( sort.getObjectAlias(), sort.getPropertyName() ) ).append( ')' );
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
    public static String formRestrictionClause( Filters filters ) {
        if ( filters.isEmpty() )
            return "";
        int i = 0;
        StringBuilder conjunction = new StringBuilder();
        for ( ObjectFilter[] filterArray : filters ) {
            if ( filterArray == null || filterArray.length == 0 )
                continue;
            StringBuilder disjunction = new StringBuilder();
            boolean first = true;
            for ( ObjectFilter filter : filterArray ) {
                if ( filter == null )
                    continue;
                if ( !first )
                    disjunction.append( " or " );
                disjunction
                        .append( formPropertyName( filter.getObjectAlias(), filter.getPropertyName() ) ).append( " " );

                String paramName = formParamName( filter.getObjectAlias(), filter.getPropertyName() ) + ( ++i );

                // we need to handle two special cases when comparing to NULL which cannot use == or != operators.
                if ( filter.getOperator().equals( ObjectFilter.Operator.eq ) && filter.getRequiredValue() == null ) {
                    disjunction.append( "is" );
                } else if ( filter.getOperator().equals( ObjectFilter.Operator.notEq ) && filter.getRequiredValue() == null ) {
                    disjunction.append( "is not" );
                } else {
                    disjunction.append( filter.getOperator().getSqlToken() );
                }

                disjunction.append( " " );
                if ( filter.getRequiredValue() instanceof Collection<?> ) {
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
     * Form both the filters and order by clauses, accounting for potential nulls.
     *
     * @param filters the filters to apply, or null to ignore
     * @param groupBy a property to group by, or null to ignore
     * @param sort the sort to apply, or null to ignore
     * @return a string with restriction, group by and order by clauses
     */
    public static String formRestrictionAndGroupByAndOrderByClauses( @Nullable Filters filters, @Nullable String groupBy, @Nullable Sort sort ) {
        String queryString = "";
        if ( filters != null ) {
            queryString += ObjectFilterQueryUtils.formRestrictionClause( filters );
        }
        if ( groupBy != null ) {
            queryString += " group by " + groupBy;
        }
        if ( sort != null ) {
            queryString += ObjectFilterQueryUtils.formOrderByClause( sort );
        }
        return queryString;
    }

    /**
     * Adds all parameters contained in the filters argument to the query by calling {@link Query#setParameter(String, Object)}
     * or {@link Query#setParameterList(String, Collection)} as needed.
     * <p>
     * Use this if you've appended {@link #formRestrictionClause(Filters)} to the query so that the provided
     * object filters will be bound.
     * <p>
     * If the {@link ObjectFilter#getRequiredValue()} is a {@link Collection}, it will be sorted and duplicates will be
     * excluded.
     *
     * @param query   the query that needs parameters populated.
     * @param filters filters that provide the parameter values.
     */
    public static void addRestrictionParameters( Query query, Filters filters ) {
        int i = 0;
        for ( ObjectFilter[] filterArray : filters ) {
            if ( filterArray == null )
                continue;
            for ( ObjectFilter filter : filterArray ) {
                if ( filter == null )
                    continue;
                String paramName = formParamName( filter.getObjectAlias(), filter.getPropertyName() ) + ( ++i );
                if ( filter.getOperator().equals( ObjectFilter.Operator.in ) ) {
                    // order is unimportant for this operation, so we can ensure that it is consistent and therefore cacheable
                    query.setParameterList( paramName, ( ( Collection<?> ) filter.getRequiredValue() ).stream().sorted().distinct().collect( Collectors.toList() ) );
                } else if ( filter.getOperator().equals( ObjectFilter.Operator.like ) ) {
                    query.setParameter( paramName, "%" + filter.getRequiredValue() + "%" );
                } else {
                    query.setParameter( paramName, filter.getRequiredValue() );
                }
            }
        }
    }
}
