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

import ubic.gemma.core.security.gsec.util.SecurityUtil;
import org.springframework.security.access.AfterInvocationProvider;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;

import java.util.Collections;
import java.util.List;

/**
 * Filter a collection of {@code Securable} to keep only entries that are <em>not</em> public AND that the current user
 * can read (this includes data sets that are read-only-shared by another user). Used for the "my private data"
 * listing.
 * <p>
 * Phase 3 AfterInvocation Phase B port: verbatim port of
 * {@code ubic.gemma.core.security.gsec.acl.afterinvocation.AclEntryAfterInvocationPrivateCollectionFilteringProvider} into gemma-core so
 * the AfterInvocation provider chain no longer depends on the gsec class.
 *
 * @author keshav
 * @see AfterInvocationProvider
 */
public class AclEntryAfterInvocationPrivateCollectionFilteringProvider extends AclEntryAfterInvocationCollectionFilteringProvider {

    public AclEntryAfterInvocationPrivateCollectionFilteringProvider( AclService aclService, List<Permission> requirePermission ) {
        super( aclService, "AFTER_ACL_FILTER_MY_PRIVATE_DATA", requirePermission );
    }

    @Override
    protected boolean hasPermission( Acl acl, List<Sid> sids ) {
        return super.hasPermission( acl, sids )
                && SecurityUtil.isPrivate( acl )
                && isReadable( acl, sids );
    }

    private boolean isReadable( Acl acl, List<Sid> sids ) {
        return acl.isGranted( Collections.singletonList( BasePermission.READ ), sids, false )
                || acl.isGranted( Collections.singletonList( BasePermission.ADMINISTRATION ), sids, false );
    }
}
