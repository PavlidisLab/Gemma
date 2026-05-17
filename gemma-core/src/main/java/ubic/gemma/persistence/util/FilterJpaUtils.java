package ubic.gemma.persistence.util;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static ubic.gemma.persistence.util.QueryUtils.escapeLike;

/**
 * JPA Criteria equivalent of {@link FilterQueryUtils}.
 * <p>
 * Phase 2 replacement for the Hibernate-Criteria-based {@code FilterCriteriaUtils} (deleted in
 * Step 3 along with the rest of the legacy Criteria API). Translates a {@link Filters}
 * conjunction-of-disjunctions into a single {@link Predicate} suitable for use with a JPA
 * {@link jakarta.persistence.criteria.CriteriaQuery}.
 *
 * @author poirigui (Phase 2 port)
 */
public class FilterJpaUtils {

    private FilterJpaUtils() {}

    /**
     * Build a JPA {@link Predicate} from a {@link Filters} (a conjunction of disjunctions). The
     * caller is responsible for attaching it to a {@code CriteriaQuery.where(...)} or otherwise
     * combining it with other predicates.
     * <p>
     * Returns {@link CriteriaBuilder#conjunction()} (an always-true predicate) when {@code filters}
     * is {@code null} or empty.
     */
    public static Predicate formRestrictionClause( CriteriaBuilder cb, Root<?> root, @Nullable Filters filters ) {
        if ( filters == null || filters.isEmpty() ) {
            return cb.conjunction();
        }
        List<Predicate> conjuncts = new ArrayList<>();
        for ( List<Filter> clause : filters ) {
            if ( clause == null || clause.isEmpty() ) continue;
            List<Predicate> disjuncts = new ArrayList<>( clause.size() );
            for ( Filter subClause : clause ) {
                if ( subClause == null ) continue;
                disjuncts.add( buildPredicate( cb, root, subClause ) );
            }
            if ( !disjuncts.isEmpty() ) {
                conjuncts.add( disjuncts.size() == 1 ? disjuncts.get( 0 ) : cb.or( disjuncts.toArray( new Predicate[ 0 ] ) ) );
            }
        }
        if ( conjuncts.isEmpty() ) {
            return cb.conjunction();
        }
        return conjuncts.size() == 1 ? conjuncts.get( 0 ) : cb.and( conjuncts.toArray( new Predicate[ 0 ] ) );
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Predicate buildPredicate( CriteriaBuilder cb, Root<?> root, Filter f ) {
        // Subquery and .size-suffix filters used to compile via FilterCriteriaUtils; that path was deleted
        // in Phase 2 Step 3 and the JPA-Criteria port hasn't reimplemented them yet. Callers that need
        // these operators should override the relevant load*/count method or use HQL via
        // AbstractQueryFilteringVoEnabledDao instead.
        if ( f.getRequiredValue() instanceof Subquery ) {
            throw new UnsupportedOperationException( "Subquery filters are not yet supported by the JPA-Criteria port; override the relevant DAO method." );
        }
        if ( f.getPropertyName().endsWith( ".size" ) ) {
            throw new UnsupportedOperationException( "Collection-size filters are not yet supported by the JPA-Criteria port; override the relevant DAO method." );
        }
        Path<?> path = resolvePath( root, f.getPropertyName() );
        Object value = f.getRequiredValue();
        switch ( f.getOperator() ) {
            case eq:
                return value == null ? path.isNull() : cb.equal( path, value );
            case notEq:
                return value == null ? path.isNotNull() : cb.notEqual( path, value );
            case like:
                return cb.like( ( Expression<String> ) path, escapeLike( ( String ) value ) + "%" );
            case notLike:
                return cb.notLike( ( Expression<String> ) path, escapeLike( ( String ) value ) + "%" );
            case lessThan:
                return cb.lessThan( ( Expression ) path, ( Comparable ) value );
            case greaterThan:
                return cb.greaterThan( ( Expression ) path, ( Comparable ) value );
            case lessOrEq:
                return cb.lessThanOrEqualTo( ( Expression ) path, ( Comparable ) value );
            case greaterOrEq:
                return cb.greaterThanOrEqualTo( ( Expression ) path, ( Comparable ) value );
            case in:
                return path.in( ( Collection<?> ) value );
            case notIn:
                return cb.not( path.in( ( Collection<?> ) value ) );
            default:
                throw new IllegalArgumentException( "Unsupported operator: " + f.getOperator() );
        }
    }

    /**
     * Walk a dot-separated property path from the root (e.g. {@code "taxon.commonName"} →
     * {@code root.get("taxon").get("commonName")}).
     */
    public static Path<?> resolvePath( Root<?> root, String propertyName ) {
        String[] parts = propertyName.split( "\\." );
        Path<?> p = root;
        for ( String part : parts ) {
            p = p.get( part );
        }
        return p;
    }
}
