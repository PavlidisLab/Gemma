package ubic.gemma.persistence.util;

import org.hibernate.Criteria;
import org.hibernate.criterion.*;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static ubic.gemma.core.util.ListUtils.padToNextPowerOfTwo;
import static ubic.gemma.persistence.util.PropertyMappingUtils.formProperty;

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
        for ( Iterable<Filter> clause : filters ) {
            if ( clause == null || !clause.iterator().hasNext() )
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
        String property = formProperty( filter );
        if ( property.endsWith( ".size" ) ) {
            if ( !( filter.getRequiredValue() instanceof Integer ) ) {
                throw new IllegalArgumentException( "Right hand size for a size-check must be a non-null integer." );
            }
            property = property.replaceFirst( "\\.size$", "" );
            int size = ( Integer ) filter.getRequiredValue();
            switch ( filter.getOperator() ) {
                case eq:
                    return Restrictions.sizeEq( property, size );
                case notEq:
                    return Restrictions.sizeNe( property, size );
                case lessThan:
                    return Restrictions.sizeLt( property, size );
                case lessOrEq:
                    return Restrictions.sizeLe( property, size );
                case greaterThan:
                    return Restrictions.sizeGt( property, size );
                case greaterOrEq:
                    return Restrictions.sizeGe( property, size );
                default:
                    throw new IllegalArgumentException( String.format( "Unsupported operator %s for a size-check.", filter.getOperator() ) );
            }
        }
        switch ( filter.getOperator() ) {
            case eq:
                if ( filter.getRequiredValue() == null ) {
                    return Restrictions.isNull( property );
                } else {
                    return Restrictions.eq( property, filter.getRequiredValue() );
                }
            case notEq:
                if ( filter.getRequiredValue() == null ) {
                    return Restrictions.isNotNull( property );
                } else {
                    return Restrictions.ne( property, filter.getRequiredValue() );
                }
            case like:
                return Restrictions.like( property, escapeLike( ( String ) requireNonNull( filter.getRequiredValue(), "Required value cannot be null for the like operator." ) ), MatchMode.START );
            case lessThan:
                return Restrictions.lt( property, filter.getRequiredValue() );
            case greaterThan:
                return Restrictions.gt( property, filter.getRequiredValue() );
            case lessOrEq:
                return Restrictions.le( property, filter.getRequiredValue() );
            case greaterOrEq:
                return Restrictions.ge( property, filter.getRequiredValue() );
            case in:
                List<?> item = requireNonNull( ( Collection<?> ) filter.getRequiredValue(), "Required value cannot be null for a collection." )
                        .stream().sorted().distinct().collect( Collectors.toList() );
                if ( item.isEmpty() ) {
                    throw new IllegalArgumentException( "The right hand size of a in operator cannot be empty." );
                }
                return Restrictions.in( property, padToNextPowerOfTwo( item, item.get( item.size() - 1 ) ) );
            default:
                throw new IllegalStateException( "Unexpected operator for filter: " + filter.getOperator() );
        }
    }

    private static String escapeLike( String s ) {
        return s.replace( "%", "\\%" )
                .replace( "_", "\\_" );
    }
}
