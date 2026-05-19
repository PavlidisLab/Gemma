package gemma.gsec.acl.afterinvocation;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.core.Authentication;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class AclEntryAfterInvocationValueObjectMapValueFilteringProvider extends AclEntryAfterInvocationValueObjectCollectionFilteringProvider {

    public AclEntryAfterInvocationValueObjectMapValueFilteringProvider( AclService aclService, List<Permission> requirePermission ) {
        super( aclService, "AFTER_ACL_VALUE_OBJECT_MAP_VALUE_READ", requirePermission );
    }

    @Override
    public Object decide( Authentication authentication, Object object, Collection<ConfigAttribute> config,
        Object returnedObject ) throws AccessDeniedException {
        for ( ConfigAttribute configAttribute : config ) {
            if ( !supports( configAttribute ) ) {
                continue;
            }
            if ( returnedObject instanceof Map ) {
                Map<?, ?> map = ( Map<?, ?> ) returnedObject;
                super.decide( authentication, object, config, map.values() );
                return returnedObject;
            } else {
                throw new AuthorizationServiceException( "A Map was required as the "
                    + "returnedObject, but the returnedObject was: " + returnedObject );
            }
        }
        return returnedObject;
    }
}
