package ubic.gemma.persistence.util;

import gemma.gsec.model.Securable;
import gemma.gsec.util.SecurityUtil;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.QueryParameterException;
import org.springframework.security.acls.domain.BasePermission;

/**
 * Utilities for integrating ACL into {@link Query}.
 * <p>
 * To build a query, sequentially proceed as follows:
 * <ol>
 * <li>form your select clause and your jointures</li>
 * <li>concatenate {@link #formAclJoinClause(String)} or {@link #formNativeAclJoinClause(String)} in the jointure section</li>
 * <li>form where clause and add your constraints</li>
 * <li>concatenate {@link #formAclRestrictionClause()} or {@link #formNativeAclRestrictionClause()} in the clause section</li>
 * <li>bind all your parameters</li>
 * <li>apply {@link #addAclParameters(Query, Class)} to the query object</li>
 * </ol>
 * @author poirigui
 */
public class AclQueryUtils {

    /**
     * Alias used.
     */
    public static final String
            AOI_ALIAS = "aoi",
            SID_ALIAS = "sid",
            ACE_ALIAS = "ace";

    /**
     * Parameter name prefix to avoid clashes with user-defined parameters.
     */
    private static final String PARAM_PREFIX = "aclQueryUtils_";
    private static final String
            AOI_TYPE_PARAM = PARAM_PREFIX + "aoiType";
    private static final String READ_MASK_PARAM = PARAM_PREFIX + "readMask";
    private static final String WRITE_MASK_PARAM = PARAM_PREFIX + "writeMask";
    static final String USER_NAME_PARAM = PARAM_PREFIX + "userName";

    /**
     * Select all the SIDs that belong to a given user (specified by a :userName parameter).
     */
    //language=HQL
    static final String CURRENT_USER_SIDS_HQL =
            "select sid from UserGroup as ug join ug.authorities as ga, AclGrantedAuthoritySid sid "
                    + "where sid.grantedAuthority = CONCAT('GROUP_', ga.authority) "
                    + "and ug.name in (select ug.name from UserGroup ug join ug.groupMembers memb where memb.userName = :" + USER_NAME_PARAM + ")";

