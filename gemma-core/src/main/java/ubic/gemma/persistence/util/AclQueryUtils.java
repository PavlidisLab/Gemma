package ubic.gemma.persistence.util;

import ubic.gemma.core.security.util.SecurityUtil;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.query.Query;
import org.hibernate.QueryParameterException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;
import org.springframework.util.Assert;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.auditAndSecurity.SecuredChild;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Utilities for integrating ACL into {@link Query}.
 * <p>
 * To build a query, sequentially proceed as follows:
 * <ol>
 * <li>form your select clause and your jointures</li>
 * <li>concatenate {@link #formAclRestrictionClause(String)} in the jointure section</li>
 * <li>form where clause and add your constraints</li>
 * <li>concatenate {@link #formNativeAclRestrictionClause(SessionFactoryImplementor, String)} in the clause section (only for native queries)</li>
 * <li>bind all your parameters</li>
 * <li>bind ACL-specific parameters with {@link #addAclParameters(Query, Class)} to the query object</li>
 * </ol>
 *
 * <h2>EXISTS rewrite (Session 2 of the ACL EXISTS refactor)</h2>
 * <p>
 * {@link #formAclRestrictionClause(String, Permission)} historically emitted a Cartesian
 * {@code , AclObjectIdentity as aoi join aoi.ownerSid sid [left join aoi.entries ace] where (...)}
 * fragment that multiplied result rows whenever an AOI had more than one matching ACE. Callers
 * worked around the row multiplication by sprinkling {@code distinct} and {@code group by}.
 * <p>
 * The emitted clause is now a correlated {@code EXISTS} sub-query against
 * {@link ubic.gemma.core.security.acl.domain.AclObjectIdentity}. Semantics are identical
 * (anonymous role check, group-grant check, owner-principal check, admin bypass) but no
 * row multiplication can occur, so the {@code distinct} / {@code group by} compensations have
 * been removed (Session 3 cleanup). The {@code aoi} / {@code sid} HQL aliases that the old
 * clause exposed in scope are <strong>no longer visible</strong> to the surrounding query —
 * callers that need the ACL info (only {@code ExpressionExperimentDaoImpl.getFilteringQuery})
 * must post-fetch via {@link #loadAclInfoFor(org.hibernate.Session, java.util.Collection, Class)}.
 *
 * <h2>Native callers: explicit id column (HQL_SQL_AUDIT C5)</h2>
 * <p>
 * Native callers previously had to invoke a {@code formNativeAclJoinClause(String)} shim before
 * {@link #formNativeAclRestrictionClause(SessionFactoryImplementor, String)} so the id column
 * could be threaded across via a {@link ThreadLocal}. That coupling is gone: the restriction
 * clause now takes the {@code aoiIdColumn} as an explicit parameter, and the join shim has
 * been removed.
 *
 * @author poirigui
 */
public class AclQueryUtils {

    /**
     * Alias used for the object identity {@link ubic.gemma.core.security.acl.domain.AclObjectIdentity}
     * and the owner identity {@link ubic.gemma.core.security.acl.domain.AclSid} inside the EXISTS body.
     * <p>
     * Note: after the EXISTS rewrite, {@link #formAclRestrictionClause(String, Permission)} no
     * longer leaves these aliases in the outer query's scope (the sub-query has its own scope).
     * Native callers still rely on the aliases for the {@code on}-clause shape.
     */
    public static final String
            AOI_ALIAS = "aoi",
            SID_ALIAS = "sid";

    /**
     * Do not refer to ACEs in your code, it might not be present in the query.
     */
    private static final String ACE_ALIAS = "ace";

    /**
     * Parameter name prefix to avoid clashes with user-defined parameters.
     */
    private static final String PARAM_PREFIX = "aclQueryUtils_";
    private static final String
            AOI_TYPE_PARAM = PARAM_PREFIX + "aoiType",
    // HQL-only: the resolved acl_class.id, used in place of the formula-backed
    // `aoi.type = :aoiType` so MySQL doesn't emit a correlated subquery per
    // outer row. See the formAclRestrictionClause comment.
            AOI_CLASS_ID_PARAM = PARAM_PREFIX + "aoiClassId";
    static final String USER_NAME_PARAM = PARAM_PREFIX + "userName";

    /**
     * Cache of {@code acl_class.id} keyed by {@link Class#getCanonicalName()}.
     * Stable across JVM lifetime (acl_class rows are insert-once on first
     * Securable registration). Lookup is via a one-shot native query the first
     * time a given class name is bound.
     */
    private static final ConcurrentHashMap<String, Long> ACL_CLASS_ID_CACHE = new ConcurrentHashMap<>();

    /**
     * Select all the SIDs that belong to a given user (specified by a :userName parameter).
     */
    //language=HQL
    private static final String CURRENT_USER_SIDS_HQL =
            "select sid from UserGroup as ug join ug.authorities as ga, AclGrantedAuthoritySid sid "
                    + "where sid.grantedAuthority = CONCAT('GROUP_', ga.authority) "
                    + "and ug.name in (select ug.name from UserGroup ug join ug.groupMembers memb where memb.userName = :" + USER_NAME_PARAM + ")";

    //language=HQL
    private static final String ANONYMOUS_SID_HQL = "select sid from AclGrantedAuthoritySid sid where sid.grantedAuthority = 'IS_AUTHENTICATED_ANONYMOUSLY'";

    /**
     * Native SQL version of {@link #CURRENT_USER_SIDS_HQL}.
     * <p>
     * Targets Spring Security 6's canonical {@code acl_sid} schema where {@code principal}
     * is a 0/1 discriminator (0 = GrantedAuthoritySid, 1 = PrincipalSid) and the SID name
     * lives in the {@code sid} column.
     */
    //language=SQL
    static final String CURRENT_USER_SIDS_SQL =
            "select sid.id from USER_GROUP as UG "
                    + "join GROUP_AUTHORITY GA on UG.ID = GA.GROUP_FK "
                    + "join acl_sid sid on (sid.principal = 0 and sid.sid = CONCAT('GROUP_', GA.AUTHORITY)) "
                    + "join GROUP_MEMBERS GM on UG.ID = GM.USER_GROUPS_FK "
                    + "join CONTACT C on GM.GROUP_MEMBERS_FK = C.ID "
                    + "where C.USER_NAME = :" + USER_NAME_PARAM;

    //language=SQL
    static final String ANONYMOUS_SID_SQL = "select sid.id from acl_sid sid where sid.principal = 0 and sid.sid = 'IS_AUTHENTICATED_ANONYMOUSLY'";

    /**
     * Create an HQL restriction clause with the {@link BasePermission#READ} permission.
     * @see #formAclRestrictionClause(String, Permission)
     */
    public static String formAclRestrictionClause( String aoiIdColumn ) {
        return formAclRestrictionClause( aoiIdColumn, BasePermission.READ );
    }

    /**
     * Create an HQL restriction clause that limits the result only to objects the current user can access.
     * <p>
     * The clause is a correlated {@code EXISTS} sub-query against
     * {@link ubic.gemma.core.security.acl.domain.AclObjectIdentity} that preserves the
     * pre-refactor security semantics (anonymous role check, group-grant check,
     * owner-principal check, admin bypass) but does <strong>not</strong> multiply rows the
     * way the old Cartesian join did. As a consequence:
     * <ul>
     *     <li>Callers no longer need {@code distinct} / {@code group by} compensations; the
     *     historical {@code requiresCountDistinct()} / {@code requiresGroupBy()} helpers have
     *     been removed (Session 3 cleanup).</li>
     *     <li>The {@code aoi} / {@code sid} HQL aliases are <strong>no longer in scope</strong>
     *     in the surrounding query (the sub-query has its own scope). Callers that need to
     *     project ACL info must post-fetch via
     *     {@link #loadAclInfoFor(org.hibernate.Session, java.util.Collection, Class)}.</li>
     * </ul>
     * <p>
     * The emission shape is {@code " where (exists (...))"} (or empty for admin), so callers
     * can continue to concatenate {@code " and ..."} after it the same way they did with
     * the old JOIN clause. Empty for admin because no filtering is needed; callers that need
     * a {@code where} clause for their own predicates must therefore introduce one themselves
     * — but in practice the existing pattern is {@code "from X x " + formAclRestrictionClause(...) + " and ..."}
     * which short-circuits in the admin case to {@code "from X x  and ..."} which is invalid.
     * The {@link #formAclRestrictionClause(String, Permission)} contract therefore now always
     * emits at least a {@code " where (1=1)"} placeholder when the caller has no other WHERE,
     * preserving the {@code " and ..."} concatenation idiom for all three principal classes.
     *
     * @param aoiIdColumn column name to match against the ACL object identity, the object class is passed via
     *                    {@link #addAclParameters(Query, Class)} afterward
     * @param permission  requested permission(s)
     * @return clause to add to the query after any jointure
     */
    public static String formAclRestrictionClause( String aoiIdColumn, Permission permission ) {
        if ( StringUtils.isBlank( aoiIdColumn ) ) {
            throw new IllegalArgumentException( "Object identity column cannot be empty." );
        }
        Assert.isTrue( permission.getMask() > 0, "The mask must have at least one bit set." );

        // Admin bypass: no ACL filter, but we still must emit a `where` so callers can append
        // ` and X`. The 1=1 placeholder is the cheapest way to keep the caller-side
        // concatenation idiom honest.
        if ( SecurityUtil.isUserAdmin() ) {
            //language=HQL
            return " where (1=1)";
        }

        // Build the EXISTS body: select 1 from AclObjectIdentity aoi [join sid] [left join aoi.entries ace]
        //   where aoi.identifier = <aoiIdCol> and aoi.objectIdClass = :aoiClassId
        //         and (<acl predicates>)
        //
        // Filtering on aoi.objectIdClass (the mapped BIGINT column) instead of
        // aoi.type (a formula property that resolves to a correlated subquery on
        // acl_class) lets MySQL fold the AOI class lookup to a const. With
        // aoi.type the planner re-ran acl_class per outer row and the
        // /datasets/count query never finished against prod cardinalities.
        //language=HQL
        StringBuilder exists = new StringBuilder( 256 );
        exists.append( "exists (select 1 from AclObjectIdentity " ).append( AOI_ALIAS )
                .append( " join " ).append( AOI_ALIAS ).append( ".ownerSid " ).append( SID_ALIAS );

        if ( SecurityUtil.isUserAnonymous() ) {
            // Anonymous: only the ACE check is in play; SID isn't used in the predicate but
            // joining keeps the shape consistent with the authenticated branch.
            exists.append( " join " ).append( AOI_ALIAS ).append( ".entries " ).append( ACE_ALIAS )
                    .append( " where " ).append( AOI_ALIAS ).append( ".identifier = " ).append( aoiIdColumn )
                    .append( " and " ).append( AOI_ALIAS ).append( ".objectIdClass = :" ).append( AOI_CLASS_ID_PARAM )
                    .append( " and bitand(" ).append( ACE_ALIAS ).append( ".mask, " ).append( permission.getMask() )
                    .append( ") <> 0 and " ).append( ACE_ALIAS ).append( ".sid in (" ).append( ANONYMOUS_SID_HQL ).append( "))" );
        } else {
            // Authenticated non-admin: owner-or-grant predicate. The original code left-joined
            // aoi.entries so that owner-without-ACE rows still matched; we model the disjunction
            // explicitly with two EXISTS variants OR'd together.
            exists.append( " left join " ).append( AOI_ALIAS ).append( ".entries " ).append( ACE_ALIAS )
                    .append( " where " ).append( AOI_ALIAS ).append( ".identifier = " ).append( aoiIdColumn )
                    .append( " and " ).append( AOI_ALIAS ).append( ".objectIdClass = :" ).append( AOI_CLASS_ID_PARAM )
                    .append( " and (" )
                    // user owns the object
                    .append( SID_ALIAS ).append( ".principal = :" ).append( USER_NAME_PARAM ).append( " " )
                    // specific rights to the object
                    .append( "or (" ).append( ACE_ALIAS ).append( ".sid in (" ).append( CURRENT_USER_SIDS_HQL )
                    .append( ") and bitand(" ).append( ACE_ALIAS ).append( ".mask, " ).append( permission.getMask() ).append( ") <> 0) " )
                    // publicly available
                    .append( "or (" ).append( ACE_ALIAS ).append( ".sid in (" ).append( ANONYMOUS_SID_HQL )
                    .append( ") and bitand(" ).append( ACE_ALIAS ).append( ".mask, " ).append( permission.getMask() ).append( ") <> 0)" )
                    .append( "))" );
        }

        return " where (" + exists + ")";
    }

    /**
     * Batched post-fetch of ACL info (object identity + owner SID) for a set of entity ids
     * of a given {@link Securable} type. Replaces the {@code select ee, aoi, sid} projection
     * that the old JOIN-form ACL clause supported — the EXISTS-form clause can no longer
     * propagate {@code aoi}/{@code sid} into the outer projection.
     *
     * @param session    Hibernate session to run the query on
     * @param ids        the entity ids whose ACL info to fetch; empty input returns empty map
     * @param aoiType    the AOI type (the entity class) — used to scope the lookup
     * @return a map keyed by entity id, value is a {@link org.apache.commons.lang3.tuple.Pair}
     *         of (AclObjectIdentity, AclSid). Ids without ACL rows are absent.
     */
    public static java.util.Map<Long, org.apache.commons.lang3.tuple.Pair<ubic.gemma.core.security.acl.domain.AclObjectIdentity, ubic.gemma.core.security.acl.domain.AclSid>>
    loadAclInfoFor( org.hibernate.Session session, java.util.Collection<Long> ids, Class<? extends Securable> aoiType ) {
        if ( ids == null || ids.isEmpty() ) {
            return java.util.Collections.emptyMap();
        }
        //language=HQL
        // Filter on aoi.objectIdClass (indexed BIGINT) rather than aoi.type (formula
        // resolving to a correlated subquery on acl_class) — see the formAclRestrictionClause
        // comment. The IN list here is the
        // post-fetch batch (usually ≤ 128 ids) so the formula form wasn't catastrophic, but
        // applying the same shape keeps the surface consistent.
        Long aoiClassId = resolveAclClassId( aoiType.getCanonicalName() );
        @SuppressWarnings("unchecked")
        java.util.List<Object[]> rows = session
                .createQuery( "select aoi.identifier, aoi, aoi.ownerSid from AclObjectIdentity aoi "
                        + "where aoi.identifier in :ids and aoi.objectIdClass = :aoiClassId" )
                .setParameterList( "ids", ids )
                .setParameter( "aoiClassId", aoiClassId )
                .list();
        java.util.Map<Long, org.apache.commons.lang3.tuple.Pair<ubic.gemma.core.security.acl.domain.AclObjectIdentity, ubic.gemma.core.security.acl.domain.AclSid>> out
                = new java.util.HashMap<>( rows.size() * 2 );
        for ( Object[] row : rows ) {
            Long id = ( Long ) row[0];
            ubic.gemma.core.security.acl.domain.AclObjectIdentity aoi = ( ubic.gemma.core.security.acl.domain.AclObjectIdentity ) row[1];
            ubic.gemma.core.security.acl.domain.AclSid sid = ( ubic.gemma.core.security.acl.domain.AclSid ) row[2];
            out.put( id, org.apache.commons.lang3.tuple.Pair.of( aoi, sid ) );
        }
        return out;
    }

    /**
     * Native flavour of the ACL restriction clause with a {@link BasePermission#READ} permission.
     * @see #formNativeAclRestrictionClause(SessionFactoryImplementor, String, Permission)
     */
    public static String formNativeAclRestrictionClause( SessionFactoryImplementor sessionFactoryImplementor, String aoiIdColumn ) {
        return formNativeAclRestrictionClause( sessionFactoryImplementor, aoiIdColumn, BasePermission.READ );
    }

    /**
     * Native flavour of the ACL restriction clause.
     * <p>
     * Emits a self-contained {@code " and exists (...)"} clause that correlates back to the
     * outer query via the supplied {@code aoiIdColumn}.
     *
     * @param sessionFactoryImplementor session factory implementor used to dialect-render the
     *                                  bitwise-AND fragment
     * @param aoiIdColumn               outer-query column to correlate against
     *                                  {@code acl_object_identity.object_id_identity}; must be
     *                                  non-blank (must be a SQL column reference, never user input)
     * @param permission                requested permission(s)
     * @see #formAclRestrictionClause(String, Permission)
     */
    public static String formNativeAclRestrictionClause( SessionFactoryImplementor sessionFactoryImplementor, String aoiIdColumn, Permission permission ) {
        if ( StringUtils.isBlank( aoiIdColumn ) ) {
            throw new IllegalArgumentException( "Object identity column cannot be empty." );
        }
        if ( SecurityUtil.isUserAdmin() ) {
            return "";
        }
        // Dialect-aware bitwise AND (MySQL emits "(a & b)", H2 emits "BITAND(a, b)").
        String renderedMask = BitwiseUtils.bitand( sessionFactoryImplementor.getJdbcServices().getDialect(),
                ACE_ALIAS + ".mask", String.valueOf( permission.getMask() ) );
        //language=SQL
        StringBuilder sb = new StringBuilder( 384 );
        sb.append( " and exists (select 1 from acl_object_identity " ).append( AOI_ALIAS )
                .append( " join acl_class " ).append( AOI_ALIAS ).append( "_cls on (" )
                .append( AOI_ALIAS ).append( "_cls.id = " ).append( AOI_ALIAS ).append( ".object_id_class)" )
                .append( " join acl_sid " ).append( SID_ALIAS ).append( " on (" )
                .append( SID_ALIAS ).append( ".id = " ).append( AOI_ALIAS ).append( ".owner_sid)" );
        if ( SecurityUtil.isUserAnonymous() ) {
            sb.append( " join acl_entry " ).append( ACE_ALIAS )
                    .append( " on (" ).append( AOI_ALIAS ).append( ".id = " ).append( ACE_ALIAS ).append( ".acl_object_identity)" )
                    .append( " where " ).append( AOI_ALIAS ).append( ".object_id_identity = " ).append( aoiIdColumn )
                    .append( " and " ).append( AOI_ALIAS ).append( "_cls.class = :" ).append( AOI_TYPE_PARAM )
                    .append( " and " ).append( renderedMask ).append( " <> 0" )
                    .append( " and " ).append( ACE_ALIAS ).append( ".sid in (" ).append( ANONYMOUS_SID_SQL ).append( "))" );
        } else {
            sb.append( " left join acl_entry " ).append( ACE_ALIAS )
                    .append( " on (" ).append( AOI_ALIAS ).append( ".id = " ).append( ACE_ALIAS ).append( ".acl_object_identity)" )
                    .append( " where " ).append( AOI_ALIAS ).append( ".object_id_identity = " ).append( aoiIdColumn )
                    .append( " and " ).append( AOI_ALIAS ).append( "_cls.class = :" ).append( AOI_TYPE_PARAM )
                    .append( " and (" )
                    // user owns the object
                    .append( "(" ).append( SID_ALIAS ).append( ".principal = 1 and " ).append( SID_ALIAS ).append( ".sid = :" ).append( USER_NAME_PARAM ).append( ") " )
                    // specific rights to the object
                    .append( "or (" ).append( ACE_ALIAS ).append( ".sid in (" ).append( CURRENT_USER_SIDS_SQL )
                    .append( ") and " ).append( renderedMask ).append( " <> 0) " )
                    // publicly available
                    .append( "or (" ).append( ACE_ALIAS ).append( ".sid in (" ).append( ANONYMOUS_SID_SQL )
                    .append( ") and " ).append( renderedMask ).append( " <> 0)" )
                    .append( "))" );
        }
        return sb.toString();
    }

    /**
     * Bind {@link Query} parameters to a join clause generated with {@link #formAclRestrictionClause(String)} and add ACL
     * restriction parameters defined in {@link #formAclRestrictionClause(String)}.
     * <p>
     * This method also work for native queries formed with
     * {@link #formNativeAclRestrictionClause(SessionFactoryImplementor, String)}.
     *
     * @param query   a {@link Query} object that contains the join and restriction clauses
     * @param aoiType the AOI type to be bound in the query
     * @throws QueryParameterException if any defined parameters are missing, which is typically due to a missing prior
     *                                 {@link #formAclRestrictionClause(String)}.
     */
    @SuppressWarnings("StatementWithEmptyBody")
    public static void addAclParameters( Query query, Class<? extends Securable> aoiType ) throws QueryParameterException {
        if ( SecuredChild.class.isAssignableFrom( aoiType ) ) {
            throw new IllegalArgumentException( "ACL filtering cannot be done on a SecuredChild; instead identify the owner and apply ACLs on it." );
        }
        if ( SecurityUtil.isUserAdmin() ) {
            // For administrators, no filtering is needed, so the ACE is completely skipped from
            // the where clause and no parameters are bound.
            return;
        }
        // HQL formAclRestrictionClause uses :aoiClassId (the resolved acl_class.id);
        // native formNativeAclRestrictionClause + loadAclInfoFor use :aoiType (the string).
        // Each call site exposes exactly one of the two parameter names, so bind both
        // conditionally rather than requiring the caller to know which form it built.
        String className = aoiType.getCanonicalName();
        setParameterIfPresent( query, AOI_TYPE_PARAM, className );
        if ( hasNamedParameter( query, AOI_CLASS_ID_PARAM ) ) {
            query.setParameter( AOI_CLASS_ID_PARAM, resolveAclClassId( className ) );
        }
        if ( SecurityUtil.isUserAnonymous() ) {
            // a constant is used directly in ANONYMOUS_SID_HQL/ANONYMOUS_SID_SQL, so no binding is necessary
        } else {
            query.setParameter( USER_NAME_PARAM, SecurityUtil.getCurrentUsername() );
        }
    }

    private static boolean hasNamedParameter( Query query, String name ) {
        return query.getParameterMetadata().getNamedParameterNames().contains( name );
    }

    private static void setParameterIfPresent( Query query, String name, Object value ) {
        if ( hasNamedParameter( query, name ) ) {
            query.setParameter( name, value );
        }
    }

    /**
     * Stashed by {@link AclClassIdInitializer} (in `gemma-core/src/main/java/.../AclClassIdInitializer.java`)
     * at Spring context init so the static {@link #resolveAclClassId} doesn't need a
     * Hibernate-internal unwrap on every query. One factory per JVM; thread-safe via final + volatile.
     */
    static volatile SessionFactoryImplementor sessionFactory;

    /**
     * Resolve {@code acl_class.id} for a Securable class name, caching the result for
     * the JVM lifetime. The acl_class table is insert-once on first registration of
     * each Securable subclass, so the id is stable.
     */
    private static Long resolveAclClassId( String className ) {
        Long cached = ACL_CLASS_ID_CACHE.get( className );
        if ( cached != null ) {
            return cached;
        }
        SessionFactoryImplementor sf = sessionFactory;
        if ( sf == null ) {
            throw new IllegalStateException( "AclQueryUtils.sessionFactory not set; AclClassIdInitializer must run before any ACL-filtered query." );
        }
        // Open a stateless session for the lookup so we don't piggyback on the
        // caller's Hibernate session (which may be mid-flush or carrying state
        // we don't want to interleave with).
        Long resolved;
        try ( org.hibernate.StatelessSession ss = sf.openStatelessSession() ) {
            // Don't pass a result class: Hibernate 6 requires it to be concrete with
            // a single-arg constructor (Number is abstract; Long has no Long(Number)
            // ctor). The MySQL bigint comes back as java.lang.Long anyway.
            Number id = (Number) ss.createNativeQuery(
                            "select id from acl_class where class = :c" )
                    .setParameter( "c", className )
                    .getSingleResult();
            resolved = id.longValue();
        }
        ACL_CLASS_ID_CACHE.put( className, resolved );
        return resolved;
    }
}
