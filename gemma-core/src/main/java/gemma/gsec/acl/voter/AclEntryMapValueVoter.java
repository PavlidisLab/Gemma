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
 * A voter for the value of a map.
 */
@SuppressWarnings("unused")
public class AclEntryMapValueVoter extends AclEntryCollectionVoter {

    public AclEntryMapValueVoter( AclService aclService, String processConfigAttribute, Permission[] requirePermission ) {
        super( aclService, processConfigAttribute, requirePermission );
    }

    @Override
    protected Collection<?> getCollectionInstance( MethodInvocation secureObject ) {
        Class<?>[] params = secureObject.getMethod().getParameterTypes();
        Type[] types = secureObject.getMethod().getGenericParameterTypes();
        Object[] args = secureObject.getArguments();
        for ( int i = 0; i < args.length; i++ ) {
            if ( Map.class.isAssignableFrom( params[i] ) ) {
                Type valueType = ( ( ParameterizedType ) types[i] ).getActualTypeArguments()[1];
                Collection<?> mapValues = args[i] != null ? ( ( Map<?, ?> ) args[i] ).values() : null;
                if ( TypeUtils.isAssignable( getProcessDomainObjectClass(), valueType ) ) {
                    return mapValues;
                }
                if ( areAllElementsAssignable( getProcessDomainObjectClass(), mapValues ) ) {
                    return mapValues;
                }
            }
        }
        throw new AuthorizationServiceException( "Secure object: " + secureObject
            + " did not provide a Map with values of " + this.getProcessDomainObjectClass() + "'s" );
    }
}