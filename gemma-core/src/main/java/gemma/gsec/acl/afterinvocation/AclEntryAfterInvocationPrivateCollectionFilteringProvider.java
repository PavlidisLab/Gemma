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

import gemma.gsec.model.Securable;
import gemma.gsec.util.SecurityUtil;
import org.springframework.security.access.AfterInvocationProvider;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;

import java.util.Collections;
import java.util.List;

/**
 * Filter out public {@link Securable}s, leaving only ones that the user specifically can view but aren't public. This
 * includes data sets that are read-only for the user, e.g. shared by another user
 *
 * @author keshav
 * @version $Id: AclAfterFilterCollectionForMyPrivateData.java,v 1.4 2013/09/14 16:56:02 paul Exp $
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
        return ( acl.isGranted( Collections.singletonList( BasePermission.READ ), sids, false )
            || acl.isGranted( Collections.singletonList( BasePermission.ADMINISTRATION ), sids, false ) );
    }
}
