package ubic.gemma.persistence.util;

import org.hibernate.Criteria;
import org.hibernate.criterion.*;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Objects;

/**
 * Utilities for integrating {@link Filter} with Hibernate {@link Criteria} API.
 * @author poirigui
 */
public class FilterCriteriaUtils {

    /**
     * Form a restriction clause using a {@link Criterion}.
     * @see FilterQueryUtils#formRestrictionClause(Filters)
     * @param filters the filters to use to create the clause
     * @return a restriction clause that can be appended to a {@link Criteria} using {@link Criteria#add(Criterion)}
     */
    public static Criterion formRestrictionClause( @Nullable Filters filters ) {
        Conjunction c = Restrictions.conjunction();
        if ( filters == null || filters.isEmpty() )
            return c;
        for ( Filter[] clause : filters ) {
            if ( clause == null || clause.length == 0 )
                continue;
            Disjunction d = Restrictions.disjunction();
            for ( Filter subClause : clause ) {
                d.add( formRestrictionClause( subClause ) );
            }
            c.add( d );
        }
        return c;
    }

    private static Criterion formRestrictionClause( Filter filter ) {
        String propertyName;
        if ( filter.getObjectAlias() != null ) {
            propertyName = filter.getObjectAlias() + "." + filter.getPropertyName();
        } else {
            propertyName = filter.getPropertyName();
        }
        if ( propertyName.endsWith( ".size" ) ) {
            if ( !( filter.getRequiredValue() instanceof Integer ) ) {
                throw new IllegalArgumentException( "Right hand size for a size-check must be a non-null integer." );
            }
            propertyName = propertyName.replaceFirst( "\\.size$", "" );
            int size = ( Integer ) filter.getRequiredValue();
            switch ( filter.getOperator() ) {
                case eq:
                    return Restrictions.sizeEq( propertyName, size );
                case notEq:
                    return Restrictions.sizeNe( propertyName, size );
                case lessThan:
                    return Restrictions.sizeLt( propertyName, size );
                case lessOrEq:
                    return Restrictions.sizeLe( propertyName, size );
                case greaterThan:
                    return Restrictions.sizeGt( propertyName, size );
                case greaterOrEq:
                    return Restrictions.sizeGe( propertyName, size );
                default:
                    throw new IllegalArgumentException( String.format( "Unsupported operator %s for a size-check.", filter.getOperator() ) );
            }
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
                return Restrictions.in( propertyName, ( Collection<?> ) Objects.requireNonNull( filter.getRequiredValue(),
                        "Required value cannot be null for a collection." ) );
            default:
                throw new IllegalStateException( "Unexpected operator for filter: " + filter.getOperator() );
        }
    }
}
