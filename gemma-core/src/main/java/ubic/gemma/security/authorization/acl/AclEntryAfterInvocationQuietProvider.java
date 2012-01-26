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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.core.Authentication;

/**
 * Overrides default behaviour by returning null, rather than throwing an access denied exception
 * 
 * @author paul
 * @version $Id$
 */
public class AclEntryAfterInvocationQuietProvider extends
        org.springframework.security.acls.afterinvocation.AclEntryAfterInvocationProvider {

    private static Log log = LogFactory.getLog( AclEntryAfterInvocationQuietProvider.class );

    public AclEntryAfterInvocationQuietProvider( AclService aclService, List<Permission> requirePermission ) {
        super( aclService, "AFTER_ACL_READ_QUIET", requirePermission );
    }

    @Override
    public Object decide( Authentication authentication, Object object, Collection<ConfigAttribute> config,
            Object returnedObject ) throws AccessDeniedException {
        try {
            return super.decide( authentication, object, config, returnedObject );
        } catch ( AccessDeniedException e ) {
            log.warn( e + ": returning null" );
            return null;
        }
    }

}
