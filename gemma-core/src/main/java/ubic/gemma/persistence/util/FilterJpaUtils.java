package ubic.gemma.persistence.util;

import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CommonAbstractCriteria;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.lang.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * Build a JPA {@link Predicate} from a {@link Filters} (a conjunction of disjunctions).
     * <p>
     * Returns {@link CriteriaBuilder#conjunction()} (an always-true predicate) when {@code filters}
     * is {@code null} or empty.
     *
     * @param cb     criteria builder.
     * @param query  the enclosing {@link AbstractQuery} (a {@code CriteriaQuery} or
     *               {@code Subquery}); used to create JPA subqueries for {@code inSubquery} /
     *               {@code notInSubquery} filters. May be {@code null} if the caller is certain no
     *               subquery filters will be encountered.
     * @param root   the root of the outer query the predicate will be attached to.
     * @param filters the filters to render.
     */
    public static Predicate formRestrictionClause( CriteriaBuilder cb, @Nullable CommonAbstractCriteria query, Root<?> root, @Nullable Filters filters ) {
        return formRestrictionClause( cb, query, root, filters, null );
    }

    /**
     * Variant of {@link #formRestrictionClause(CriteriaBuilder, CommonAbstractCriteria, Root, Filters)}
     * that honours a registered {@code objectAlias → dotted-prefix} map so alias-prefixed paths
     * (e.g. {@code Filter(objectAlias="bc", propertyName="value")} where {@code bc} was registered
     * with prefix {@code baselineGroup.characteristics}) resolve via an explicit join on the root.
     */
    public static Predicate formRestrictionClause( CriteriaBuilder cb, @Nullable CommonAbstractCriteria query, Root<?> root, @Nullable Filters filters, @Nullable Map<String, String> aliasPrefixes ) {
        if ( filters == null || filters.isEmpty() ) {
            return cb.conjunction();
        }
        List<Predicate> conjuncts = new ArrayList<>();
        for ( List<Filter> clause : filters ) {
            if ( clause == null || clause.isEmpty() ) continue;
            List<Predicate> disjuncts = new ArrayList<>( clause.size() );
            for ( Filter subClause : clause ) {
                if ( subClause == null ) continue;
                disjuncts.add( buildPredicate( cb, query, root, subClause, aliasPrefixes ) );
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

    /**
     * Convenience overload for callers that don't need subquery support (back-compat shim).
     *
     * @deprecated prefer {@link #formRestrictionClause(CriteriaBuilder, CommonAbstractCriteria, Root, Filters)}
     *     so {@code inSubquery} filters can resolve.
     */
    @Deprecated
    public static Predicate formRestrictionClause( CriteriaBuilder cb, Root<?> root, @Nullable Filters filters ) {
        return formRestrictionClause( cb, null, root, filters );
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Predicate buildPredicate( CriteriaBuilder cb, @Nullable CommonAbstractCriteria enclosingQuery, Root<?> root, Filter f, @Nullable Map<String, String> aliasPrefixes ) {
        // Subquery filters: build a JPA jakarta.persistence.criteria.Subquery and emit an IN /
        // NOT IN predicate against the outer root path. Pre-Phase-2 this lived in the deleted
        // FilterCriteriaUtils; the HQL equivalent is in FilterQueryUtils.formSubClause.
        if ( f.getRequiredValue() instanceof Subquery sq ) {
            if ( !( enclosingQuery instanceof AbstractQuery ) ) {
                throw new UnsupportedOperationException(
                        "Subquery filters require a CriteriaQuery / Subquery to host the nested SELECT; "
                                + "caller passed null. Use the (CriteriaBuilder, CommonAbstractCriteria, Root, Filters) overload." );
            }
            return buildSubqueryPredicate( cb, ( AbstractQuery<?> ) enclosingQuery, root, f, sq );
        }
        // .size-suffix filters: cb.size(collection-expression) returns an Expression<Integer>.
        if ( f.getPropertyName().endsWith( ".size" ) ) {
            return buildSizePredicate( cb, root, f, aliasPrefixes );
        }
        Path<?> path = resolvePathWithAlias( root, f.getObjectAlias(), f.getPropertyName(), aliasPrefixes );
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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Predicate buildSizePredicate( CriteriaBuilder cb, Root<?> root, Filter f, @Nullable Map<String, String> aliasPrefixes ) {
        String collectionPath = f.getPropertyName().substring( 0, f.getPropertyName().length() - ".size".length() );
        // Walk the property path; the final segment must be a collection-typed attribute.
        Path<?> path = resolvePathWithAlias( root, f.getObjectAlias(), collectionPath, aliasPrefixes );
        Expression<Integer> sizeExpr = cb.size( ( Expression ) path );
        Object value = f.getRequiredValue();
        switch ( f.getOperator() ) {
            case eq:
                return cb.equal( sizeExpr, value );
            case notEq:
                return cb.notEqual( sizeExpr, value );
            case lessThan:
                return cb.lessThan( sizeExpr, ( Expression<Integer> ) cb.literal( ( ( Number ) value ).intValue() ) );
            case greaterThan:
                return cb.greaterThan( sizeExpr, ( Expression<Integer> ) cb.literal( ( ( Number ) value ).intValue() ) );
            case lessOrEq:
                return cb.lessThanOrEqualTo( sizeExpr, ( Expression<Integer> ) cb.literal( ( ( Number ) value ).intValue() ) );
            case greaterOrEq:
                return cb.greaterThanOrEqualTo( sizeExpr, ( Expression<Integer> ) cb.literal( ( ( Number ) value ).intValue() ) );
            default:
                throw new IllegalArgumentException( "Unsupported operator for collection-size filter: " + f.getOperator() );
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Predicate buildSubqueryPredicate( CriteriaBuilder cb, AbstractQuery<?> enclosingQuery, Root<?> root, Filter f, Subquery sq ) {
        // Locate the entity class via thread-context classloader so we don't need the call site to
        // pass it explicitly. The Subquery model carries the FQN as a String.
        Class<?> subRootEntityClass;
        try {
            subRootEntityClass = Class.forName( sq.getEntityName(), false, Thread.currentThread().getContextClassLoader() );
        } catch ( ClassNotFoundException e ) {
            throw new IllegalStateException( "Cannot resolve subquery entity " + sq.getEntityName(), e );
        }
        jakarta.persistence.criteria.Subquery<Long> jpaSub = enclosingQuery.subquery( Long.class );
        Root<?> subRoot = jpaSub.from( subRootEntityClass );

        // Build an alias -> From map so the inner filter's objectAlias can resolve. The root alias
        // (e.g. "e") is what Subquery#getRootAlias() returns.
        Map<String, From<?, ?>> aliasMap = new HashMap<>();
        aliasMap.put( sq.getRootAlias(), subRoot );
        for ( Subquery.Alias a : ( List<Subquery.Alias> ) sq.getAliases() ) {
            if ( a.getPropertyName().isEmpty() ) {
                // Root-alias declaration; already mapped above.
                continue;
            }
            From<?, ?> parent = a.getObjectAlias() != null ? aliasMap.get( a.getObjectAlias() ) : subRoot;
            if ( parent == null ) {
                throw new IllegalStateException( "Unresolvable subquery alias '" + a.getObjectAlias() + "'." );
            }
            From<?, ?> joined = ( From<?, ?> ) parent.join( a.getPropertyName() );
            aliasMap.put( a.getAlias(), joined );
        }

        jpaSub.select( ( Expression<Long> ) ( Expression ) subRoot.get( sq.getPropertyName() ) );

        // Render the inner filter against the appropriate From in the alias map. Pre-Phase-2 the
        // inner filter's right-hand side was always a scalar/collection (never another Subquery);
        // we keep that assumption here and use the leaf-predicate helper which resolves paths off
        // the inner From rather than the outer Root.
        // A subquery may carry several conjoined filters, all binding to the same element of the
        // relation. Conjoining them INSIDE the subquery is the whole point — hoisting them to the
        // outer query would ask "some element matches X and some element matches Y" instead.
        java.util.List<Predicate> innerPredicates = new java.util.ArrayList<>();
        for ( Filter innerFilter : sq.getFilters() ) {
            From<?, ?> innerFrom = innerFilter.getObjectAlias() != null ? aliasMap.get( innerFilter.getObjectAlias() ) : subRoot;
            if ( innerFrom == null ) {
                throw new IllegalStateException( "Unresolvable inner filter alias '" + innerFilter.getObjectAlias() + "'." );
            }
            innerPredicates.add( buildLeafPredicate( cb, innerFrom, innerFilter ) );
        }
        jpaSub.where( innerPredicates.size() == 1 ? innerPredicates.get( 0 )
                : cb.and( innerPredicates.toArray( new Predicate[0] ) ) );

        Path<?> outerPath = resolvePath( root, f.getPropertyName() );
        CriteriaBuilder.In<Object> inPred = cb.in( ( Expression<Object> ) outerPath );
        inPred.value( ( jakarta.persistence.criteria.Subquery ) jpaSub );
        switch ( f.getOperator() ) {
            case inSubquery:
                return inPred;
            case notInSubquery:
                return cb.not( inPred );
            default:
                throw new IllegalArgumentException( "Unsupported operator for subquery filter: " + f.getOperator() );
        }
    }

    /**
     * Build a single (non-subquery, non-size) predicate against the given {@link From}, which may
     * be a Root or a Join. Mirrors the body of {@link #buildPredicate} but resolves the property
     * path off {@code from} rather than a Root.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Predicate buildLeafPredicate( CriteriaBuilder cb, From<?, ?> from, Filter f ) {
        Path<?> path = resolvePathFrom( from, f.getPropertyName() );
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
                throw new IllegalArgumentException( "Unsupported operator for leaf filter in subquery: " + f.getOperator() );
        }
    }

    private static Path<?> resolvePathFrom( From<?, ?> from, String propertyName ) {
        String[] parts = propertyName.split( "\\." );
        Path<?> p = from;
        for ( String part : parts ) {
            p = p.get( part );
        }
        return p;
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

    /**
     * Walk a dot-separated property path from the root, prepending the dotted prefix that the
     * given {@code objectAlias} was registered with (if any).
     * <p>
     * Pre-Phase-2 the legacy Hibernate Criteria implementation tracked aliases natively through
     * {@code Subcriteria}; the JPA Criteria port doesn't have a built-in alias-to-join mechanism,
     * so {@link ubic.gemma.persistence.service.AbstractCriteriaFilteringVoEnabledDao} threads the {@code alias → prefix} map
     * (sourced from {@link ubic.gemma.persistence.service.AbstractFilteringVoEnabledDao#getFilterablePropertyObjectAliases()})
     * through to this helper. When the alias is recognised, the registered prefix is walked first
     * (creating implicit joins via {@code Path.get}), then the leaf property name is walked on top.
     */
    public static Path<?> resolvePathWithAlias( Root<?> root, @Nullable String objectAlias, String propertyName, @Nullable Map<String, String> aliasPrefixes ) {
        Path<?> base = root;
        if ( objectAlias != null && aliasPrefixes != null ) {
            String prefix = aliasPrefixes.get( objectAlias );
            if ( prefix != null && !prefix.isEmpty() ) {
                for ( String part : prefix.split( "\\." ) ) {
                    base = base.get( part );
                }
            }
        }
        for ( String part : propertyName.split( "\\." ) ) {
            base = base.get( part );
        }
        return base;
    }
}
