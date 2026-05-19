/*
 * The Gemma project
 *
 * Copyright (c) 2008 University of British Columbia
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

import gemma.gsec.acl.domain.AclPrincipalSid;
import gemma.gsec.model.Securable;
import gemma.gsec.util.SecurityUtil;
import org.springframework.security.access.AfterInvocationProvider;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;

import java.util.List;

/**
 * Filter out public {@link Securable}s, leaving only ones that the user owns and can edit. This is used for the
 * "my data" list. Data sets that are only readable are omitted.
 *
 * @author keshav
 * @version $Id: AclAfterFilterCollectionForMyData.java,v 1.9 2013/09/14 16:56:01 paul Exp $
 * @see AfterInvocationProvider
 */
public class AclEntryAfterInvocationOwnedCollectionFilteringProvider extends AclEntryAfterInvocationCollectionFilteringProvider {

    public AclEntryAfterInvocationOwnedCollectionFilteringProvider( AclService aclService, List<Permission> requirePermission ) {
        super( aclService, "AFTER_ACL_FILTER_MY_DATA", requirePermission );
    }

    @Override
    protected boolean hasPermission( Acl acl, List<Sid> sids ) {
        boolean isAdmin = SecurityUtil.isUserAdmin();
        return super.hasPermission( acl, sids ) && ( isAdmin || ownedByCurrentUser( acl ) );
    }

    private boolean ownedByCurrentUser( Acl acl ) {
        String ownerName = gemma.gsec.acl.domain.Sids.principalName( acl.getOwner() );
        return ownerName != null && ownerName.equals( SecurityUtil.getCurrentUsername() );
    }
}
