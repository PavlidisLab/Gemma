/*
 * The Gemma_sec1 project
 *
 * Copyright (c) 2009 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
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

/**
 * Like the AclEntryAfterInvocationCollectionFilteringProvider, but filters on the keys AND values of a Map, where the
 * keys are Securable and the values MAY be Securable. If your keys are non-securable, use
 * {@link AclEntryAfterInvocationMapValueFilteringProvider}
 *
 * @author paul
 * @version $Id: AclAfterInvocationMapFilteringProvider.java,v 1.11 2013/09/14 16:56:01 paul Exp $
 * @see org.springframework.security.acls.afterinvocation.AclEntryAfterInvocationCollectionFilteringProvider
 * @see AclEntryAfterInvocationMapValueFilteringProvider
 */
public class AclEntryAfterInvocationMapFilteringProvider extends AclEntryAfterInvocationCollectionFilteringProvider {

    public AclEntryAfterInvocationMapFilteringProvider( AclService aclService, List<Permission> requirePermission ) {
        super( aclService, "AFTER_ACL_MAP_READ", requirePermission );
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
                super.decide( authentication, object, config, map.keySet() );
                return returnedObject;
            } else {
                throw new AuthorizationServiceException( "A Map was required as the "
                    + "returnedObject, but the returnedObject was: " + returnedObject );
            }
        }
        return returnedObject;
    }
}
