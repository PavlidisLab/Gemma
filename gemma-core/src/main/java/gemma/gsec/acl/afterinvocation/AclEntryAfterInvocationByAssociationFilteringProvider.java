package gemma.gsec.acl.afterinvocation;

import org.springframework.security.acls.afterinvocation.AclEntryAfterInvocationProvider;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.core.Authentication;

import java.util.List;

@SuppressWarnings("unused")
public abstract class AclEntryAfterInvocationByAssociationFilteringProvider extends AclEntryAfterInvocationProvider {

    public AclEntryAfterInvocationByAssociationFilteringProvider( AclService aclService, String processConfigAttribute, List<Permission> requirePermission ) {
        super( aclService, processConfigAttribute, requirePermission );
    }

    @Override
    protected boolean hasPermission( Authentication authentication, Object domainObject ) {
        return super.hasPermission( authentication, getActualDomainObject( domainObject ) );
    }

    /**
     * Obtain the associated domain object for which ACLs should be evaluated.
     */
    protected abstract Object getActualDomainObject( Object targetDomainObject );
}
