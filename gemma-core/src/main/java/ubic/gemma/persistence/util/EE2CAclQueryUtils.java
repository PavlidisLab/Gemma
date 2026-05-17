package ubic.gemma.persistence.util;

import gemma.gsec.util.SecurityUtil;
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

    public static String formNativeAclJoinClause( String aoiIdColumn ) {
        // ACLs are only necessary for regular, non-admin users
        if ( SecurityUtil.isUserAnonymous() || SecurityUtil.isUserAdmin() ) {
            return "";
        } else {
            return AclQueryUtils.formNativeAclJoinClause( aoiIdColumn );
        }
    }

    public static String formNativeAclRestrictionClause( SessionFactoryImplementor sessionFactoryImplementor, String anonymousMaskColumn ) {
        return formNativeAclRestrictionClause( sessionFactoryImplementor, anonymousMaskColumn, BasePermission.READ );
    }

    public static String formNativeAclRestrictionClause( SessionFactoryImplementor sessionFactoryImplementor, String anonymousMaskColumn, Permission permission ) {
        if ( SecurityUtil.isUserAnonymous() ) {
            // Dialect-aware bitwise AND (MySQL: "(a & b)", H2: "BITAND(a, b)").
            String renderedMask = BitwiseUtils.bitand( sessionFactoryImplementor.getJdbcServices().getDialect(),
                    anonymousMaskColumn, String.valueOf( permission.getMask() ) );
            return " and " + renderedMask + " <> 0";
        } else if ( SecurityUtil.isUserAdmin() ) {
            return "";
        } else {
            return AclQueryUtils.formNativeAclRestrictionClause( sessionFactoryImplementor, permission );
        }
    }

    public static void addAclParameters( Query query, Class<? extends Securable> aoiType ) {
        if ( !SecurityUtil.isUserAdmin() && !SecurityUtil.isUserAnonymous() ) {
            AclQueryUtils.addAclParameters( query, aoiType );
        }
    }
}
