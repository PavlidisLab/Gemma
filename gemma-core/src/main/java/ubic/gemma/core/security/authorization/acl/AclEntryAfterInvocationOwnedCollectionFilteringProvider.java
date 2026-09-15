/*
 * The Gemma project (ported into gemma-core for Phase 3 AfterInvocation modernization)
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
 */
package ubic.gemma.core.security.authorization.acl;

import ubic.gemma.core.security.util.SecurityUtil;
import org.springframework.security.access.AfterInvocationProvider;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;

import java.util.List;

/**
 * Filter a collection of {@code Securable} so that only the entries the current user owns (or is an admin on) and can
 * edit remain. Public, read-only-shared data sets are removed — this powers the "my data" listing.
 * <p>
 * Phase 3 AfterInvocation Phase B port: verbatim port of
 * {@code ubic.gemma.core.security.acl.afterinvocation.AclEntryAfterInvocationOwnedCollectionFilteringProvider} into gemma-core so
 * the AfterInvocation provider chain no longer depends on the gsec class. The owner-name lookup is still done via the
 * gsec {@code AclPrincipalSid}/{@code Sids} utility — that's a small utility class, not a Spring Security
 * after-invocation provider; this migration is scoped to the latter.
 *
 * @author keshav
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
        String ownerName = ubic.gemma.core.security.acl.domain.Sids.principalName( acl.getOwner() );
        return ownerName != null && ownerName.equals( SecurityUtil.getCurrentUsername() );
    }
}
