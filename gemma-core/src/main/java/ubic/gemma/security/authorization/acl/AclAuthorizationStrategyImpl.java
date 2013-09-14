/*
 * The gemma-core project
 * 
 * Copyright (c) 2013 University of British Columbia
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

import java.util.Arrays;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.acls.domain.AclAuthorizationStrategy;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.acls.model.SidRetrievalStrategy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;

import ubic.gemma.model.common.auditAndSecurity.acl.AclPrincipalSid;

/**
 * This is only needed because we use a custom Sid implementation.
 * 
 * @author Paul
 * @version $Id$
 */
public class AclAuthorizationStrategyImpl implements AclAuthorizationStrategy {

    private GrantedAuthority gaGeneralChanges;
    private GrantedAuthority gaModifyAuditing;
    private GrantedAuthority gaTakeOwnership;
    private SidRetrievalStrategy sidRetrievalStrategy = new AclSidRetrievalStrategyImpl();

    /**
     * Constructor. The only mandatory parameter relates to the system-wide {@link GrantedAuthority} instances that can
     * be held to always permit ACL changes.
     * 
     * @param auths an array of <code>GrantedAuthority</code>s that have special permissions (index 0 is the authority
     *        needed to change ownership, index 1 is the authority needed to modify auditing details, index 2 is the
     *        authority needed to change other ACL and ACE details) (required)
     */
    @SuppressWarnings("null")
    public AclAuthorizationStrategyImpl( GrantedAuthority[] auths ) {
        Assert.isTrue( auths != null && auths.length == 3, "GrantedAuthority[] with three elements required" );
        this.gaTakeOwnership = auths[0];
        this.gaModifyAuditing = auths[1];
        this.gaGeneralChanges = auths[2];
    }

    // ~ Methods
    // ========================================================================================================

    @Override
    public void securityCheck( Acl acl, int changeType ) {
        if ( ( SecurityContextHolder.getContext() == null )
                || ( SecurityContextHolder.getContext().getAuthentication() == null )
                || !SecurityContextHolder.getContext().getAuthentication().isAuthenticated() ) {
            throw new AccessDeniedException( "Authenticated principal required to operate with ACLs" );
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Check if authorized by virtue of ACL ownership
        Sid currentUser = new AclPrincipalSid( authentication ); // this is the only line that differs from spring
                                                                 // implementation.

        if ( currentUser.equals( acl.getOwner() )
                && ( ( changeType == CHANGE_GENERAL ) || ( changeType == CHANGE_OWNERSHIP ) ) ) {
            return;
        }

        // Not authorized by ACL ownership; try via adminstrative permissions
        GrantedAuthority requiredAuthority = null;

        if ( changeType == CHANGE_AUDITING ) {
            requiredAuthority = this.gaModifyAuditing;
        } else if ( changeType == CHANGE_GENERAL ) {
            requiredAuthority = this.gaGeneralChanges;
        } else if ( changeType == CHANGE_OWNERSHIP ) {
            requiredAuthority = this.gaTakeOwnership;
        } else {
            throw new IllegalArgumentException( "Unknown change type" );
        }

        // Iterate this principal's authorities to determine right
        if ( authentication.getAuthorities().contains( requiredAuthority ) ) {
            return;
        }

        // Try to get permission via ACEs within the ACL
        List<Sid> sids = sidRetrievalStrategy.getSids( authentication );

        if ( acl.isGranted( Arrays.asList( BasePermission.ADMINISTRATION ), sids, false ) ) {
            return;
        }

        throw new AccessDeniedException(
                "Principal does not have required ACL permissions to perform requested operation" );
    }

    public void setSidRetrievalStrategy( SidRetrievalStrategy sidRetrievalStrategy ) {
        Assert.notNull( sidRetrievalStrategy, "SidRetrievalStrategy required" );
        this.sidRetrievalStrategy = sidRetrievalStrategy;
    }

}
