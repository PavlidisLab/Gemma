package ubic.gemma.persistence.util;

import com.google.common.base.Strings;
import gemma.gsec.util.SecurityUtil;
import org.hibernate.Query;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Utilities for integrating {@link ObjectFilter} into {@link org.hibernate.Query}.
 */
public class ObjectFilterQueryUtils {

    /**
     * Forms an order by clause for a Hibernate query based on given arguments.
     *
     * @param  orderByProperty the property the query should be ordered by.
     * @param  orderDesc       whether the ordering should be descending or ascending.
     * @return an order by clause. Empty string if the orderByProperty argument is null or empty.
     */
    public static String formOrderByProperty( String orderByProperty, boolean orderDesc ) {
        if ( Strings.isNullOrEmpty( orderByProperty ) )
            return "";
        StringBuilder ret = new StringBuilder();

        ret.append( "order by" );

        if ( orderByProperty.endsWith( ".size" ) ) {
            // This will crate an order by count clause, stripping the object alias and size suffix
            ret.append( " count( distinct " + orderByProperty.split( "\\." )[1] + ")" );
        } else {
            ret.append( " " );
            ret.append( orderByProperty );
        }


        if ( orderDesc ) {
            ret.append( " desc" );
        }

        return ret.toString();
    }

    /**
     * Creates a CNF restriction clause from the given Filters list. FIXME The problem with this: it assumes the join is already there.
     *
     * @param  filters A list of filtering properties arrays.
     *                 Elements in each array will be in a disjunction (OR) with each other.
     *                 Arrays will then be in a conjunction (AND) with each other.
     *                 I.e. The filter will be in a conjunctive normal form.
     *                 <code>[0 OR 1 OR 2] AND [0 OR 1] AND [0 OR 1 OR 3]</code>
     * @param  addAcl  whether the acl restriction clause should also be added.
     * @return a string containing the clause, without the leading "WHERE" keyword.
     */
    public static String formRestrictionClause( List<ObjectFilter[]> filters, boolean addAcl ) {
        StringBuilder queryString = new StringBuilder();

        if ( addAcl ) {
            queryString.append( AclQueryUtils.formAclRestrictionClause() );
        }

        if ( filters == null || filters.isEmpty() )
            return queryString.toString();

        StringBuilder conjunction = new StringBuilder();
        for ( ObjectFilter[] filterArray : filters ) {
            if ( filterArray.length == 0 )
                continue;
            StringBuilder disjunction = new StringBuilder();
            boolean first = true;
            for ( ObjectFilter filter : filterArray ) {
                if ( filter == null )
                    continue;
                if ( !first )
                    disjunction.append( "or " );
                if ( filter.getObjectAlias() != null ) {
                    disjunction.append( filter.getObjectAlias() ).append( "." );
                    disjunction.append( filter.getPropertyName() ).append( " " ).append( filter.getOperator() );
                    if ( filter.getOperator().equals( ObjectFilter.in ) ) {
                        disjunction.append( "( " ).append( ObjectFilterQueryUtils.formParamName( filter ) )
                                .append( " ) " );
                    } else {
                        disjunction.append( ":" ).append( ObjectFilterQueryUtils.formParamName( filter ) ).append( " " );
                    }
                }
                first = false;
            }
            String disjunctionString = disjunction.toString();
            if ( !disjunctionString.isEmpty() ) {
                conjunction.append( "and ( " ).append( disjunctionString ).append( " ) " );
            }
        }

        return queryString.append( conjunction ).toString();
    }

    /**
     * Adds all parameters contained in the filters argument to the Query by calling query.setParameter as needed.
     *
     * @param query   the query that needs parameters populated.
     * @param filters filters that provide the parameter values.
     */
    public static void addRestrictionParameters( Query query, List<ObjectFilter[]> filters ) {
        if ( !SecurityUtil.isUserAnonymous() && !SecurityUtil.isUserAdmin() ) {
            query.setParameter( "userName", SecurityUtil.getCurrentUsername() );
        }

        if ( filters == null || filters.isEmpty() )
            return;

        for ( ObjectFilter[] filterArray : filters ) {
            if ( filterArray == null || filterArray.length < 1 )
                continue;
            for ( ObjectFilter filter : filterArray ) {
                if ( filter == null )
                    continue;
                if ( Objects.equals( filter.getOperator(), ObjectFilter.in ) ) {
                    query.setParameterList( formParamName( filter ),
                            ( Collection<?> ) filter.getRequiredValue() );
                } else {
                    query.setParameter( formParamName( filter ), filter.getRequiredValue() );
                }
            }
        }
    }

    /**
     * Forms a parameter name out of the filter object.
     *
     * @param  filter the filter to create the parameter name out of.
     * @return a name unique to the provided filter that can be used in a hql query. returned string does not
     *                contain the leading ":" character that denotes a parameter keyword in the hql query.
     */
    private static String formParamName( ObjectFilter filter ) {
        return filter.getObjectAlias().replaceAll( "\\.", "_" ) + filter.getPropertyName().replaceAll( "\\.", "_" );
    }
}
