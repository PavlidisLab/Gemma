/*
 * The Gemma project
 *
 * Copyright (c) 2013 University of British Columbia
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

import gemma.gsec.model.SecureValueObject;
import gemma.gsec.util.SecurityUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.acls.model.*;
import org.springframework.security.core.Authentication;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static gemma.gsec.acl.afterinvocation.AclEntryAfterInvocationValueObjectProvider.populateValueObject;

/**
 * Security check for reading collections of SecureValueObjects, or maps that have SecureValueObjects as keys - map
 * values are NOT checked.
 * <p>
 * As a side effect, it fills in security status information in the value objects, on those object for which permission
 * was granted.
 *
 * @author cmcdonald
 * @version $Id: AclAfterFilterValueObjectCollectionProvider.java,v 1.9 2013/09/14 16:56:01 paul Exp $
 */
public class AclEntryAfterInvocationValueObjectCollectionFilteringProvider extends AclEntryAfterInvocationCollectionFilteringProvider {

    protected static final Log log = LogFactory.getLog( AclEntryAfterInvocationValueObjectCollectionFilteringProvider.class );

    // used in the XML configuration
    @SuppressWarnings("unused")
    public AclEntryAfterInvocationValueObjectCollectionFilteringProvider( AclService aclService, List<Permission> requirePermission ) {
        super( aclService, "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ", requirePermission );
    }

    protected AclEntryAfterInvocationValueObjectCollectionFilteringProvider( AclService aclService, String processConfigAttribute, List<Permission> requirePermission ) {
        super( aclService, processConfigAttribute, requirePermission );
    }

    @Override
    protected boolean[] hasPermission( Authentication authentication, List<Object> domainObjects ) {
        boolean[] perms = new boolean[domainObjects.size()];

        List<Sid> sids = this.sidRetrievalStrategy.getSids( authentication );
        List<ObjectIdentity> ois = getObjectIdentities( domainObjects );
        Map<ObjectIdentity, Acl> aclsById;
        try {
            aclsById = aclService.readAclsById( ois, sids );
        } catch ( NotFoundException e ) {
            aclsById = Collections.emptyMap();
        }

        String currentUsername = SecurityUtil.getCurrentUsername();
        boolean isAdmin = SecurityUtil.isUserAdmin();

        int i = 0;
        for ( ObjectIdentity oi : ois ) {
            Acl acl = aclsById.get( oi );
            if ( acl != null ) {
                perms[i] = acl.isGranted( requirePermission, sids, false );
                Object domainObject = domainObjects.get( i );
                if ( domainObject instanceof SecureValueObject ) {
                    populateValueObject( ( SecureValueObject ) domainObject, acl, sids, requirePermission, currentUsername, isAdmin );
                }
            } else {
                log.trace( String.format( "No ACL was found for %s.", oi ) );
                perms[i] = false;
            }
            i++;
        }

        return perms;
    }
}