    //language=HQL
    static final String ANONYMOUS_SID_HQL = "select sid from AclGrantedAuthoritySid sid where sid.grantedAuthority = 'IS_AUTHENTICATED_ANONYMOUSLY'";

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
     * Create an HQL join clause for {@link gemma.gsec.acl.domain.AclObjectIdentity}, {@link gemma.gsec.acl.domain.AclGrantedAuthoritySid}.
     * <p>
     * Ensure that you use {@link #addAclParameters(Query, Class)} afterward to bind the query parameters.
     * <p>
     * FIXME: this ACL jointure is really annoying because it is one-to-many, maybe handling everything in a sub-query
     * would be preferable?
     * <p>
     * Note: the returned clause contains a {@code where} clause.
     *
     * @param aoiIdColumn column for the identifier e.g. "ee.id"
     * @return clause to add to the query
     */
    public static String formAclJoinClause( String aoiIdColumn ) {
        if ( StringUtils.isBlank( aoiIdColumn ) ) {
            throw new IllegalArgumentException( "Object identity column cannot be empty." );
        }
        //language=HQL
        String q = ", AclObjectIdentity as " + AOI_ALIAS + " join " + AOI_ALIAS + ".ownerSid " + SID_ALIAS;
        // for non-admin, we have to include aoi.entries
        if ( !SecurityUtil.isUserAdmin() ) {
            q += " join " + AOI_ALIAS + ".entries " + ACE_ALIAS;
        }
        //language=HQL
        q += " where (" + AOI_ALIAS + ".identifier = " + aoiIdColumn + " and " + AOI_ALIAS + ".type = :" + AOI_TYPE_PARAM + ")";
        return q;
    }

    /**
     * Native SQL flavour of the ACL jointure.
     * <p>
     * Note: unlike the HQL version, this query uses {@code on} to restrict the jointure, so you can define the
     * {@code where} clause yourself.
     * @see #formAclJoinClause(String)
     */
    public static String formNativeAclJoinClause( String aoiIdColumn ) {
        if ( StringUtils.isBlank( aoiIdColumn ) ) {
            throw new IllegalArgumentException( "Object identity column cannot be empty." );
        }
        //language=SQL
        String q = " left join ACLOBJECTIDENTITY " + AOI_ALIAS + " on (" + AOI_ALIAS + ".OBJECT_CLASS = :" + AOI_TYPE_PARAM + " and " + AOI_ALIAS + ".OBJECT_ID = " + aoiIdColumn + ") "
                + "left join ACLSID " + SID_ALIAS + " on (" + SID_ALIAS + ".ID = " + AOI_ALIAS + ".OWNER_SID_FK)";

        // for non-admin, we have to include aoi.entries
        if ( !SecurityUtil.isUserAdmin() ) {
            q += " join ACLENTRY " + ACE_ALIAS + " on (" + AOI_ALIAS + ".ID = " + ACE_ALIAS + ".OBJECTIDENTITY_FK)";
        }
        return q;
    }

    /**
     * Creates a restriction clause to limit the result only to objects the currently logged user can access.
     * <p>
     * If you use this, you must also bind its parameters with {@link #addAclParameters(Query, Class)}.
     * @return a string that can be appended to a query string that was created using {@link #formAclJoinClause(String)}.
     */
    public static String formAclRestrictionClause() {
        // add ACL restrictions
        if ( SecurityUtil.isUserAnonymous() ) {
            // For anonymous users, only pick publicly readable data
            //language=HQL
            return " and (" + ACE_ALIAS + ".mask = :" + READ_MASK_PARAM + " and " + ACE_ALIAS + ".sid in (" + ANONYMOUS_SID_HQL + "))";
        } else if ( !SecurityUtil.isUserAdmin() ) {
            // For non-admin users, pick non-troubled, publicly readable data and data that are readable by them or a group they belong to
            //language=HQL
            return " and ("
                    // user own the object
                    + SID_ALIAS + ".principal = :" + USER_NAME_PARAM + " "
                    // specific rights to the object
                    + "or (" + ACE_ALIAS + ".sid in (" + CURRENT_USER_SIDS_HQL + ") and (" + ACE_ALIAS + ".mask = :" + READ_MASK_PARAM + " or " + ACE_ALIAS + ".mask = :" + WRITE_MASK_PARAM + ")) "
                    // publicly available
                    + "or (" + ACE_ALIAS + ".sid in (" + ANONYMOUS_SID_HQL + ") and " + ACE_ALIAS + ".mask = :" + READ_MASK_PARAM + ")"
                    + ")";
        } else {
            // For administrators, no filtering is needed, so the ACE is completely skipped from the where clause.
            return " ";
        }
    }

    /**
     * Native flavour of the ACL restriction clause.
     * @see #formAclRestrictionClause()
     */
    public static String formNativeAclRestrictionClause() {
        //language=SQL
        if ( SecurityUtil.isUserAnonymous() ) {
            return " and (" + ACE_ALIAS + ".MASK = :" + READ_MASK_PARAM + " and " + ACE_ALIAS + ".SID_FK in (" + ANONYMOUS_SID_SQL + "))";
        } else if ( !SecurityUtil.isUserAdmin() ) {
            return " and ("
                    + SID_ALIAS + ".PRINCIPAL = :" + USER_NAME_PARAM + " "
                    + "or (" + ACE_ALIAS + ".SID_FK in (" + CURRENT_USER_SIDS_SQL + ") and (" + ACE_ALIAS + ".MASK = :" + READ_MASK_PARAM + " or " + ACE_ALIAS + ".MASK = :" + WRITE_MASK_PARAM + ")) "
                    + "or (" + ACE_ALIAS + ".SID_FK in (" + ANONYMOUS_SID_SQL + ") and " + ACE_ALIAS + ".MASK = :" + READ_MASK_PARAM + ")"
                    + ")";
        } else {
            // For administrators, no filtering is needed, so the ACE is completely skipped from the where clause.
            return "";
        }
    }

    /**
     * Bind {@link Query} parameters to a join clause generated with {@link #formAclJoinClause(String)} and add ACL
     * restriction parameters defined in {@link #formAclRestrictionClause()}.
     * <p>
     * This method also work for native queries formed with {@link #formNativeAclJoinClause(String)} and
     * {@link #formNativeAclRestrictionClause()}.
     *
     * @param query   a {@link Query} object that contains the join and restriction clauses
     * @param aoiType the AOI type to be bound in the query
     * @throws QueryParameterException if any defined parameters are missing, which is typically due to a missing prior
     * {@link #formAclRestrictionClause()}.
     */
    public static void addAclParameters( Query query, Class<? extends Securable> aoiType ) throws QueryParameterException {
        query.setParameter( AOI_TYPE_PARAM, aoiType.getCanonicalName() );
        if ( SecurityUtil.isUserAnonymous() ) {
            // sid 4 = IS_AUTHENTICATED_ANONYMOUSLY
            query.setParameter( READ_MASK_PARAM, BasePermission.READ.getMask() );
        } else if ( !SecurityUtil.isUserAdmin() ) {
            query.setParameter( USER_NAME_PARAM, SecurityUtil.getCurrentUsername() );
            query.setParameter( READ_MASK_PARAM, BasePermission.READ.getMask() );
            query.setParameter( WRITE_MASK_PARAM, BasePermission.WRITE.getMask() );
        }
        // For administrators, no filtering is needed, so the ACE is completely skipped from the where clause.
    }
}
