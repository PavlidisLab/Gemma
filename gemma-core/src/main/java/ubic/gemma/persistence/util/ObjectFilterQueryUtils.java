package ubic.gemma.persistence.util;

import com.google.common.base.Strings;
import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.Query;

import java.util.*;

/**
 * Utilities for integrating {@link ObjectFilter} into {@link org.hibernate.Query}.
 */
public class ObjectFilterQueryUtils {

    /**
     * Form a property name with an alias.
     * <p>
     * The alias is omitted if set to null.
     *
     * @param alias        an alias, typically defined in {@link ObjectFilter}, or null
     * @param propertyName a property name within the defined entity
     * @return a well-formed property name suitable for a HQL query
     */
    public static String formPropertyName( String alias, String propertyName ) {
        return alias == null ? propertyName : alias + "." + propertyName;
    }

    public static String formPropertyName( ObjectFilter filter ) {
        return formPropertyName( filter.getObjectAlias(), filter.getPropertyName() );
    }

    /**
     * Forms an order by clause for a Hibernate query based on given arguments.
     *
     * @param orderByProperty the property the query should be ordered by.
     * @param orderDesc       whether the ordering should be descending or ascending.
     * @return an order by clause. Empty string if the orderByProperty argument is null or empty.
     */
    public static String formOrderByProperty( String orderBy, Sort.Direction direction ) {
        if ( Strings.isNullOrEmpty( orderBy ) )
            return "";
        StringBuilder ret = new StringBuilder();

        ret.append( " order by" );

        if ( orderBy.endsWith( ".size" ) ) {
            // This will crate an order by count clause, stripping the object alias and size suffix
            ret.append( " count( distinct " + orderBy.split( "\\." )[1] + ")" );
        } else {
            ret.append( " " );
            ret.append( orderBy );
        }

        if ( direction == null ) {
            // use default direction
        } else if ( direction.equals( Sort.Direction.ASC ) ) {
            ret.append( " asc" );
        } else if ( direction.equals( Sort.Direction.DESC ) ) {
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
        StringBuilder queryString = new StringBuilder();

        if ( filters == null || filters.isEmpty() )
            return queryString.toString();
        BindingParamNameGenerator generator = new BindingParamNameGenerator();
        StringBuilder conjunction = new StringBuilder();
        for ( ObjectFilter[] filterArray : filters ) {
            if ( filterArray == null || filterArray.length == 0 )
                continue;
            StringBuilder disjunction = new StringBuilder();
            boolean first = true;
            for ( ObjectFilter filter : filterArray ) {
                if ( filter == null || filter.getObjectAlias() == null )
                    continue;
                String paramName = generator.nextParam( filter );
                if ( !first )
                    disjunction.append( " or " );
                disjunction
                        .append( formPropertyName( filter.getObjectAlias(), filter.getPropertyName() ) ).append( " " );

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

        return queryString.append( conjunction ).toString();
    }

    /**
     * Adds all parameters contained in the filters argument to the Query by calling query.setParameter as needed.
     * <p>
     * Use this if you've appended {@link #formRestrictionClause(Filters)} to the query so that the provided
     * object filters will be bound.
     *
     * @param query   the query that needs parameters populated.
     * @param filters filters that provide the parameter values.
     */
    public static void addRestrictionParameters( Query query, Filters filters ) {
        if ( filters == null || filters.isEmpty() )
            return;

        BindingParamNameGenerator generator = new BindingParamNameGenerator();
        for ( ObjectFilter[] filterArray : filters ) {
            if ( filterArray == null || filterArray.length < 1 )
                continue;
            for ( ObjectFilter filter : filterArray ) {
                if ( filter == null || filter.getObjectAlias() == null )
                    continue;
                String paramName = generator.nextParam( filter );
                if ( filter.getOperator().equals( ObjectFilter.Operator.in ) ) {
                    query.setParameterList( paramName, ( Collection<?> ) filter.getRequiredValue() );
                } else if ( filter.getOperator().equals( ObjectFilter.Operator.like ) ) {
                    query.setParameter( paramName, "%" + filter.getRequiredValue() + "%" );
                } else {
                    query.setParameter( paramName, filter.getRequiredValue() );
                }
            }
        }
    }

    /**
     * Check if an alias is mentioned in a set of {@link ObjectFilter}.
     *
     * This should be used to eliminate parts of an HQL query that are not mentioned in the filters.
     *
     * @param filters
     * @param aliases
     * @return true if any provided alias is mentioned anywhere in the set of filters
     */
    public static boolean containsAnyAlias( Filters filters, String... aliases ) {
        if ( filters == null )
            return false;
        for ( ObjectFilter[] filter : filters ) {
            if ( filter == null )
                continue;
            for ( ObjectFilter f : filter ) {
                if ( f == null )
                    continue;
                if ( ArrayUtils.contains( aliases, f.getObjectAlias() ) ) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Generates unique parameter names for binding a {@link Query}.
     */
    private static class BindingParamNameGenerator {

        private final Map<String, Integer> counts;

        /**
         * Create a new parameter name generator.
         */
        public BindingParamNameGenerator() {
            this.counts = new HashMap<>();
        }

        /**
         * Create a parameter name generator with initial counts, possibly from a previous parameter generator
         * execution.
         * @param initialCounts
         */
        public BindingParamNameGenerator( Map<String, Integer> initialCounts ) {
            this.counts = new HashMap<>( initialCounts );
        }

        /**
         * Generate the next, uniquely named parameter.
         *
         * Note that the returned string does not contain the leading ":" character that denotes a parameter keyword in
         * the HQL query.
         */
        public String nextParam( String paramName ) {
            if ( counts.containsKey( paramName ) ) {
                counts.compute( paramName, ( k, v ) -> v + 1 );
            } else {
                counts.put( paramName, 1 );
            }
            return paramName + counts.get( paramName );
        }

        /**
         * Forms a parameter name out of the filter object.
         *
         * @param filter the filter to create the parameter name out of.
         * @return a name unique to the provided filter that can be used in an HQL query.
         */
        public String nextParam( ObjectFilter filter ) {
            return this.nextParam( filter.getObjectAlias().replaceAll( "\\.", "_" ) + filter.getPropertyName().replaceAll( "\\.", "_" ) );
        }

        public Map<String, Integer> getCounts() {
            return counts;
        }
    }

}
