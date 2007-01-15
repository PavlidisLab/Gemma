package ubic.gemma.security;

import org.acegisecurity.Authentication;
import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.acl.AclEntry;
import org.acegisecurity.acl.AclManager;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;

/**
 * @author keshav
 * @spring.bean name="securityService"
 * @spring.property name="aclManager" ref="aclManager"
 */
public class SecurityService {
    private Log log = LogFactory.getLog( SecurityService.class );

    private AclManager aclManager = null;

    public void makePrivate( Object object ) {

        if ( object instanceof ArrayDesign ) {
            ArrayDesign arrayDesign = ( ArrayDesign ) object;

            SecurityContext securityCtx = SecurityContextHolder.getContext();

            Authentication authentication = securityCtx.getAuthentication();
            GrantedAuthority[] grantedAuthorities = authentication.getAuthorities();
            for ( GrantedAuthority authority : grantedAuthorities ) {
                log.debug( "Authority: " + authority.getAuthority() );
            }

            AclEntry[] acls = aclManager.getAcls( arrayDesign );
            for ( AclEntry acl : acls ) {
                log.debug( "acl entry: " + acl );
                // I want to remove acl entries that are not administrator
            }

            // I need a setter to set the acls. AclManager does not have one.
        }

    }

    /**
     * @param aclManager the aclManager to set
     */
    public void setAclManager( AclManager aclManager ) {
        this.aclManager = aclManager;
    }

}
