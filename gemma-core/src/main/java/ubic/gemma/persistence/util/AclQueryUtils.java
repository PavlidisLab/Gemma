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

/**
 * Utilities for integrating ACL into {@link Query}.
 * <p>
 * To build a query, sequentially proceed as follows:
 * <ol>
 * <li>form your select clause and your jointures</li>
 * <li>concatenate {@link #formAclRestrictionClause(String)} or {@link #formNativeAclJoinClause(String)} in the jointure section</li>
 * <li>form where clause and add your constraints</li>
 * <li>concatenate {@link #formNativeAclRestrictionClause(SessionFactoryImplementor)} in the clause section (only for native queries)</li>
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
 * @author poirigui
 */
public class AclQueryUtils {

    /**
     * Alias used by {@link #formNativeAclJoinClause(String)} for the
     * object identity {@link ubic.gemma.core.security.acl.domain.AclObjectIdentity} and the owner identity {@link ubic.gemma.core.security.acl.domain.AclSid}.
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
            AOI_TYPE_PARAM = PARAM_PREFIX + "aoiType";
    static final String USER_NAME_PARAM = PARAM_PREFIX + "userName";

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
        //   where aoi.identifier = <aoiIdCol> and aoi.type = :aoiType
        //         and (<acl predicates>)
        //language=HQL
        StringBuilder exists = new StringBuilder( 256 );
        exists.append( "exists (select 1 from AclObjectIdentity " ).append( AOI_ALIAS )
                .append( " join " ).append( AOI_ALIAS ).append( ".ownerSid " ).append( SID_ALIAS );

        if ( SecurityUtil.isUserAnonymous() ) {
            // Anonymous: only the ACE check is in play; SID isn't used in the predicate but
            // joining keeps the shape consistent with the authenticated branch.
            exists.append( " join " ).append( AOI_ALIAS ).append( ".entries " ).append( ACE_ALIAS )
                    .append( " where " ).append( AOI_ALIAS ).append( ".identifier = " ).append( aoiIdColumn )
                    .append( " and " ).append( AOI_ALIAS ).append( ".type = :" ).append( AOI_TYPE_PARAM )
                    .append( " and bitand(" ).append( ACE_ALIAS ).append( ".mask, " ).append( permission.getMask() )
                    .append( ") <> 0 and " ).append( ACE_ALIAS ).append( ".sid in (" ).append( ANONYMOUS_SID_HQL ).append( "))" );
        } else {
            // Authenticated non-admin: owner-or-grant predicate. The original code left-joined
            // aoi.entries so that owner-without-ACE rows still matched; we model the disjunction
            // explicitly with two EXISTS variants OR'd together.
            exists.append( " left join " ).append( AOI_ALIAS ).append( ".entries " ).append( ACE_ALIAS )
                    .append( " where " ).append( AOI_ALIAS ).append( ".identifier = " ).append( aoiIdColumn )
                    .append( " and " ).append( AOI_ALIAS ).append( ".type = :" ).append( AOI_TYPE_PARAM )
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
        @SuppressWarnings("unchecked")
        java.util.List<Object[]> rows = session
                .createQuery( "select aoi.identifier, aoi, aoi.ownerSid from AclObjectIdentity aoi "
                        + "where aoi.identifier in :ids and aoi.type = :aoiType" )
                .setParameterList( "ids", ids )
                .setParameter( "aoiType", aoiType.getCanonicalName() )
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
     * Native SQL flavour of the ACL jointure.
     * <p>
     * After the EXISTS rewrite, this method returns the empty string: the native restriction
     * clause produced by {@link #formNativeAclRestrictionClause(SessionFactoryImplementor, Permission)}
     * now emits a self-contained correlated {@code EXISTS} sub-query that needs no outer
     * jointure. Kept for API stability so that callers that string-concatenate
     * "{@code from X x } + formNativeAclJoinClause(...) + ..." continue to compile.
     *
     * @param aoiIdColumn column name to match against the ACL object identity, the object class is passed via
     *                    {@link #addAclParameters(Query, Class)} afterward
     *
     * @see #formAclRestrictionClause(String)
     */
    public static String formNativeAclJoinClause( String aoiIdColumn ) {
        if ( StringUtils.isBlank( aoiIdColumn ) ) {
            throw new IllegalArgumentException( "Object identity column cannot be empty." );
        }
        // The EXISTS sub-query in formNativeAclRestrictionClause carries the aoi-id correlation
        // back to the outer query via :aclQueryUtils_aoiIdCol, so we store the caller's column
        // name on a thread-local that the restriction clause reads.
        NATIVE_AOI_ID_COLUMN.set( aoiIdColumn );
        return "";
    }

    /**
     * Thread-local hand-off between {@link #formNativeAclJoinClause(String)} (which records the
     * caller's id column) and {@link #formNativeAclRestrictionClause(SessionFactoryImplementor, Permission)}
     * (which embeds it into the EXISTS sub-query body). The contract is: callers MUST invoke
     * the join clause before the restriction clause in the same call chain. All in-tree
     * callers follow this pattern.
     */
    private static final ThreadLocal<String> NATIVE_AOI_ID_COLUMN = new ThreadLocal<>();

    /**
     * Native flavour of the ACL restriction clause with a {@link BasePermission#READ} permission.
     * @see #formNativeAclRestrictionClause(SessionFactoryImplementor, Permission)
     */
    public static String formNativeAclRestrictionClause( SessionFactoryImplementor sessionFactoryImplementor ) {
        return formNativeAclRestrictionClause( sessionFactoryImplementor, BasePermission.READ );
    }

    /**
     * Native flavour of the ACL restriction clause.
     * <p>
     * After the EXISTS rewrite, this returns a self-contained {@code " and exists (...)"} clause
     * that correlates back to the outer query via the {@code aoiIdColumn} that
     * {@link #formNativeAclJoinClause(String)} stashed on a thread-local during the same call
     * chain.
     *
     * @param sessionFactoryImplementor a session factory implementor that will be used to adjust the SQL generated
     *                                  based on the dialect
     * @param permission                requested permission(s)
     * @see #formAclRestrictionClause(String, Permission)
     */
    public static String formNativeAclRestrictionClause( SessionFactoryImplementor sessionFactoryImplementor, Permission permission ) {
        String aoiIdColumn = NATIVE_AOI_ID_COLUMN.get();
        NATIVE_AOI_ID_COLUMN.remove();
        if ( aoiIdColumn == null ) {
            throw new IllegalStateException(
                    "formNativeAclRestrictionClause was called without a prior formNativeAclJoinClause; "
                            + "the EXISTS rewrite needs the outer-query id column to correlate." );
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
     * This method also work for native queries formed with {@link #formNativeAclJoinClause(String)} and
     * {@link #formNativeAclRestrictionClause(SessionFactoryImplementor)}.
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
        query.setParameter( AOI_TYPE_PARAM, aoiType.getCanonicalName() );
        if ( SecurityUtil.isUserAnonymous() ) {
            // a constant is used directly in ANONYMOUS_SID_HQL/ANONYMOUS_SID_SQL, so no binding is necessary
        } else {
            query.setParameter( USER_NAME_PARAM, SecurityUtil.getCurrentUsername() );
        }
    }
}
