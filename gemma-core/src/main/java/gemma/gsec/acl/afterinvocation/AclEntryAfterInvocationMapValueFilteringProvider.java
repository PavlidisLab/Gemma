/*
 * The Gemma project
 *
 * Copyright (c) 2010 University of British Columbia
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
 * Filter a one-to-one map where the keys are NON-SECURABLE and the values ARE securable (or at least, can be). The
 * values can be a mixture of securable or non-securable. If you are using a map where both they keys and values are
 * securable, use {@link AclEntryAfterInvocationMapFilteringProvider}
 *
 * @author paul
 * @version $Id: AclAfterInvocationMapValueFilteringProvider.java,v 1.6 2013/09/14 16:56:00 paul Exp $
 * @see AclEntryAfterInvocationMapFilteringProvider
 */
public class AclEntryAfterInvocationMapValueFilteringProvider extends AclEntryAfterInvocationCollectionFilteringProvider {

    public AclEntryAfterInvocationMapValueFilteringProvider( AclService aclService, List<Permission> requirePermission ) {
        super( aclService, "AFTER_ACL_MAP_VALUES_READ", requirePermission );
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
