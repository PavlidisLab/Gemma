package ubic.gemma.persistence.util;

import org.hibernate.Criteria;
import org.hibernate.criterion.*;

import java.util.Collection;

/**
 * Utilities for integrating {@link ObjectFilter} with Hibernate {@link Criteria} API.
 * @author poirigui
 */
public class ObjectFilterCriteriaUtils {

    /**
     * Form a restriction clause using a {@link Criterion}.
     * @see ObjectFilterQueryUtils#formRestrictionClause(Filters)
     * @param objectFilters the filters to use to create the clause
     * @return a restriction clause that can be appended to a {@link Criteria} using {@link Criteria#add(Criterion)}
     */
    public static Criterion formRestrictionClause( Filters objectFilters ) {
        Conjunction c = Restrictions.conjunction();
        if ( objectFilters == null || objectFilters.isEmpty() )
            return c;
        for ( ObjectFilter[] filters : objectFilters ) {
            if ( filters == null || filters.length == 0 )
                continue;
            Disjunction d = Restrictions.disjunction();
            for ( ObjectFilter filter : filters ) {
                d.add( formRestrictionClause( filter ) );
            }
            c.add( d );
        }
        return c;
    }

    private static Criterion formRestrictionClause( ObjectFilter filter ) {
        String propertyName;
        if ( filter.getObjectAlias() != null ) {
            propertyName = filter.getObjectAlias() + "." + filter.getPropertyName();
        } else {
            propertyName = filter.getPropertyName();
        }
        switch ( filter.getOperator() ) {
            case eq:
                if ( filter.getRequiredValue() == null ) {
                    return Restrictions.isNull( propertyName );
                } else {
                    return Restrictions.eq( propertyName, filter.getRequiredValue() );
                }
            case notEq:
                if ( filter.getRequiredValue() == null ) {
                    return Restrictions.isNotNull( propertyName );
                } else {
                    return Restrictions.ne( propertyName, filter.getRequiredValue() );
                }
            case like:
                return Restrictions.like( propertyName, ( String ) filter.getRequiredValue(), MatchMode.ANYWHERE );
            case lessThan:
                return Restrictions.lt( propertyName, filter.getRequiredValue() );
            case greaterThan:
                return Restrictions.gt( propertyName, filter.getRequiredValue() );
            case lessOrEq:
                return Restrictions.le( propertyName, filter.getRequiredValue() );
            case greaterOrEq:
                return Restrictions.ge( propertyName, filter.getRequiredValue() );
            case in:
                return Restrictions.in( propertyName, ( Collection<?> ) filter.getRequiredValue() );
            default:
                throw new IllegalStateException( "Unexpected operator for filter: " + filter.getOperator() );
        }
    }
}
