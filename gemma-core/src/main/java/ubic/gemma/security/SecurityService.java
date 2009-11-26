/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.vote.AuthenticatedVoter;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.ObjectIdentityRetrievalStrategyImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.ObjectIdentityRetrievalStrategy;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.acls.model.SidRetrievalStrategy;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.security.authentication.UserManager;
import ubic.gemma.util.AuthorityConstants;

/**
 * @author keshav
 * @author paul
 * @version $Id$
 */
/**
 * TODO Document Me
 * 
 * @author paul
 * @version $Id$
 */
@Service
@Lazy
public class SecurityService {

    /**
     * This is defined in spring-security AuthenticationConfigBuilder, and can be set in the <security:anonymous />
     * configuration of the <security:http/> namespace config
     */
    public static final String ANONYMOUS = "anonymousUser";

    private static AuthenticationTrustResolver authenticationTrustResolver = new AuthenticationTrustResolverImpl();

    /**
     * Returns true if the current user has admin authority.
     * 
     * @return true if the current user has admin authority
     */
    public static boolean isUserAdmin() {

        if ( !isUserLoggedIn() ) {
            return false;
        }

        Collection<GrantedAuthority> authorities = getAuthentication().getAuthorities();
        assert authorities != null;
        for ( GrantedAuthority authority : authorities ) {
            if ( authority.getAuthority().equals( AuthorityConstants.ADMIN_GROUP ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return
     */
    public static boolean isUserAnonymous() {
        return authenticationTrustResolver.isAnonymous( getAuthentication() )
                || getAuthentication().getPrincipal().equals( "anonymousUser" );
    }

    /**
     * Returns true if the user is non-anonymous.
     * 
     * @return
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

    @Autowired
    private MutableAclService aclService;

    private Log log = LogFactory.getLog( SecurityService.class );

    private ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy = new ObjectIdentityRetrievalStrategyImpl();

    @Autowired
    private SidRetrievalStrategy sidRetrievalStrategy;

    @Autowired
    private UserManager userManager;

    /**
     * @param securables
     * @return
     */
    public java.util.Map<Securable, Boolean> arePrivate( Collection<? extends Securable> securables ) {
        Map<Securable, Boolean> result = new HashMap<Securable, Boolean>();
        for ( Securable s : securables ) {
            boolean p = isPrivate( s );
            result.put( s, p );
        }
        return result;
    }

    /**
     * If the group already exists, an exception will be thrown.
     * 
     * @param groupName
     */
    @Transactional
    public void createGroup( String groupName ) {

        /*
         * Nice if we can get around this uniqueness constraint...but I guess it's not easy.
         */
        if ( userManager.groupExists( groupName ) ) {
            throw new IllegalArgumentException( "A group already exists with that name" );
        }

        /*
         * We do make the groupAuthority unique.
         */
        String groupAuthority = groupName.toUpperCase() + "_"
                + RandomStringUtils.randomAlphanumeric( 32 ).toUpperCase();

        List<GrantedAuthority> auths = new ArrayList<GrantedAuthority>();
        auths.add( new GrantedAuthorityImpl( groupAuthority ) );

        this.userManager.createGroup( groupName, auths );
        addUserToGroup( userManager.getCurrentUsername(), groupName );

    }

    /**
     * @param userName
     * @param groupName
     */
    public void addUserToGroup( String userName, String groupName ) {
        this.userManager.addUserToGroup( userName, groupName );
    }

    /**
     * @param securables
     * @return the subset which are private, if any
     */
    public Collection<Securable> choosePrivate( Collection<? extends Securable> securables ) {
        Collection<Securable> result = new HashSet<Securable>();
        for ( Securable s : securables ) {
            if ( isPrivate( s ) ) result.add( s );
        }
        return result;
    }

    /**
     * @param securables
     * @return the subset that are public, if any
     */
    public Collection<Securable> choosePublic( Collection<? extends Securable> securables ) {
        Collection<Securable> result = new HashSet<Securable>();
        for ( Securable s : securables ) {
            if ( isPublic( s ) ) result.add( s );
        }
        return result;
    }

    /**
     * @param s
     * @return list of userNames who can edit the given securable.
     */
    @Secured( { "ACL_SECURABLE_READ" })
    public Collection<String> editableBy( Securable s ) {

        Collection<String> allUsers = userManager.findAllUsers();

        Collection<String> result = new HashSet<String>();

        for ( String u : allUsers ) {
            if ( isEditableByUser( s, u ) ) {
                result.add( u );
            }
        }

        return result;

    }

    /**
     * @param s
     * @param userName
     * @return true if the user has WRITE permissions or ADMIN
     */
    @Secured("ACL_SECURABLE_READ")
    public boolean isEditableByUser( Securable s, String userName ) {
        List<Permission> requiredPermissions = new ArrayList<Permission>();
        requiredPermissions.add( BasePermission.WRITE );
        if ( hasPermission( s, requiredPermissions, userName ) ) {
            return true;
        }

        requiredPermissions.clear();
        requiredPermissions.add( BasePermission.ADMINISTRATION );
        return hasPermission( s, requiredPermissions, userName );
    }

    /**
     * Convenience method to determine the visibility of an object.
     * 
     * @param s
     * @return true if anonymous users can view (READ) the object, false otherwise. If the object doesn't have an ACL,
     *         return true (be safe!)
     * @see org.springframework.security.acls.jdbc.BasicLookupStrategy
     */
    public boolean isPrivate( Securable s ) {

        if ( s == null ) {
            return false;
        }

        /*
         * Implementation note: this code mimics AclEntryVoter.vote, but in adminsitrative mode so no auditing etc
         * happens.
         */

        List<Permission> perms = new Vector<Permission>();
        perms.add( BasePermission.READ );

        Sid anonSid = new GrantedAuthoritySid( new GrantedAuthorityImpl(
                AuthenticatedVoter.IS_AUTHENTICATED_ANONYMOUSLY ) );

        List<Sid> sids = new Vector<Sid>();
        sids.add( anonSid );

        ObjectIdentity oi = new ObjectIdentityImpl( s.getClass(), s.getId() );

        /*
         * Note: in theory, it should pay attention to the sid we ask for and return nothing if there is no acl.
         * However, the implementation actually ignores the sid argument. See BasicLookupStrategy
         */
        try {
            Acl acl = this.aclService.readAclById( oi, sids );

            return isPrivate( acl );
        } catch ( NotFoundException nfe ) {
            return true;
        }

    }

    /**
     * Convenience method to determine the visibility of an object.
     * 
     * @param s
     * @return the negation of isPrivate().
     */
    public boolean isPublic( Securable s ) {
        return !isPrivate( s );
    }

    /**
     * @param s
     * @param userName
     * @return true if the given user can read the securable, false otherwise. (READ or ADMINISTRATION required)
     */
    @Secured( { "ACL_SECURABLE_READ" })
    public boolean isViewableByUser( Securable s, String userName ) {
        List<Permission> requiredPermissions = new ArrayList<Permission>();
        requiredPermissions.add( BasePermission.READ );
        if ( hasPermission( s, requiredPermissions, userName ) ) {
            return true;
        }

        requiredPermissions.clear();
        requiredPermissions.add( BasePermission.ADMINISTRATION );
        return hasPermission( s, requiredPermissions, userName );
    }

    /**
     * @param objs
     */
    public void makePrivate( Collection<? extends Securable> objs ) {
        for ( Securable s : objs ) {
            makePrivate( s );
        }
    }

    /**
     * Makes the object private.
     * 
     * @param object
     */
    @Secured("ACL_SECURABLE_EDIT")
    @Transactional
    public void makePrivate( Securable object ) {
        if ( object == null ) {
            return;
        }

        if ( isPrivate( object ) ) {
            log.warn( "Object is already private" );
            return;
        }

        /*
         * Remove ACE for IS_AUTHENTICATED_ANOYMOUSLY, if it's there.
         */
        String authorityToRemove = AuthorityConstants.IS_AUTHENTICATED_ANONYMOUSLY;

        removeGrantedAuthority( object, BasePermission.READ, authorityToRemove );

        if ( isPublic( object ) ) {
            throw new IllegalStateException( "Failed to make object private: " + object );
        }

    }

    /**
     * @param objs
     */
    @Transactional
    public void makePublic( Collection<? extends Securable> objs ) {
        for ( Securable s : objs ) {
            makePublic( s );
        }
    }

    /**
     * Makes the object public
     * 
     * @param object
     */
    @Secured("ACL_SECURABLE_EDIT")
    @Transactional
    public void makePublic( Securable object ) {

        if ( object == null ) {
            return;
        }

        if ( isPublic( object ) ) {
            log.warn( "Object is already public" );
            return;
        }

        /*
         * Add an ACE for IS_AUTHENTICATED_ANOYMOUSLY.
         */

        MutableAcl acl = getAcl( object );

        if ( acl == null ) {
            throw new IllegalArgumentException( "makePrivate is only valid for objects that have an ACL" );
        }

        acl.insertAce( acl.getEntries().size(), BasePermission.READ, new GrantedAuthoritySid( new GrantedAuthorityImpl(
                AuthorityConstants.IS_AUTHENTICATED_ANONYMOUSLY ) ), true );

        aclService.updateAcl( acl );

        if ( isPrivate( object ) ) {
            throw new IllegalStateException( "Failed to make object public: " + object );
        }

    }

    /**
     * Administrative method to allow a user to get access to an object. This is useful for cases where a data set is
     * loaded by admin but we need to hand it off to a user.
     * 
     * @param s
     * @param userName
     */
    @Secured("GROUP_ADMIN")
    @Transactional
    public void makeOwnedByUser( Securable s, String userName ) {
        MutableAcl acl = getAcl( s );
        acl.setOwner( new PrincipalSid( userName ) );
        aclService.updateAcl( acl );

        /*
         * FIXME: I don't know if these are necessary if you are the owner.
         */
        addPrincipalAuthority( s, BasePermission.WRITE, userName );
        addPrincipalAuthority( s, BasePermission.READ, userName );
    }

    /**
     * Adds read permission.
     * 
     * @param s
     * @param groupName
     * @throws AccessDeniedException
     */
    @Secured("ACL_SECURABLE_EDIT")
    @Transactional
    public void makeReadableByGroup( Securable s, String groupName ) throws AccessDeniedException {
        Collection<String> groups = checkForGroupAccessByCurrentuser( groupName );

        if ( !groups.contains( groupName ) ) {
            throw new AccessDeniedException( "User doesn't have access to that group" );
        }

        addGroupAuthority( s, BasePermission.READ, groupName );

    }

    /**
     * Remove read permissions; also removes write permissions.
     * 
     * @param s
     * @param groupName, with or without GROUP_
     * @throws AccessDeniedException
     */
    @Secured("ACL_SECURABLE_EDIT")
    @Transactional
    public void makeUnreadableByGroup( Securable s, String groupName ) throws AccessDeniedException {

        Collection<String> groups = checkForGroupAccessByCurrentuser( groupName );

        if ( !groups.contains( groupName ) ) {
            throw new AccessDeniedException( "User doesn't have access to that group" );
        }

        List<GrantedAuthority> groupAuthorities = userManager.findGroupAuthorities( groupName );

        if ( groupAuthorities == null || groupAuthorities.isEmpty() ) {
            throw new IllegalStateException( "Group has no authorities" );
        }

        if ( groupAuthorities.size() > 1 ) {
            throw new UnsupportedOperationException( "Sorry, groups can only have a single authority" );
        }

        GrantedAuthority ga = groupAuthorities.get( 0 );

        String authority = ga.getAuthority();

        removeGrantedAuthority( s, BasePermission.READ, userManager.getRolePrefix() + authority );
        removeGrantedAuthority( s, BasePermission.WRITE, userManager.getRolePrefix() + authority );
    }

    /**
     * Remove write permissions. Leaves read permissions, if present.
     * 
     * @param s
     * @param groupName
     * @throws AccessDeniedException
     */
    @Secured("ACL_SECURABLE_EDIT")
    @Transactional
    public void makeUnwriteableByGroup( Securable s, String groupName ) throws AccessDeniedException {

        Collection<String> groups = checkForGroupAccessByCurrentuser( groupName );

        if ( !groups.contains( groupName ) ) {
            throw new AccessDeniedException( "User doesn't have access to that group" );
        }

        removeGrantedAuthority( s, BasePermission.WRITE, groupName );
    }

    /**
     * Adds write (and read) permissions.
     * 
     * @param s
     * @param groupName
     * @throws AccessDeniedException
     */
    @Secured("ACL_SECURABLE_EDIT")
    @Transactional
    public void makeWriteableByGroup( Securable s, String groupName ) throws AccessDeniedException {
        Collection<String> groups = checkForGroupAccessByCurrentuser( groupName );

        if ( !groups.contains( groupName ) ) {
            throw new AccessDeniedException( "User doesn't have access to that group" );
        }

        addGroupAuthority( s, BasePermission.WRITE, groupName );
        addGroupAuthority( s, BasePermission.READ, groupName );
    }

    /**
     * @param s
     * @return list of userNames of users who can read the given securable.
     */
    @Secured("ACL_SECURABLE_EDIT")
    public Collection<String> readableBy( Securable s ) {
        Collection<String> allUsers = userManager.findAllUsers();

        Collection<String> result = new HashSet<String>();

        for ( String u : allUsers ) {
            if ( isViewableByUser( s, u ) ) {
                result.add( u );
            }
        }

        return result;
    }

    /**
     * Provide permission to the given group on the given securable.
     * 
     * @param s
     * @param permission
     * @param groupName e.g. "GROUP_JOESLAB"
     */
    private void addGroupAuthority( Securable s, Permission permission, String groupName ) {
        MutableAcl acl = getAcl( s );

        List<GrantedAuthority> groupAuthorities = userManager.findGroupAuthorities( groupName );

        if ( groupAuthorities == null || groupAuthorities.isEmpty() ) {
            throw new IllegalStateException( "Group has no authorities" );
        }

        if ( groupAuthorities.size() > 1 ) {
            throw new UnsupportedOperationException( "Sorry, groups can only have a single authority" );
        }

        GrantedAuthority ga = groupAuthorities.get( 0 );

        acl.insertAce( acl.getEntries().size(), permission,
                new GrantedAuthoritySid( userManager.getRolePrefix() + ga ), true );
        aclService.updateAcl( acl );
    }

    /**
     * @param s
     * @param permission
     * @param principal i.e. username
     */
    private void addPrincipalAuthority( Securable s, Permission permission, String principal ) {
        MutableAcl acl = getAcl( s );
        acl.insertAce( acl.getEntries().size(), permission, new PrincipalSid( principal ), true );
        aclService.updateAcl( acl );
    }

    /**
     * @param groupName
     * @return
     */
    private Collection<String> checkForGroupAccessByCurrentuser( String groupName ) {
        if ( groupName.equals( AuthorityConstants.ADMIN_GROUP ) ) {
            throw new AccessDeniedException( "Attempt to mess with ADMIN privileges denied" );
        }
        Collection<String> groups = userManager.findGroupsForUser( userManager.getCurrentUsername() );
        return groups;
    }

    /**
     * @param s
     * @return
     */
    private MutableAcl getAcl( Securable s ) {
        ObjectIdentity oi = objectIdentityRetrievalStrategy.getObjectIdentity( s );

        try {
            return ( MutableAcl ) aclService.readAclById( oi );
        } catch ( NotFoundException e ) {
            return null;
        }
    }

    /*
     * Private method that really doesn't work unless you are admin
     */
    private boolean hasPermission( Securable domainObject, List<Permission> requiredPermissions, String userName ) {

        // Obtain the OID applicable to the domain object
        ObjectIdentity objectIdentity = objectIdentityRetrievalStrategy.getObjectIdentity( domainObject );

        // Obtain the SIDs applicable to the principal
        UserDetails user = userManager.loadUserByUsername( userName );
        Authentication authentication = new UsernamePasswordAuthenticationToken( userName, user.getPassword(), user
                .getAuthorities() );
        List<Sid> sids = sidRetrievalStrategy.getSids( authentication );

        Acl acl = null;

        try {
            // Lookup only ACLs for SIDs we're interested in
            acl = aclService.readAclById( objectIdentity, sids );
            // administrative mode = true
            return acl.isGranted( requiredPermissions, sids, true );
        } catch ( NotFoundException ignore ) {
            return false;
        }
    }

    /**
     * @param acl
     * @return
     */
    private boolean isPrivate( Acl acl ) {

        /*
         * If the given Acl has anonymous permissions on it, then we can't be private.
         */
        for ( AccessControlEntry ace : acl.getEntries() ) {

            if ( !ace.getPermission().equals( BasePermission.READ ) ) continue;

            Sid sid = ace.getSid();
            if ( sid instanceof GrantedAuthoritySid ) {
                String grantedAuthority = ( ( GrantedAuthoritySid ) sid ).getGrantedAuthority();
                if ( grantedAuthority.equals( AuthorityConstants.IS_AUTHENTICATED_ANONYMOUSLY ) && ace.isGranting() ) {
                    return false;
                }
            }
        }

        /*
         * Even if the object is not private, it's parent might be and we might inherit that. Recursion happens here.
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

    /**
     * @param s
     * @param permission
     * @param authority e.g. "GROUP_JOESLAB"
     */
    private void removeGrantedAuthority( Securable object, Permission permission, String authority ) {
        MutableAcl acl = getAcl( object );

        if ( acl == null ) {
            throw new IllegalArgumentException( "makePrivate is only valid for objects that have an ACL" );
        }

        List<Integer> toremove = new Vector<Integer>();
        for ( int i = 0; i < acl.getEntries().size(); i++ ) {
            AccessControlEntry entry = acl.getEntries().get( i );

            if ( !entry.getPermission().equals( permission ) ) {
                continue;
            }

            Sid sid = entry.getSid();
            if ( sid instanceof GrantedAuthoritySid ) {

                if ( ( ( GrantedAuthoritySid ) sid ).getGrantedAuthority().equals( authority ) ) {
                    toremove.add( i );
                }
            }
        }

        if ( toremove.size() > 1 ) {
            // problem is that as you delete them, the list changes size... so the indexes don't match...have to update
            // first.
            throw new UnsupportedOperationException( "Can't deal with case of more than one ACE to remove" );
        }

        if ( toremove.isEmpty() ) {
            log.warn( "No changes, didn't remove: " + authority );
        } else {

            for ( Integer j : toremove ) {
                acl.deleteAce( j );
            }

            aclService.updateAcl( acl );
        }
    }

}