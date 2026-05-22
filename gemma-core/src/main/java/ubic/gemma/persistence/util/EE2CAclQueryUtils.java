package ubic.gemma.persistence.util;

import ubic.gemma.core.security.util.SecurityUtil;
import org.hibernate.query.Query;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;
import ubic.gemma.model.common.auditAndSecurity.Securable;

/**
 * This class provides a fast-path to {@link AclQueryUtils} that uses the denormalized mask for anonymous users.
 * @author poirigui
 */
public class EE2CAclQueryUtils {

    /**
     * @deprecated as of HQL_SQL_AUDIT C5 the id column is passed explicitly to
     *             {@link #formNativeAclRestrictionClause(SessionFactoryImplementor, String, String, Permission)};
     *             this join shim now always returns the empty string. Drop the call.
     */
    @Deprecated
    public static String formNativeAclJoinClause( String aoiIdColumn ) {
        // The EE2C fast-path never depended on the ACL join clause (it short-circuited for
        // admin/anonymous and delegated to AclQueryUtils otherwise — which itself returned
        // empty post-EXISTS rewrite). Kept for API stability; safe to delete.
        return "";
    }

    public static String formNativeAclRestrictionClause( SessionFactoryImplementor sessionFactoryImplementor, String aoiIdColumn, String anonymousMaskColumn ) {
        return formNativeAclRestrictionClause( sessionFactoryImplementor, aoiIdColumn, anonymousMaskColumn, BasePermission.READ );
    }

    public static String formNativeAclRestrictionClause( SessionFactoryImplementor sessionFactoryImplementor, String aoiIdColumn, String anonymousMaskColumn, Permission permission ) {
        if ( SecurityUtil.isUserAnonymous() ) {
            // Dialect-aware bitwise AND (MySQL: "(a & b)", H2: "BITAND(a, b)").
            String renderedMask = BitwiseUtils.bitand( sessionFactoryImplementor.getJdbcServices().getDialect(),
                    anonymousMaskColumn, String.valueOf( permission.getMask() ) );
            return " and " + renderedMask + " <> 0";
        } else if ( SecurityUtil.isUserAdmin() ) {
            return "";
        } else {
            return AclQueryUtils.formNativeAclRestrictionClause( sessionFactoryImplementor, aoiIdColumn, permission );
        }
    }

    public static void addAclParameters( Query query, Class<? extends Securable> aoiType ) {
        if ( !SecurityUtil.isUserAdmin() && !SecurityUtil.isUserAnonymous() ) {
            AclQueryUtils.addAclParameters( query, aoiType );
        }
    }
}
