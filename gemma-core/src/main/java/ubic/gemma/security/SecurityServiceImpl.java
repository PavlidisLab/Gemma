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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.vote.AuthenticatedVoter;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.ObjectIdentityRetrievalStrategyImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.MutableAcl;
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
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.auditAndSecurity.UserGroup;
import ubic.gemma.security.authentication.UserManager;
import ubic.gemma.security.authorization.acl.AclService;
import ubic.gemma.util.AuthorityConstants;

/**
 * Methods for changing security on objects, creating and modifying groups, checking security on objects.
 * 
 * @author keshav
 * @author paul
 * @version $Id$
 */
@Service
public class SecurityServiceImpl implements SecurityService {

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
            if ( authority.getAuthority().equals( AuthorityConstants.ADMIN_GROUP_AUTHORITY ) ) {
                return true;
            }
        }
        return false;
    }

    public static boolean isRunningAsAdmin() {

        Collection<GrantedAuthority> authorities = getAuthentication().getAuthorities();
        assert authorities != null;
        for ( GrantedAuthority authority : authorities ) {
            if ( authority.getAuthority().equals( AuthorityConstants.RUN_AS_ADMIN_AUTHORITY ) ) {
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
    private AclService aclService;

    private Log log = LogFactory.getLog( SecurityServiceImpl.class );

    private ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy = new ObjectIdentityRetrievalStrategyImpl();

    @Autowired
    private SessionRegistry sessionRegistry;

    @Autowired
    private SidRetrievalStrategy sidRetrievalStrategy;

    @Autowired
    private UserManager userManager;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#addUserToGroup(java.lang.String, java.lang.String)
     */
    @Override
    public void addUserToGroup( String userName, String groupName ) {
        this.userManager.addUserToGroup( userName, groupName );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#areNonPublicButReadableByCurrentUser(java.util.Collection)
     */
    @Override
    public Map<Securable, Boolean> areNonPublicButReadableByCurrentUser( Collection<? extends Securable> securables ) {

        Map<Securable, Boolean> result = new HashMap<Securable, Boolean>();

        for ( Securable s : securables ) {
            result.put( s, false );
        }

        Collection<Securable> privateOnes = this.choosePrivate( securables );

        if ( privateOnes.isEmpty() ) return result;

        String currentUsername = userManager.getCurrentUsername();

        for ( Securable s : privateOnes ) {
            try {
                if ( this.isViewableByUser( s, currentUsername ) ) {
                    result.put( s, true );
                }
            } catch ( AccessDeniedException e ) {
                // ok
            }
        }

        return result;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#areOwnedByCurrentUser(java.util.Collection)
     */
    @Override
    public Map<Securable, Boolean> areOwnedByCurrentUser( Collection<? extends Securable> securables ) {

        Map<Securable, Boolean> result = new HashMap<Securable, Boolean>();

        Map<ObjectIdentity, Securable> objectIdentities = getObjectIdentities( securables );

        if ( objectIdentities.isEmpty() ) return result;

        /*
         * Take advantage of fast bulk loading of ACLs. Other methods sohuld adopt this if they turn out to be heavily
         * used/slow.
         */
        Map<ObjectIdentity, Acl> acls = aclService
                .readAclsById( new Vector<ObjectIdentity>( objectIdentities.keySet() ) );

        String currentUsername = userManager.getCurrentUsername();

        boolean isAdmin = isUserAdmin();

        for ( ObjectIdentity oi : acls.keySet() ) {
            Acl a = acls.get( oi );
            Sid owner = a.getOwner();

            result.put( objectIdentities.get( oi ), false );
            if ( isAdmin
                    || ( owner != null && owner instanceof PrincipalSid && ( ( PrincipalSid ) owner ).getPrincipal()
                            .equals( currentUsername ) ) ) {
                result.put( objectIdentities.get( oi ), true );
            }
        }
        return result;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#arePrivate(java.util.Collection)
     */
    @Override
    @Secured({ "ACL_SECURABLE_COLLECTION_READ" })
    public java.util.Map<Securable, Boolean> arePrivate( Collection<? extends Securable> securables ) {
        Map<Securable, Boolean> result = new HashMap<Securable, Boolean>();
        Map<ObjectIdentity, Securable> objectIdentities = getObjectIdentities( securables );

        if ( objectIdentities.isEmpty() ) return result;

        /*
         * Take advantage of fast bulk loading of ACLs. Other methods should adopt this if they turn out to be heavily
         * used/slow.
         */
        Map<ObjectIdentity, Acl> acls = aclService
                .readAclsById( new Vector<ObjectIdentity>( objectIdentities.keySet() ) );

        for ( ObjectIdentity oi : acls.keySet() ) {
            Acl a = acls.get( oi );
            boolean p = isPrivate( a );
            result.put( objectIdentities.get( oi ), p );
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#areShared(java.util.Collection)
     */
    @Override
    @Secured({ "ACL_SECURABLE_COLLECTION_READ" })
    public Map<Securable, Boolean> areShared( Collection<? extends Securable> securables ) {
        Map<Securable, Boolean> result = new HashMap<Securable, Boolean>();
        Map<ObjectIdentity, Securable> objectIdentities = getObjectIdentities( securables );

        if ( objectIdentities.isEmpty() ) return result;

        Map<ObjectIdentity, Acl> acls = aclService
                .readAclsById( new Vector<ObjectIdentity>( objectIdentities.keySet() ) );

        for ( ObjectIdentity oi : acls.keySet() ) {
            Acl a = acls.get( oi );
            boolean p = isShared( a );
            result.put( objectIdentities.get( oi ), p );
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#choosePrivate(java.util.Collection)
     */
    @Override
    public Collection<Securable> choosePrivate( Collection<? extends Securable> securables ) {
        Collection<Securable> result = new HashSet<Securable>();
        Map<Securable, Boolean> arePrivate = arePrivate( securables );

        for ( Securable s : securables ) {
            if ( arePrivate.get( s ) ) result.add( s );
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#choosePublic(java.util.Collection)
     */
    @Override
    @Secured({ "ACL_SECURABLE_COLLECTION_READ" })
    public Collection<Securable> choosePublic( Collection<? extends Securable> securables ) {
        Collection<Securable> result = new HashSet<Securable>();

        Map<Securable, Boolean> arePrivate = arePrivate( securables );

        for ( Securable s : securables ) {
            if ( !arePrivate.get( s ) ) result.add( s );
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#createGroup(java.lang.String)
     */
    @Override
    public void createGroup( String groupName ) {

        /*
         * Nice if we can get around this uniqueness constraint...but I guess it's not easy.
         */
        if ( userManager.groupExists( groupName ) ) {
            throw new IllegalArgumentException( "A group already exists with that name: " + groupName );
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
     * This will throw an error if the group has any permissions set (if there are any entries for this sid in the acl_entry table)
     * 
     * @see ubic.gemma.security.SecurityService#deleteGroup(java.lang.String)
     */
    @Override
    public void deleteGroup( String groupName ) throws DataIntegrityViolationException{

        if ( !userManager.groupExists( groupName ) ) {
            throw new IllegalArgumentException( "No group with that name: " + groupName );
        }

        /*
         * make sure this isn't one of the special groups - Administrators, Users, Agents
         */
        if ( groupName.equalsIgnoreCase( "Administrator" ) || groupName.equalsIgnoreCase( "Users" )
                || groupName.equalsIgnoreCase( "Agents" ) ) {
            throw new IllegalArgumentException( "Cannot delete that group, it is required for system operation." );
        }

        if ( !isOwnedByCurrentUser( userManager.findGroupByName( groupName ) ) ) {
            throw new IllegalArgumentException( "Only the owner of a group can delete it" );
        }
        
        String authority = getGroupAuthorityNameFromGroupName( groupName );

        userManager.deleteGroup( groupName );

        /*
         * clean up acls that use this group...do that last!
         */
        try{
            aclService.deleteSid( new GrantedAuthoritySid( authority ) );
        }catch(DataIntegrityViolationException div){
            throw div;
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#editableBy(ubic.gemma.model.common.auditAndSecurity.Securable)
     */
    @Override
    @Secured({ "ACL_SECURABLE_READ" })
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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#getAuthenticatedUserCount()
     */
    @Override
    public Integer getAuthenticatedUserCount() {
        return this.sessionRegistry.getAllPrincipals().size();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#getAuthenticatedUserNames()
     */
    @Override
    @Secured("GROUP_ADMIN")
    public Collection<String> getAuthenticatedUserNames() {
        List<Object> allPrincipals = this.sessionRegistry.getAllPrincipals();
        Collection<String> result = new HashSet<String>();
        for ( Object o : allPrincipals ) {
            result.add( o.toString() );
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#getAvailableSids()
     */
    @Override
    @Secured("GROUP_ADMIN")
    public Collection<Sid> getAvailableSids() {

        Collection<Sid> results = new HashSet<Sid>();

        Collection<String> users = userManager.findAllUsers();

        for ( String u : users ) {
            results.add( new PrincipalSid( u ) );
        }

        Collection<String> groups = userManager.findAllGroups();

        for ( String g : groups ) {
            List<GrantedAuthority> ga = userManager.findGroupAuthorities( g );
            for ( GrantedAuthority grantedAuthority : ga ) {
                results.add( new GrantedAuthoritySid( grantedAuthority.getAuthority() ) );
            }
        }

        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#getGroupsEditableBy(java.util.Collection)
     */
    @Override
    @Secured({ "ACL_SECURABLE_COLLECTION_READ" })
    public Map<Securable, Collection<String>> getGroupsEditableBy( Collection<? extends Securable> securables ) {
        Collection<String> groupNames = getGroupsUserCanView();
        Map<Securable, Collection<String>> result = new HashMap<Securable, Collection<String>>();

        List<Permission> write = new ArrayList<Permission>();
        write.add( BasePermission.WRITE );

        List<Permission> admin = new ArrayList<Permission>();
        admin.add( BasePermission.ADMINISTRATION );

        for ( String groupName : groupNames ) {
            Map<Securable, Boolean> groupHasPermission = this.groupHasPermission( securables, write, groupName );

            populateGroupsEditableBy( result, groupName, groupHasPermission );

            groupHasPermission = this.groupHasPermission( securables, admin, groupName );

            populateGroupsEditableBy( result, groupName, groupHasPermission );

        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#getGroupsEditableBy(ubic.gemma.model.common.auditAndSecurity.Securable)
     */
    @Override
    @Secured({ "ACL_SECURABLE_READ" })
    public Collection<String> getGroupsEditableBy( Securable s ) {
        Collection<String> groupNames = getGroupsUserCanView();

        Collection<String> result = new HashSet<String>();

        for ( String string : groupNames ) {
            if ( this.isEditableByGroup( s, string ) ) {
                result.add( string );
            }
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#getGroupsReadableBy(java.util.Collection)
     */
    @Override
    @Secured({ "ACL_SECURABLE_COLLECTION_READ" })
    public Map<Securable, Collection<String>> getGroupsReadableBy( Collection<? extends Securable> securables ) {

        Map<Securable, Collection<String>> result = new HashMap<Securable, Collection<String>>();

        if ( securables.isEmpty() ) return result;

        Collection<String> groupNames = getGroupsUserCanView();

        List<Permission> read = new ArrayList<Permission>();
        read.add( BasePermission.READ );

        List<Permission> admin = new ArrayList<Permission>();
        admin.add( BasePermission.ADMINISTRATION );

        for ( String groupName : groupNames ) {
            Map<Securable, Boolean> groupHasPermission = this.groupHasPermission( securables, read, groupName );

            populateGroupsEditableBy( result, groupName, groupHasPermission );

            groupHasPermission = this.groupHasPermission( securables, admin, groupName );

            populateGroupsEditableBy( result, groupName, groupHasPermission );
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#getGroupsReadableBy(ubic.gemma.model.common.auditAndSecurity.Securable)
     */
    @Override
    @Secured({ "ACL_SECURABLE_READ" })
    public Collection<String> getGroupsReadableBy( Securable s ) {
        Collection<String> groupNames = getGroupsUserCanView();

        Collection<String> result = new HashSet<String>();

        for ( String string : groupNames ) {
            if ( this.isReadableByGroup( s, string ) ) {
                result.add( string );
            }
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#getGroupsUserCanEdit(java.lang.String)
     */
    @Override
    public Collection<String> getGroupsUserCanEdit( String userName ) {
        Collection<String> groupNames = getGroupsUserCanView();

        Collection<String> result = new HashSet<String>();
        for ( String gname : groupNames ) {
            UserGroup g = userManager.findGroupByName( gname );
            if ( this.isEditableByUser( g, userName ) ) {
                result.add( gname );
            }
        }

        return result;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#getOwner(ubic.gemma.model.common.auditAndSecurity.Securable)
     */
    @Override
    @Secured("ACL_SECURABLE_READ")
    public Sid getOwner( Securable s ) {
        ObjectIdentity oi = this.objectIdentityRetrievalStrategy.getObjectIdentity( s );
        Acl a = this.aclService.readAclById( oi );
        return a.getOwner();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#getOwners(java.util.Collection)
     */
    @Override
    @Secured("ACL_SECURABLE_COLLECTION_READ")
    public Map<Securable, Sid> getOwners( Collection<? extends Securable> securables ) {
        Map<Securable, Sid> result = new HashMap<Securable, Sid>();
        Map<ObjectIdentity, Securable> objectIdentities = getObjectIdentities( securables );

        if ( securables.isEmpty() ) return result;

        /*
         * Take advantage of fast bulk loading of ACLs. Other methods sohuld adopt this if they turn out to be heavily
         * used/slow.
         */
        Map<ObjectIdentity, Acl> acls = aclService
                .readAclsById( new Vector<ObjectIdentity>( objectIdentities.keySet() ) );

        for ( ObjectIdentity oi : acls.keySet() ) {
            Acl a = acls.get( oi );
            Sid owner = a.getOwner();
            if ( owner == null )
                result.put( objectIdentities.get( oi ), null );
            else
                result.put( objectIdentities.get( oi ), owner );
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#isEditable(ubic.gemma.model.common.auditAndSecurity.Securable)
     */
    @Override
    @Secured("ACL_SECURABLE_READ")
    public boolean isEditable( Securable s ) {

        if ( !isUserLoggedIn() ) {
            return false;
        }

        String currentUser = this.userManager.getCurrentUsername();

        List<Permission> requiredPermissions = new ArrayList<Permission>();

        requiredPermissions.add( BasePermission.WRITE );
        if ( hasPermission( s, requiredPermissions, currentUser ) ) {
            return true;
        }

        requiredPermissions.clear();
        requiredPermissions.add( BasePermission.ADMINISTRATION );
        return hasPermission( s, requiredPermissions, currentUser );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#isEditableByGroup(ubic.gemma.model.common.auditAndSecurity.Securable,
     * java.lang.String)
     */
    @Override
    @Secured("ACL_SECURABLE_READ")
    public boolean isEditableByGroup( Securable s, String groupName ) {
        List<Permission> requiredPermissions = new ArrayList<Permission>();
        requiredPermissions.add( BasePermission.WRITE );

        if ( groupHasPermission( s, requiredPermissions, groupName ) ) {
            return true;
        }

        requiredPermissions.clear();
        requiredPermissions.add( BasePermission.ADMINISTRATION );
        return groupHasPermission( s, requiredPermissions, groupName );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#isEditableByUser(ubic.gemma.model.common.auditAndSecurity.Securable,
     * java.lang.String)
     */
    @Override
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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#isOwnedByCurrentUser(ubic.gemma.model.common.auditAndSecurity.Securable)
     */
    @Override
    public boolean isOwnedByCurrentUser( Securable s ) {
        ObjectIdentity oi = objectIdentityRetrievalStrategy.getObjectIdentity( s );

        try {
            Acl acl = this.aclService.readAclById( oi );

            Sid owner = acl.getOwner();
            if ( owner == null ) return false;

            if ( owner instanceof PrincipalSid ) {
                return ( ( PrincipalSid ) owner ).getPrincipal().equals( userManager.getCurrentUsername() );
            }

            /*
             * Special case: if we're the administrator, and the owner of the data is GROUP_ADMIN, we are considered the
             * owner.
             */
            return owner instanceof GrantedAuthoritySid
                    && isUserAdmin()
                    && ( ( GrantedAuthoritySid ) owner ).getGrantedAuthority().equals(
                            AuthorityConstants.ADMIN_GROUP_AUTHORITY );

        } catch ( NotFoundException nfe ) {
            return false;
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#isPrivate(ubic.gemma.model.common.auditAndSecurity.Securable)
     */
    @Override
    public boolean isPrivate( Securable s ) {

        if ( s == null ) {
            log.warn( "Null object: considered public!" );
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

            assert acl != null;

            return isPrivate( acl );
        } catch ( NotFoundException nfe ) {
            return true;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#isPublic(ubic.gemma.model.common.auditAndSecurity.Securable)
     */
    @Override
    public boolean isPublic( Securable s ) {
        return !isPrivate( s );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#isReadableByGroup(ubic.gemma.model.common.auditAndSecurity.Securable,
     * java.lang.String)
     */
    @Override
    @Secured("ACL_SECURABLE_READ")
    public boolean isReadableByGroup( Securable s, String groupName ) {
        List<Permission> requiredPermissions = new ArrayList<Permission>();
        requiredPermissions.add( BasePermission.READ );

        if ( groupHasPermission( s, requiredPermissions, groupName ) ) {
            return true;
        }

        requiredPermissions.clear();
        requiredPermissions.add( BasePermission.ADMINISTRATION );
        return groupHasPermission( s, requiredPermissions, groupName );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#isShared(ubic.gemma.model.common.auditAndSecurity.Securable)
     */
    @Override
    public boolean isShared( Securable s ) {
        if ( s == null ) {
            return false;
        }

        /*
         * Implementation note: this code mimics AclEntryVoter.vote, but in adminsitrative mode so no auditing etc
         * happens.
         */

        List<Permission> perms = new Vector<Permission>();
        perms.add( BasePermission.READ );

        ObjectIdentity oi = objectIdentityRetrievalStrategy.getObjectIdentity( s );

        /*
         * Note: in theory, it should pay attention to the sid we ask for and return nothing if there is no acl.
         * However, the implementation actually ignores the sid argument. See BasicLookupStrategy
         */
        try {
            Acl acl = this.aclService.readAclById( oi );

            return isShared( acl );
        } catch ( NotFoundException nfe ) {
            return true;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#isViewableByUser(ubic.gemma.model.common.auditAndSecurity.Securable,
     * java.lang.String)
     */
    @Override
    @Secured({ "ACL_SECURABLE_READ" })
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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#makeOwnedByUser(ubic.gemma.model.common.auditAndSecurity.Securable,
     * java.lang.String)
     */
    @Override
    @Secured("GROUP_ADMIN")
    public void makeOwnedByUser( Securable s, String userName ) {
        MutableAcl acl = getAcl( s );

        Sid owner = acl.getOwner();
        if ( owner != null && owner instanceof PrincipalSid
                && ( ( PrincipalSid ) owner ).getPrincipal().equals( userName ) ) {
            /*
             * Already owned by the given user -- note we don't check if the user exists here.
             */
            return;
        }

        // make sure user exists and is enabled.
        UserDetails user = this.userManager.loadUserByUsername( userName );
        if ( !user.isEnabled() || !user.isAccountNonExpired() || !user.isAccountNonLocked() ) {
            throw new IllegalArgumentException( "User  " + userName + " has a disabled account" );
        }

        acl.setOwner( new PrincipalSid( userName ) );
        aclService.updateAcl( acl );

        /*
         * FIXME: I don't know if these are necessary if you are the owner.
         */
        addPrincipalAuthority( s, BasePermission.WRITE, userName );
        addPrincipalAuthority( s, BasePermission.READ, userName );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#makePrivate(java.util.Collection)
     */
    @Override
    public void makePrivate( Collection<? extends Securable> objs ) {
        for ( Securable s : objs ) {
            makePrivate( s );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#makePrivate(ubic.gemma.model.common.auditAndSecurity.Securable)
     */
    @Override
    @Secured("ACL_SECURABLE_EDIT")
    public void makePrivate( Securable object ) {
        if ( object == null ) {
            return;
        }

        if ( isPrivate( object ) ) {
            log.warn( "Object is already private: " + object );
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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#makePublic(java.util.Collection)
     */
    @Override
    public void makePublic( Collection<? extends Securable> objs ) {
        for ( Securable s : objs ) {
            makePublic( s );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#makePublic(ubic.gemma.model.common.auditAndSecurity.Securable)
     */
    @Override
    @Secured("ACL_SECURABLE_EDIT")
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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#makeReadableByGroup(ubic.gemma.model.common.auditAndSecurity.Securable,
     * java.lang.String)
     */
    @Override
    @Secured("ACL_SECURABLE_EDIT")
    public void makeReadableByGroup( Securable s, String groupName ) throws AccessDeniedException {

        if ( StringUtils.isBlank( groupName ) ) {
            throw new IllegalArgumentException( "'group' cannot be null" );
        }

        Collection<String> groups = checkForGroupAccessByCurrentuser( groupName );

        if ( !groups.contains( groupName ) && !isUserAdmin() ) {
            throw new AccessDeniedException( "User doesn't have access to that group" );
        }

        if ( isReadableByGroup( s, groupName ) ) {
            return;
        }

        addGroupAuthority( s, BasePermission.READ, groupName );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.security.SecurityService#makeUnreadableByGroup(ubic.gemma.model.common.auditAndSecurity.Securable,
     * java.lang.String)
     */
    @Override
    @Secured("ACL_SECURABLE_EDIT")
    public void makeUnreadableByGroup( Securable s, String groupName ) throws AccessDeniedException {

        if ( StringUtils.isBlank( groupName ) ) {
            throw new IllegalArgumentException( "'group' cannot be null" );
        }

        removeGrantedAuthority( s, BasePermission.READ, getGroupAuthorityNameFromGroupName( groupName ) );
        removeGrantedAuthority( s, BasePermission.WRITE, getGroupAuthorityNameFromGroupName( groupName ) );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.security.SecurityService#makeUnwriteableByGroup(ubic.gemma.model.common.auditAndSecurity.Securable,
     * java.lang.String)
     */
    @Override
    @Secured("ACL_SECURABLE_EDIT")
    public void makeUnwriteableByGroup( Securable s, String groupName ) throws AccessDeniedException {

        if ( StringUtils.isBlank( groupName ) ) {
            throw new IllegalArgumentException( "'group' cannot be null" );
        }

        removeGrantedAuthority( s, BasePermission.WRITE, getGroupAuthorityNameFromGroupName( groupName ) );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#makeWriteableByGroup(ubic.gemma.model.common.auditAndSecurity.Securable,
     * java.lang.String)
     */
    @Override
    @Secured("ACL_SECURABLE_EDIT")
    public void makeWriteableByGroup( Securable s, String groupName ) throws AccessDeniedException {

        if ( StringUtils.isBlank( groupName ) ) {
            throw new IllegalArgumentException( "'group' cannot be null" );
        }

        Collection<String> groups = checkForGroupAccessByCurrentuser( groupName );

        if ( !groups.contains( groupName ) && !isUserAdmin() ) {
            throw new AccessDeniedException( "User doesn't have access to that group" );
        }

        if ( isEditableByGroup( s, groupName ) ) {
            return;
        }
        // Bug 1835: Duplicate ACLS were added to an object for group read access as part of writable
        // only add read access if not there already.

        if ( !( isReadableByGroup( s, groupName ) ) ) {
            addGroupAuthority( s, BasePermission.READ, groupName );
        }
        addGroupAuthority( s, BasePermission.WRITE, groupName );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#readableBy(ubic.gemma.model.common.auditAndSecurity.Securable)
     */
    @Override
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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#removeUserFromGroup(java.lang.String, java.lang.String)
     */
    @Override
    public void removeUserFromGroup( String userName, String groupName ) {
        this.userManager.removeUserFromGroup( userName, groupName );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#setOwner(ubic.gemma.model.common.auditAndSecurity.Securable,
     * java.lang.String)
     */
    @Override
    @Secured("GROUP_ADMIN")
    public void setOwner( Securable s, String userName ) {

        // make sure user exists and is enabled.
        UserDetails user = this.userManager.loadUserByUsername( userName );
        if ( !user.isEnabled() || !user.isAccountNonExpired() || !user.isAccountNonLocked() ) {
            throw new IllegalArgumentException( "User  " + userName + " has a disabled account" );
        }

        ObjectIdentity oi = this.objectIdentityRetrievalStrategy.getObjectIdentity( s );
        MutableAcl a = ( MutableAcl ) this.aclService.readAclById( oi );

        a.setOwner( new PrincipalSid( userName ) );

        this.aclService.updateAcl( a );

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
     * Check if the current user can access the given group.
     * 
     * @param groupName
     * @return
     */
    private Collection<String> checkForGroupAccessByCurrentuser( String groupName ) {
        if ( groupName.equals( AuthorityConstants.ADMIN_GROUP_NAME ) ) {
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

    /**
     * From the group name get the authority which should be underscored with GROUP_
     * 
     * @param The group name e.g. fish
     * @return The authority e.g. GROUP_FISH_...
     */
    private String getGroupAuthorityNameFromGroupName( String groupName ) {
        Collection<String> groups = checkForGroupAccessByCurrentuser( groupName );

        if ( !groups.contains( groupName ) && !isUserAdmin() ) {
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
        String authority = userManager.getRolePrefix() + ( ga.getAuthority() );
        return authority;
    }

    /**
     * @return
     */
    private Collection<String> getGroupsUserCanView() {
        Collection<String> groupNames;
        try {
            // administrator...
            groupNames = userManager.findAllGroups();
        } catch ( AccessDeniedException e ) {
            groupNames = userManager.findGroupsForUser( userManager.getCurrentUsername() );
        }
        return groupNames;
    }

    /**
     * @param securables
     * @return
     */
    private Map<ObjectIdentity, Securable> getObjectIdentities( Collection<? extends Securable> securables ) {
        Map<ObjectIdentity, Securable> result = new HashMap<ObjectIdentity, Securable>();
        for ( Securable s : securables ) {
            result.put( objectIdentityRetrievalStrategy.getObjectIdentity( s ), s );
        }
        return result;
    }

    private Map<Securable, Boolean> groupHasPermission( Collection<? extends Securable> securables,
            List<Permission> requiredPermissions, String groupName ) {
        Map<Securable, Boolean> result = new HashMap<Securable, Boolean>();
        Map<ObjectIdentity, Securable> objectIdentities = getObjectIdentities( securables );

        List<GrantedAuthority> auths = userManager.findGroupAuthorities( groupName );

        List<Sid> sids = new ArrayList<Sid>();
        for ( GrantedAuthority a : auths ) {
            GrantedAuthoritySid sid = new GrantedAuthoritySid( new GrantedAuthorityImpl( userManager.getRolePrefix()
                    + a.getAuthority() ) );
            sids.add( sid );
        }

        Map<ObjectIdentity, Acl> acls = aclService
                .readAclsById( new Vector<ObjectIdentity>( objectIdentities.keySet() ) );

        for ( ObjectIdentity oi : acls.keySet() ) {
            Acl a = acls.get( oi );
            try {
                result.put( objectIdentities.get( oi ), a.isGranted( requiredPermissions, sids, true ) );
            } catch ( NotFoundException ignore ) {
            }
        }
        return result;
    }

    /**
     * @param domainObject
     * @param requiredPermissions
     * @param groupName
     * @return
     */
    private boolean groupHasPermission( Securable domainObject, List<Permission> requiredPermissions, String groupName ) {
        ObjectIdentity objectIdentity = objectIdentityRetrievalStrategy.getObjectIdentity( domainObject );

        List<GrantedAuthority> auths = userManager.findGroupAuthorities( groupName );

        List<Sid> sids = new ArrayList<Sid>();
        for ( GrantedAuthority a : auths ) {
            GrantedAuthoritySid sid = new GrantedAuthoritySid( new GrantedAuthorityImpl( userManager.getRolePrefix()
                    + a.getAuthority() ) );
            sids.add( sid );
        }

        try {
            // Lookup only ACLs for SIDs we're interested in (this actually get them all)
            Acl acl = aclService.readAclById( objectIdentity, sids );
            // administrative mode = true
            return acl.isGranted( requiredPermissions, sids, true );
        } catch ( NotFoundException ignore ) {
            return false;
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
        Authentication authentication = new UsernamePasswordAuthenticationToken( userName, user.getPassword(),
                user.getAuthorities() );
        List<Sid> sids = sidRetrievalStrategy.getSids( authentication );

        Acl acl = null;

        try {
            // Lookup only ACLs for SIDs we're interested in (this actually get them all)
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
         * If the given Acl has anonymous permissions on it, then it can't be private.
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
     * @param acl
     * @return true if the ACL grants READ authority to at least one group that is not admin or agent.
     */
    private boolean isShared( Acl acl ) {
        for ( AccessControlEntry ace : acl.getEntries() ) {

            if ( !ace.getPermission().equals( BasePermission.READ ) ) continue;

            Sid sid = ace.getSid();
            if ( sid instanceof GrantedAuthoritySid ) {
                String grantedAuthority = ( ( GrantedAuthoritySid ) sid ).getGrantedAuthority();
                if ( grantedAuthority.startsWith( "GROUP_" ) && ace.isGranting() ) {

                    if ( grantedAuthority.equals( AuthorityConstants.AGENT_GROUP_AUTHORITY )
                            || grantedAuthority.equals( AuthorityConstants.ADMIN_GROUP_AUTHORITY ) ) {
                        continue;
                    }
                    return true;

                }
            }
        }

        /*
         * Even if the object is not private, it's parent might be and we might inherit that. Recursion happens here.
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

    private void populateGroupsEditableBy( Map<Securable, Collection<String>> result, String groupName,
            Map<Securable, Boolean> groupHasPermission ) {
        for ( Securable s : groupHasPermission.keySet() ) {
            if ( groupHasPermission.get( s ) ) {
                if ( !result.containsKey( s ) ) {
                    result.put( s, new HashSet<String>() );
                }
                result.get( s ).add( groupName );
            }

        }
    }

    /**
     * Wrapper method that calls removeOneGrantedAuthority to ensure that only one acl at a time is updated. A bit
     * clunky but it ensures that the code is called as a complete unit, that is an update is performed and the array
     * retrieved again after update.
     * 
     * @param The object to remove the permissions from
     * @param permission Permission to change.
     * @param authority e.g. "GROUP_JOESLAB"
     */
    private void removeGrantedAuthority( Securable object, Permission permission, String authority ) {
        int numberOfAclsToRemove = 1;
        // for 0 or 1 acls should only call once
        for ( int i = 0; i < numberOfAclsToRemove; i++ ) {
            log.info( "Removing acl from " + authority );
            numberOfAclsToRemove = removeOneGrantedAuthority( object, permission, authority );
        }

    }

    /**
     * Method removes just one acl and then informs calling method the number of acls to remove
     * 
     * @param object The object to remove the permissions from
     * @param permission The permission to remove
     * @param authority e.g. "GROUP_JOESLAB"
     * @return Number of acl records that need removing
     */
    private int removeOneGrantedAuthority( Securable object, Permission permission, String authority ) {
        int numberAclsToRemove = 0;

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

        if ( toremove.isEmpty() ) {
            log.warn( "No changes, didn't remove: " + authority );
        } else if ( toremove.size() >= 1 ) {

            numberAclsToRemove = toremove.size();
            // take the first acl
            acl.deleteAce( toremove.iterator().next() );
            aclService.updateAcl( acl );
        }

        return numberAclsToRemove;

    }

}