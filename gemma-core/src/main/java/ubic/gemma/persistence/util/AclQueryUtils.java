package ubic.gemma.persistence.util;

import gemma.gsec.model.Securable;
import gemma.gsec.util.SecurityUtil;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.QueryParameterException;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.IntegerType;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.util.Assert;

import java.util.Arrays;

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
 * @author poirigui
 */
public class AclQueryUtils {

    /**
     * Alias used by {@link #formAclRestrictionClause(String, int)} and {@link #formNativeAclJoinClause(String)} for the
     * object identity {@link gemma.gsec.acl.domain.AclObjectIdentity} and the owner identity {@link gemma.gsec.acl.domain.AclSid}.
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
     */
    //language=SQL
    static final String CURRENT_USER_SIDS_SQL =
            "select sid.ID from USER_GROUP as UG "
                    + "join GROUP_AUTHORITY GA on UG.ID = GA.GROUP_FK "
                    + "join ACLSID sid on sid.GRANTED_AUTHORITY = CONCAT('GROUP_', GA.AUTHORITY) "
                    + "join GROUP_MEMBERS GM on UG.ID = GM.USER_GROUPS_FK "
                    + "join CONTACT C on GM.GROUP_MEMBERS_FK = C.ID "
                    + "where C.USER_NAME = :" + USER_NAME_PARAM;

    //language=SQL
    static final String ANONYMOUS_SID_SQL = "select sid.ID from ACLSID sid where sid.GRANTED_AUTHORITY = 'IS_AUTHENTICATED_ANONYMOUSLY'";

    /**
     * Create a HQL restriction clause with the {@link BasePermission#READ} permission.
     * @see #formAclRestrictionClause(String, int)
     */
    public static String formAclRestrictionClause( String aoiIdColumn ) {
        return formAclRestrictionClause( aoiIdColumn, BasePermission.READ.getMask() );
    }

    /**
     * Create an HQL join clause for {@link gemma.gsec.acl.domain.AclObjectIdentity}, {@link gemma.gsec.acl.domain.AclGrantedAuthoritySid}
     * and a restriction clause to limit the result only to objects the current user can access.
     * <p>
     * Ensure that you use {@link #addAclParameters(Query, Class)} afterward to bind the query parameters.
     * <p>
     * <b>Important note:</b> when using this, ensure that you have a {@code group by} clause in your query, otherwise
     * entities with multiple ACL entries will be duplicated in the results.
     * <p>
     * FIXME: this ACL jointure is really annoying because it is one-to-many, maybe handling everything in a sub-query
     *        would be preferable?
     *
     * @param aoiIdColumn column name to match against the ACL object identity, the object class is passed via
     *                    {@link #addAclParameters(Query, Class)} afterward
     * @param mask        a mask with requested permissions
     * @return clause to add to the query after any jointure
     */
    public static String formAclRestrictionClause( String aoiIdColumn, int mask ) {
        if ( StringUtils.isBlank( aoiIdColumn ) ) {
            throw new IllegalArgumentException( "Object identity column cannot be empty." );
        }
        Assert.isTrue( mask > 0, "The mask must have at least one bit set." );
        //language=HQL
        String q = ", AclObjectIdentity as " + AOI_ALIAS + " join " + AOI_ALIAS + ".ownerSid " + SID_ALIAS;
        // for non-admin, we have to include aoi.entries
        // if aoi.entries is empty, the user might still be the owner, so we use a left join
        if ( !SecurityUtil.isUserAdmin() ) {
            q += " left join " + AOI_ALIAS + ".entries " + ACE_ALIAS;
        }
        q += " where (" + AOI_ALIAS + ".identifier = " + aoiIdColumn + " and " + AOI_ALIAS + ".type = :" + AOI_TYPE_PARAM + ")";
        // add ACL restrictions
        if ( !SecurityUtil.isUserAdmin() ) {
            if ( SecurityUtil.isUserAnonymous() ) {
                //language=HQL
                q += " and (bitwise_and(" + ACE_ALIAS + ".mask, " + mask + ") <> 0 and " + ACE_ALIAS + ".sid in (" + ANONYMOUS_SID_HQL + "))";
            } else {
                q += " and ("
                        // user own the object
                        + SID_ALIAS + ".principal = :" + USER_NAME_PARAM + " "
                        // specific rights to the object
                        + "or (" + ACE_ALIAS + ".sid in (" + CURRENT_USER_SIDS_HQL + ") and bitwise_and(" + ACE_ALIAS + ".mask, " + mask + ") <> 0) "
                        // publicly available
                        + "or (" + ACE_ALIAS + ".sid in (" + ANONYMOUS_SID_HQL + ") and bitwise_and(" + ACE_ALIAS + ".mask, " + mask + ") <> 0)"
                        + ")";
            }
        }
        return q;
    }

