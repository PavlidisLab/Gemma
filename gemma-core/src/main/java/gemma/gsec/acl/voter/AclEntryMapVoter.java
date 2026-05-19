package gemma.gsec.acl.voter;

import gemma.gsec.acl.domain.AclService;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.security.acls.model.Permission;
import org.springframework.util.TypeUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

/**
 * A voter over the keys of a map.
 */
@SuppressWarnings("unused")
public class AclEntryMapVoter extends AclEntryCollectionVoter {

    public AclEntryMapVoter( AclService aclService, String processConfigAttribute, Permission[] requirePermission ) {
        super( aclService, processConfigAttribute, requirePermission );
    }

    @Override
    protected Collection<?> getCollectionInstance( MethodInvocation secureObject ) {
        Class<?>[] params = secureObject.getMethod().getParameterTypes();
        Type[] types = secureObject.getMethod().getGenericParameterTypes();
        Object[] args = secureObject.getArguments();
        for ( int i = 0; i < args.length; i++ ) {
            if ( Map.class.isAssignableFrom( params[i] ) ) {
                Type keyType = ( ( ParameterizedType ) types[i] ).getActualTypeArguments()[0];
                Collection<?> mapKeys = args[i] != null ? ( ( Map<?, ?> ) args[i] ).keySet() : null;
                if ( TypeUtils.isAssignable( getProcessDomainObjectClass(), keyType ) ) {
                    return mapKeys;
                }
                if ( areAllElementsAssignable( getProcessDomainObjectClass(), mapKeys ) ) {
                    return mapKeys;
                }
            }
        }
        throw new AuthorizationServiceException( "Secure object: " + secureObject
            + " did not provide a Map with keys of " + this.getProcessDomainObjectClass() + "'s" );
    }
}