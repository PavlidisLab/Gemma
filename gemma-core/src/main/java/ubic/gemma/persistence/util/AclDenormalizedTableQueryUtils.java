package ubic.gemma.persistence.util;

import gemma.gsec.util.SecurityUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.IntegerType;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.auditAndSecurity.SecuredChild;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class provides a fast-path to {@link AclQueryUtils} that uses denormalized permission masks for a specified
 * granted authority.
 * @author poirigui
 */
public class AclDenormalizedTableQueryUtils {

    /**
     * Form a native ACL join clause for the given AOI ID column and granted authorities.
     * <p>
     * The fast path is only taken if the user is an administrator or if it has at least one of the granted authorities
     * that are relevant to the query.
     *
     * @see AclQueryUtils#formNativeAclJoinClause(String)
     */
    public static String formNativeAclJoinClause( String aoiIdColumn, String grantedAuthority ) {
        Assert.isTrue( StringUtils.isNotBlank( aoiIdColumn ), "The ACL object identity column cannot be empty." );
        Assert.isTrue( StringUtils.isNotBlank( grantedAuthority ), "The granted authority cannot be empty." );
        if ( SecurityUtil.isUserAdmin() || CollectionUtils.containsAny( getGrantedAuthorities(), grantedAuthority ) ) {
            // nothing to do, we're in the fast path
            return "";
        } else {
            return AclQueryUtils.formNativeAclJoinClause( aoiIdColumn );
        }
    }

    /**
     * Form a native ACL restriction clause.
     * @param sessionFactoryImplementor           a session factory, used for generating queries
     * @param grantedAuthority                    a granted authority relevant to the query
     * @param grantedAuthorityPermissionMaskAlias the corresponding permission mask alias in the query
     * @param permission                          a requested permission
     */
    public static String formNativeAclRestrictionClause( SessionFactoryImplementor sessionFactoryImplementor, String grantedAuthority, String grantedAuthorityPermissionMaskAlias, Permission permission ) {
        Assert.isTrue( StringUtils.isNotBlank( grantedAuthority ), "The granted authority cannot be empty." );
        Assert.isTrue( StringUtils.isNotBlank( grantedAuthorityPermissionMaskAlias ), "The granted authority permission mask alias must not be empty." );
        Assert.isTrue( permission.getMask() > 0, "At least one bit must be set in the permission mask." );
        if ( SecurityUtil.isUserAdmin() ) {
            return "";
        } else {
            if ( !getGrantedAuthorities().contains( grantedAuthority ) ) {
                SQLFunction bitwiseAnd = sessionFactoryImplementor.getSqlFunctionRegistry().findSQLFunction( "bitwise_and" );
                // user has at least one relevant authority, use it to check permissions
                return " and " + "(" + bitwiseAnd.render( new IntegerType(), Arrays.asList( grantedAuthorityPermissionMaskAlias, permission.getMask() ), sessionFactoryImplementor ) + " <> 0)";
            } else {
                // user has no relevant authorities, default to the inefficient ACL check
                return AclQueryUtils.formNativeAclRestrictionClause( sessionFactoryImplementor, permission );
            }
        }
    }

    /**
     * Add ACL parameters to the query with an ACL clause for the given granted authorities by {@link #formNativeAclJoinClause(String, String)}.
     *
     * @param query   a query on which ACL parameters should be set
     * @param aoiType the type of secured object being queried
     * @see AclQueryUtils#setAclParameters(Query, Class)
     */
    public static void setAclParameters( Query query, String grantedAuthority, Class<? extends Securable> aoiType ) {
        Assert.isTrue( StringUtils.isNotBlank( grantedAuthority ), "The granted authority cannot be empty." );
        Assert.isTrue( !SecuredChild.class.isAssignableFrom( aoiType ),
                "ACL filtering cannot be done on a SecuredChild; instead identify the owner and apply ACLs on it." );
        //noinspection StatementWithEmptyBody
        if ( SecurityUtil.isUserAdmin() || getGrantedAuthorities().contains( grantedAuthority ) ) {
            // nothing to do, we're in the fast path
        } else {
            AclQueryUtils.setAclParameters( query, aoiType );
        }
    }

    /**
     * Obtain a set of granted authorities for the current user.
     */
    private static Set<String> getGrantedAuthorities() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .map( GrantedAuthority::getAuthority )
                .collect( Collectors.toSet() );
    }
}