    /**
     * Native SQL flavour of the ACL jointure.
     * <p>
     * Note: unlike the HQL version, this query uses {@code on} to restrict the jointure, so you can define the
     * {@code where} clause yourself.
     * <p>
     * <b>Important note:</b> when using this, ensure that you have a {@code group by} clause in your query, otherwise
     * entities with multiple ACL entries will be duplicated in the results.
     * @param aoiIdColumn column name to match against the ACL object identity, the object class is passed via
     *                    {@link #addAclParameters(Query, Class)} afterward
     *
     * @see #formAclRestrictionClause(String)
     */
    public static String formNativeAclJoinClause( String aoiIdColumn ) {
        if ( StringUtils.isBlank( aoiIdColumn ) ) {
            throw new IllegalArgumentException( "Object identity column cannot be empty." );
        }
        //language=SQL
        String q = " join ACLOBJECTIDENTITY " + AOI_ALIAS + " on (" + AOI_ALIAS + ".OBJECT_CLASS = :" + AOI_TYPE_PARAM + " and " + AOI_ALIAS + ".OBJECT_ID = " + aoiIdColumn + ") "
                + "join ACLSID " + SID_ALIAS + " on (" + SID_ALIAS + ".ID = " + AOI_ALIAS + ".OWNER_SID_FK)";

        // for non-admin, we have to include aoi.entries
        // if aoi.entries is empty, the user might still be the owner, so we use a left join
        if ( !SecurityUtil.isUserAdmin() ) {
            q += " left join ACLENTRY " + ACE_ALIAS + " on (" + AOI_ALIAS + ".ID = " + ACE_ALIAS + ".OBJECTIDENTITY_FK)";
        }
        return q;
    }

    /**
     * Native flavour of the ACL restriction clause with a {@link BasePermission#READ} permission.
     * @see #formNativeAclRestrictionClause(SessionFactoryImplementor, int)
     */
    public static String formNativeAclRestrictionClause( SessionFactoryImplementor sessionFactoryImplementor ) {
        return formNativeAclRestrictionClause( sessionFactoryImplementor, BasePermission.READ.getMask() );
    }

    /**
     * Native flavour of the ACL restriction clause.
     * @param sessionFactoryImplementor a session factory implementor that will be used to adjust the SQL generated
     *                                  based on the dialect
     * @param mask                      a mask with requested permissions
     * @see #formAclRestrictionClause(String, int)
     */
    public static String formNativeAclRestrictionClause( SessionFactoryImplementor sessionFactoryImplementor, int mask ) {
        SQLFunction bitwiseAnd = sessionFactoryImplementor.getSqlFunctionRegistry().findSQLFunction( "bitwise_and" );
        String renderedMask = bitwiseAnd.render( new IntegerType(), Arrays.asList( ACE_ALIAS + ".MASK", mask ), sessionFactoryImplementor );
        //language=SQL
        if ( SecurityUtil.isUserAnonymous() ) {
            return " and (" + renderedMask + " <> 0 and " + ACE_ALIAS + ".SID_FK in (" + ANONYMOUS_SID_SQL + "))";
        } else if ( !SecurityUtil.isUserAdmin() ) {
            return " and ("
                    // user owns the object
                    + SID_ALIAS + ".PRINCIPAL = :" + USER_NAME_PARAM + " "
                    // specific rights to the object
                    + "or (" + ACE_ALIAS + ".SID_FK in (" + CURRENT_USER_SIDS_SQL + ") and " + renderedMask + " <> 0) "
                    // publicly available
                    + "or (" + ACE_ALIAS + ".SID_FK in (" + ANONYMOUS_SID_SQL + ") and " + renderedMask + " <> 0)"
                    + ")";
        } else {
            // For administrators, no filtering is needed, so the ACE is completely skipped from the where clause.
            return "";
        }
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
        query.setParameter( AOI_TYPE_PARAM, aoiType.getCanonicalName() );
        if ( SecurityUtil.isUserAnonymous() ) {
            // a constant is used directly in ANONYMOUS_SID_SQL, so no binding is necessary
        } else if ( !SecurityUtil.isUserAdmin() ) {
            query.setParameter( USER_NAME_PARAM, SecurityUtil.getCurrentUsername() );
        } else {
            // For administrators, no filtering is needed, so the ACE is completely skipped from the where clause.
        }
    }
}
