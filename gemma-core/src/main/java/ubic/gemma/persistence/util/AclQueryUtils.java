package ubic.gemma.persistence.util;

import gemma.gsec.model.Securable;
import gemma.gsec.util.SecurityUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.Query;
import org.hibernate.QueryParameterException;
import org.springframework.security.acls.domain.BasePermission;

import javax.annotation.Nullable;

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
            AOI_TYPE_PARAM = PARAM_PREFIX + "aoiType",
            READ_MASK_PARAM = PARAM_PREFIX + "readMask",
            WRITE_MASK_PARAM = PARAM_PREFIX + "writeMask",
            USER_NAME_PARAM = PARAM_PREFIX + "userName",
            ANONYMOUS_AUTH_SID_PARAM = PARAM_PREFIX + "anonymousAuthSid";

    /**
     * Select all the SIDs that belong to a given user (specified by a :userName parameter).
     */
    //language=HQL
    static final String CURRENT_USER_SIDS_HQL =
            "select sid.id from UserGroup as ug join ug.authorities as ga, AclGrantedAuthoritySid sid "
                    + "where sid.grantedAuthority = CONCAT('GROUP_', ga.authority) "
                    + "and ug.name in (select ug.name from UserGroup ug join ug.groupMembers memb where memb.userName = :" + USER_NAME_PARAM + ")";

    /**
     * Native SQL version of {@link #CURRENT_USER_SIDS_HQL}.
     */
    //language=SQL
    private static final String CURRENT_USER_SIDS_SQL =
            "select sid.id from USER_GROUP as UG "
                    + "join GROUP_AUTHORITY GA on ug.ID = GA.GROUP_FK "
                    + "join ACLSID sid on sid.GRANTED_AUTHORITY = CONCAT('GROUP_', GA.AUTHORITY) "
                    + "join GROUP_MEMBERS GM on UG.ID = GM.USER_GROUPS_FK "
                    + "join CONTACT C on GM.GROUP_MEMBERS_FK = C.ID "
                    + "where C.USER_NAME = :" + USER_NAME_PARAM;

    /**
     * Create an HQL join clause from ACL OI -{@literal >} ACL entries and ACL OI -{@literal >} ACL SID.
     * <p>
     * Ensure that you use {@link #addAclParameters(Query, Class)} afterward to bind the query parameters.
     * <p>
     * FIXME: this ACL jointure is really annoying because it is one-to-many, maybe handling everything in a sub-query
     * would be preferable?
     *
     * @param alias   placeholder for the identifier e.g. "ee.id"
     * @return clause to add to the query
     */
    public static String formAclJoinClause( @Nullable String alias ) {
        //language=HQL
        String q = ", AclObjectIdentity as " + AOI_ALIAS + " join " + AOI_ALIAS + ".ownerSid " + SID_ALIAS;
        // for non-admin, we have to include aoi.entries
        if ( !SecurityUtil.isUserAdmin() ) {
            q += " join " + AOI_ALIAS + ".entries " + ACE_ALIAS;
        }
        //language=HQL
        q += " where (" + AOI_ALIAS + ".identifier = " + FilterQueryUtils.formPropertyName( alias, "id" ) + " and " + AOI_ALIAS + ".type = :" + AOI_TYPE_PARAM +
                ")";
        return q;
    }

    /**
     * Form an ACL jointure clause suitable for a native SQL query.
     * @param aoiIdColumn column refeerring to the object identity
     */
    public static String formNativeAclJoinClause( String aoiIdColumn ) {
        //language=SQL
        String q = " join ACLOBJECTIDENTITY " + AOI_ALIAS + " on (" + AOI_ALIAS + ".OBJECT_CLASS = :" + AOI_TYPE_PARAM + " and " + AOI_ALIAS + ".OBJECT_ID = " + aoiIdColumn + ")";
        if ( !SecurityUtil.isUserAdmin() ) {
            q += " join ACLENTRY " + ACE_ALIAS + " on (" + AOI_ALIAS + ".ID = " + ACE_ALIAS + ".OBJECTIDENTITY_FK)";
        }
        return q;
    }

    /**
     * Creates a restriction clause to limit the result only to objects the currently logged user can access.
     * Do not forget to populate the :userName parameter for non-admin logged users before using the string
     * to create a Query object.
     * <p>
     * If you use this, you must also bind its parameters with {@link #addAclParameters(Query, Class)}.
     *
     * @return a string that can be appended to a query string that was created using {@link #formAclJoinClause(String)}.
     */
    public static String formAclRestrictionClause() {
        // add ACL restrictions
        if ( SecurityUtil.isUserAnonymous() ) {
            // For anonymous users, only pick publicly readable data
            //language=HQL
            return " and (" + ACE_ALIAS + ".mask = :" + READ_MASK_PARAM + " and " + ACE_ALIAS + ".sid.id = :" + ANONYMOUS_AUTH_SID_PARAM + ")"; // sid 4 = IS_AUTHENTICATED_ANONYMOUSLY
        } else if ( !SecurityUtil.isUserAdmin() ) {
            // For non-admin users, pick non-troubled, publicly readable data and data that are readable by them or a group they belong to
            //language=HQL
            return " and ("
                    // user own the object
                    + SID_ALIAS + ".principal = :" + USER_NAME_PARAM + " "
                    // specific rights to the object
                    + "or (" + ACE_ALIAS + ".sid.id in (" + CURRENT_USER_SIDS_HQL + ") and (" + ACE_ALIAS + ".mask = :" + READ_MASK_PARAM + " or " + ACE_ALIAS + ".mask = :" + WRITE_MASK_PARAM + ")) "
                    // publicly available
                    + "or (" + ACE_ALIAS + ".sid.id = :" + ANONYMOUS_AUTH_SID_PARAM + " and " + ACE_ALIAS + ".mask = :" + READ_MASK_PARAM + ")"
                    + ")";
        } else {
            // For administrators, no filtering is needed, so the ACE is completely skipped from the where clause.
            return " ";
        }
    }

    public static String formNativeAclRestrictionClause() {
        //language=SQL
        if ( SecurityUtil.isUserAnonymous() ) {
            return " and (" + ACE_ALIAS + ".MASK = :" + READ_MASK_PARAM + " and " + ACE_ALIAS + ".SID_FK = :" + ANONYMOUS_AUTH_SID_PARAM + ")";
        } else if ( !SecurityUtil.isUserAdmin() ) {
            return " and ("
                    + SID_ALIAS + ".PRINCIPAL = :" + USER_NAME_PARAM + " "
                    + "or (" + ACE_ALIAS + ".SID_FK in (" + CURRENT_USER_SIDS_SQL + ") and (" + ACE_ALIAS + ".MASK = :" + READ_MASK_PARAM + " or " + ACE_ALIAS + ".MASK = :" + WRITE_MASK_PARAM + ")) "
                    + "or (" + ACE_ALIAS + ".SID_FK = :" + ANONYMOUS_AUTH_SID_PARAM + " and " + ACE_ALIAS + ".MASK = :" + READ_MASK_PARAM + ")"
                    + ")";
        } else {
            return "";
        }
    }

    /**
     * Bind {@link Query} parameters to a join clause generated with {@link #formAclJoinClause(String)} and add ACL
     * restriction parameters defined in {@link #formAclRestrictionClause()}.
     *
     * @param query   a {@link Query} object that contains the join clause
     * @param aoiType the AOI type to be bound in the query
     * @throws QueryParameterException if any defined parameters are missing, which is typically due to a missing {@link #formAclRestrictionClause()}.
     */
    public static void addAclParameters( Query query, Class<? extends Securable> aoiType ) throws QueryParameterException {
        query.setParameter( AOI_TYPE_PARAM, aoiType.getCanonicalName() );
        if ( SecurityUtil.isUserAnonymous() ) {
            query.setParameter( ANONYMOUS_AUTH_SID_PARAM, 4L );
            query.setParameter( READ_MASK_PARAM, BasePermission.READ.getMask() );
        } else if ( !SecurityUtil.isUserAdmin() ) {
            query.setParameter( USER_NAME_PARAM, SecurityUtil.getCurrentUsername() );
            query.setParameter( ANONYMOUS_AUTH_SID_PARAM, 4L );
            query.setParameter( READ_MASK_PARAM, BasePermission.READ.getMask() );
            query.setParameter( WRITE_MASK_PARAM, BasePermission.WRITE.getMask() );
        }
        // For administrators, no filtering is needed, so the ACE is completely skipped from the where clause.
    }
}
