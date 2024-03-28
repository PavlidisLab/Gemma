package ubic.gemma.persistence.util;

import gemma.gsec.model.Securable;
import gemma.gsec.util.SecurityUtil;
import org.hibernate.Query;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.IntegerType;
import org.springframework.security.acls.domain.BasePermission;

import java.util.Arrays;

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
        return formNativeAclRestrictionClause( sessionFactoryImplementor, anonymousMaskColumn, BasePermission.READ.getMask() );
    }

    public static String formNativeAclRestrictionClause( SessionFactoryImplementor sessionFactoryImplementor, String anonymousMaskColumn, int mask ) {
        if ( SecurityUtil.isUserAnonymous() ) {
            SQLFunction bitwiseAnd = sessionFactoryImplementor.getSqlFunctionRegistry().findSQLFunction( "bitwise_and" );
            String renderedMask = bitwiseAnd.render( new IntegerType(), Arrays.asList( anonymousMaskColumn, mask ), sessionFactoryImplementor );
            return " and " + renderedMask + " <> 0";
        } else if ( SecurityUtil.isUserAdmin() ) {
            return "";
        } else {
            return AclQueryUtils.formNativeAclRestrictionClause( sessionFactoryImplementor, mask );
        }
    }

    public static void addAclParameters( Query query, Class<? extends Securable> aoiType ) {
        if ( !SecurityUtil.isUserAdmin() && !SecurityUtil.isUserAnonymous() ) {
            AclQueryUtils.addAclParameters( query, aoiType );
        }
    }
}
