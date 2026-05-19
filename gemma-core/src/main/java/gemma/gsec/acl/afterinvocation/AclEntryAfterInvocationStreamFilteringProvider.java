package gemma.gsec.acl.afterinvocation;

import gemma.gsec.acl.domain.AclService;
import org.hibernate.Session;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.acls.afterinvocation.AbstractAclProvider;
import org.springframework.security.acls.model.*;
import org.springframework.security.core.Authentication;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * Filter domain objects from a {@link Stream}.
 *
 * @author poirigui
 */
public class AclEntryAfterInvocationStreamFilteringProvider extends AbstractAclProvider {

    private final AclService aclService;

    public AclEntryAfterInvocationStreamFilteringProvider( AclService aclService, List<Permission> requirePermission ) {
        super( aclService, "AFTER_ACL_STREAM_READ", requirePermission );
        this.aclService = aclService;
    }

    @Override
    public Object decide( Authentication authentication, Object object, Collection<ConfigAttribute> config, Object returnedObject ) throws AccessDeniedException {
        for ( ConfigAttribute configAttribute : config ) {
            if ( !supports( configAttribute ) ) {
                continue;
            }
            if ( returnedObject instanceof Stream ) {
                Session session = aclService.openSession();
                List<Sid> sids = sidRetrievalStrategy.getSids( authentication );
                return ( ( Stream<?> ) returnedObject )
                    .filter( getProcessDomainObjectClass()::isInstance )
                    .filter( s -> hasPermission( s, sids, session ) )
                    .onClose( session::close );
            } else {
                throw new AuthorizationServiceException( "Expected a return type of Stream, but got " + returnedObject + "." );
            }
        }
        return returnedObject;
    }

    private boolean hasPermission( Object domainObject, List<Sid> sids, Session session ) {
        ObjectIdentity objectIdentity = objectIdentityRetrievalStrategy.getObjectIdentity( domainObject );
        try {
            // Lookup only ACLs for SIDs we're interested in
            Acl acl = aclService.readAclById( objectIdentity, session );
            return acl.isGranted( requirePermission, sids, false );
        } catch ( NotFoundException ignore ) {
            return false;
        }
    }
}
