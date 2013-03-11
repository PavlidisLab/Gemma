/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.security.authorization.acl;

import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.core.Authentication;

import ubic.gemma.model.common.auditAndSecurity.SecureValueObject;
import ubic.gemma.security.SecurityService;

/**
 * Security check for reading value objects. Also overrides default behaviour by returning null, rather than throwing an
 * access denied exception
 * 
 * @author paul
 * @version $Id$
 * @see AclAfterFilterValueObjectCollectionProvider for the same thing but for collections.
 */
public class AclAfterValueObjectProvider extends
        org.springframework.security.acls.afterinvocation.AclEntryAfterInvocationProvider {

    private static Log log = LogFactory.getLog( AclAfterValueObjectProvider.class );

    public AclAfterValueObjectProvider( AclService aclService, List<Permission> requirePermission ) {
        super( aclService, "AFTER_ACL_VALUE_OBJECT_READ", requirePermission );
    }

    @Autowired
    private SecurityService securityService;

    @Override
    public Object decide( Authentication authentication, Object object, Collection<ConfigAttribute> config,
            Object returnedObject ) throws AccessDeniedException {
        try {

            if ( !SecureValueObject.class.isAssignableFrom( returnedObject.getClass() ) ) {
                // nothing to do here.
                return returnedObject;
            }

            return securityService.hasPermission( ( SecureValueObject ) object, requirePermission, authentication );
        } catch ( AccessDeniedException e ) {
            log.warn( e.getMessage() + ": returning null" );
            return null;
        }
    }

}
