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
        switch ( filter.getOperator() ) {
            case eq:
                if ( filter.getRequiredValue() == null ) {
                    return Restrictions.isNull( ObjectFilterQueryUtils.formPropertyName( filter ) );
                } else {
                    return Restrictions.eq( ObjectFilterQueryUtils.formPropertyName( filter ), filter.getRequiredValue() );
                }
            case notEq:
                if ( filter.getRequiredValue() == null ) {
                    return Restrictions.isNotNull( ObjectFilterQueryUtils.formPropertyName( filter ) );
                } else {
                    return Restrictions.ne( ObjectFilterQueryUtils.formPropertyName( filter ), filter.getRequiredValue() );
                }
            case like:
                return Restrictions.like( ObjectFilterQueryUtils.formPropertyName( filter ), ( String ) filter.getRequiredValue(), MatchMode.ANYWHERE );
            case lessThan:
                return Restrictions.lt( ObjectFilterQueryUtils.formPropertyName( filter ), filter.getRequiredValue() );
            case greaterThan:
                return Restrictions.gt( ObjectFilterQueryUtils.formPropertyName( filter ), filter.getRequiredValue() );
            case lessOrEq:
                return Restrictions.le( ObjectFilterQueryUtils.formPropertyName( filter ), filter.getRequiredValue() );
            case greaterOrEq:
                return Restrictions.ge( ObjectFilterQueryUtils.formPropertyName( filter ), filter.getRequiredValue() );
            case in:
                return Restrictions.in( ObjectFilterQueryUtils.formPropertyName( filter ), ( Collection<?> ) filter.getRequiredValue() );
            default:
                throw new IllegalStateException( "Unexpected operator for filter: " + filter.getOperator() );
        }
    }
}
