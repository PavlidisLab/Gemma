/*
 * The Gemma project
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
package gemma.gsec.util;

import gemma.gsec.AuthorityConstants;
import gemma.gsec.acl.domain.AclGrantedAuthoritySid;
import gemma.gsec.acl.domain.AclPrincipalSid;
import org.springframework.security.access.vote.AuthenticatedVoter;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * Database-independent methods for ACLs
 *
 * @author Paul
 * @version $Id: SecurityUtil.java,v 1.6 2013/09/14 16:56:04 paul Exp $
 */
public class SecurityUtil {

    private static final AuthenticationTrustResolver authenticationTrustResolver = new AuthenticationTrustResolverImpl();

    /**
     * Returns the username of the authenticated user
     */
    public static String getCurrentUsername() {
        Object principal = getAuthentication().getPrincipal();
        if ( principal instanceof String )
            return ( String ) principal;
        else if ( principal instanceof UserDetails ) return ( ( UserDetails ) principal ).getUsername();
        throw new UnsupportedOperationException( "Principal is of unrecognized type" );
    }

    public static boolean isOwner( Acl acl, String username ) {
        String ownerName = gemma.gsec.acl.domain.Sids.principalName( acl.getOwner() );
        return ownerName != null && ownerName.equals( username );
    }

    public static boolean isPublic( Acl acl ) {
        return !isPrivate( acl );
    }

    /**
     * Test whether the given ACL is constraining access to users who are at privileges above "anonymous".
     *
     * @return true if the permissions indicate 'non-public', false if 'public'.
     */
    public static boolean isPrivate( Acl acl ) {

        /*
         * If the given Acl has anonymous permissions on it, then it can't be private.
         */
        for ( AccessControlEntry ace : acl.getEntries() ) {

            if ( !ace.getPermission().equals( BasePermission.READ ) ) continue;

            String grantedAuthority = gemma.gsec.acl.domain.Sids.grantedAuthority( ace.getSid() );
            if ( grantedAuthority != null
                    && grantedAuthority.equals( AuthenticatedVoter.IS_AUTHENTICATED_ANONYMOUSLY )
                    && ace.isGranting() ) {
                return false;
            }
        }

        /*
         * Even if the object is not private, it's parent might be, and we might inherit that. Recursion happens here.
         */
        Acl parentAcl = acl.getParentAcl();
        if ( parentAcl != null && acl.isEntriesInheriting() ) {
            return isPrivate( parentAcl );
        }

        /*
         * We didn't find a granted authority on IS_AUTHENTICATED_ANONYMOUSLY
         */
        return true;

    }

    public static boolean isRunningAsAdmin() {

        Collection<? extends GrantedAuthority> authorities = getAuthentication().getAuthorities();
        assert authorities != null;
        for ( GrantedAuthority authority : authorities ) {
            if ( authority.getAuthority().equals( AuthorityConstants.RUN_AS_ADMIN_AUTHORITY ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if the ACL grants READ authority to at least one group that is not admin or agent.
     */
    public static boolean isShared( Acl acl ) {
        for ( AccessControlEntry ace : acl.getEntries() ) {

            if ( !ace.getPermission().equals( BasePermission.READ ) ) continue;

            String grantedAuthority = gemma.gsec.acl.domain.Sids.grantedAuthority( ace.getSid() );
            if ( grantedAuthority != null && grantedAuthority.startsWith( "GROUP_" ) && ace.isGranting() ) {
                if ( grantedAuthority.equals( AuthorityConstants.AGENT_GROUP_AUTHORITY )
                    || grantedAuthority.equals( AuthorityConstants.ADMIN_GROUP_AUTHORITY ) ) {
                    continue;
                }
                return true;
            }
        }

        /*
         * Even if the object is not private, its parent might be, and we might inherit that. Recursion happens here.
         */
        Acl parentAcl = acl.getParentAcl();
        if ( parentAcl != null && acl.isEntriesInheriting() ) {
            return isShared( parentAcl );
        }

        /*
         * We didn't find a granted authority for any group.
         */
        return false;
    }

    /**
     * Returns true if the current user has admin authority.
     *
     * @return true if the current user has admin authority
     */
    public static boolean isUserAdmin() {

        if ( !isUserLoggedIn() ) {
            return false;
        }

        Collection<? extends GrantedAuthority> authorities = getAuthentication().getAuthorities();
        assert authorities != null;
        for ( GrantedAuthority authority : authorities ) {
            if ( authority.getAuthority().equals( AuthorityConstants.ADMIN_GROUP_AUTHORITY ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the user is anonymous.
     */
    public static boolean isUserAnonymous() {
        return authenticationTrustResolver.isAnonymous( getAuthentication() );
    }

    /**
     * Returns true if the user is non-anonymous.
     */
    public static boolean isUserLoggedIn() {
        return !isUserAnonymous();
    }

    /**
     * Returns the Authentication object from the SecurityContextHolder.
     *
     * @return Authentication
     */
    private static Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if ( authentication == null ) throw new RuntimeException( "Null authentication object" );

        return authentication;
    }
}
