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
package gemma.gsec.acl.afterinvocation;

import gemma.gsec.model.SecureValueObject;
import gemma.gsec.util.SecurityUtil;
import org.springframework.security.acls.afterinvocation.AclEntryAfterInvocationProvider;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.*;
import org.springframework.security.core.Authentication;

import java.util.Collections;
import java.util.List;

/**
 * Security check for reading value objects. Also overrides default behaviour by returning null, rather than throwing an
 * access denied exception.
 * <p>
 * As a side effect, it fills in security status information in the value objects to which permission was granted.
 *
 * @author paul
 * @version $Id: AclAfterValueObjectProvider.java,v 1.7 2013/09/14 16:56:03 paul Exp $
 * @see AclEntryAfterInvocationValueObjectCollectionFilteringProvider for the same thing but for collections.
 */
public class AclEntryAfterInvocationValueObjectProvider extends AclEntryAfterInvocationProvider {

    public AclEntryAfterInvocationValueObjectProvider( AclService aclService, List<Permission> requirePermission ) {
        super( aclService, "AFTER_ACL_VALUE_OBJECT_READ", requirePermission );
    }

    @Override
    protected boolean hasPermission( Authentication authentication, Object domainObject ) {
        List<Sid> sids = sidRetrievalStrategy.getSids( authentication );
        ObjectIdentity objectIdentity = objectIdentityRetrievalStrategy.getObjectIdentity( domainObject );
        try {
            Acl acl = aclService.readAclById( objectIdentity, sids );
            if ( domainObject instanceof SecureValueObject ) {
                populateValueObject( ( SecureValueObject ) domainObject, acl, sids, requirePermission, SecurityUtil.getCurrentUsername(), SecurityUtil.isUserAdmin() );
            }
            return acl.isGranted( requirePermission, sids, false );
        } catch ( NotFoundException ignore ) {
            return false;
        }
    }

    static void populateValueObject( SecureValueObject svo, Acl acl, List<Sid> sids, List<Permission> requirePermission, String currentUsername, boolean isAdmin ) {
        if ( SecurityUtil.isUserLoggedIn() ) {
            svo.setIsPublic( !SecurityUtil.isPrivate( acl ) );
            svo.setIsShared( SecurityUtil.isShared( acl ) );
            svo.setUserOwned( SecurityUtil.isOwner( acl, currentUsername ) );
            if ( svo.getUserOwned() || isAdmin || requirePermission.contains( BasePermission.WRITE ) ) {
                svo.setUserCanWrite( true );
            } else {
                svo.setUserCanWrite( acl.isGranted( Collections.singletonList( BasePermission.WRITE ), sids, false ) );
            }
        }
    }
}
