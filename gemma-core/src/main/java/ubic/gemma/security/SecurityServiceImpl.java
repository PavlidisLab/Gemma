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

import gemma.gsec.AuthorityConstants;
import gemma.gsec.SecurityService;
import gemma.gsec.acl.ValueObjectAwareIdentityRetrievalStrategyImpl;
import gemma.gsec.acl.domain.AclGrantedAuthoritySid;
import gemma.gsec.acl.domain.AclPrincipalSid;
import gemma.gsec.acl.domain.AclService;
import gemma.gsec.model.Securable;
import gemma.gsec.model.SecureValueObject;
import gemma.gsec.util.SecurityUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.vote.AuthenticatedVoter;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.ObjectIdentityRetrievalStrategy;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.acls.model.SidRetrievalStrategy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ubic.gemma.model.common.auditAndSecurity.UserGroup;
import ubic.gemma.security.authentication.UserManager;

/**
 * Methods for changing security on objects, creating and modifying groups, checking security on objects.
 * 
 * @author keshav
 * @author paul
 * @version $Id$
 */
@Service
@Transactional
public class SecurityServiceImpl implements SecurityService {

    @Autowired
    private AclService aclService;

    private Log log = LogFactory.getLog( SecurityServiceImpl.class );

    private ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy = new ValueObjectAwareIdentityRetrievalStrategyImpl();

    @Autowired
    private SessionRegistry sessionRegistry;

    @Autowired
    private SidRetrievalStrategy sidRetrievalStrategy;

    @Autowired
    private UserManager userManager;

    // @Autowired
    // private UserService userService;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#addUserToGroup(java.lang.String, java.lang.String)
     */
    @Override
    @Transactional
    public void addUserToGroup( String userName, String groupName ) {
        this.userManager.addUserToGroup( userName, groupName );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#areNonPublicButReadableByCurrentUser(java.util.Collection)
     */
    @Override
    public <T extends Securable> Map<T, Boolean> areNonPublicButReadableByCurrentUser( Collection<T> securables ) {

        Map<T, Boolean> result = new HashMap<T, Boolean>();

        for ( T s : securables ) {
            result.put( s, false );
        }

        Collection<T> privateOnes = this.choosePrivate( securables );

        if ( privateOnes.isEmpty() ) return result;

        String currentUsername = userManager.getCurrentUsername();

        for ( T s : privateOnes ) {
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
    public <T extends Securable> Map<T, Boolean> areOwnedByCurrentUser( Collection<T> securables ) {

        Map<T, Boolean> result = new HashMap<T, Boolean>();

        Map<ObjectIdentity, T> objectIdentities = getObjectIdentities( securables );

        if ( objectIdentities.isEmpty() ) return result;

        /*
         * Take advantage of fast bulk loading of ACLs. Other methods sohuld adopt this if they turn out to be heavily
         * used/slow.
         */
        Map<ObjectIdentity, Acl> acls = aclService
                .readAclsById( new Vector<ObjectIdentity>( objectIdentities.keySet() ) );

        String currentUsername = userManager.getCurrentUsername();

        boolean isAdmin = SecurityUtil.isUserAdmin();

        for ( ObjectIdentity oi : acls.keySet() ) {
            Acl a = acls.get( oi );
            Sid owner = a.getOwner();

            result.put( objectIdentities.get( oi ), false );
            if ( isAdmin
                    || ( owner != null && owner instanceof AclPrincipalSid && ( ( AclPrincipalSid ) owner )
                            .getPrincipal().equals( currentUsername ) ) ) {
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
    public <T extends Securable> Map<T, Boolean> arePrivate( Collection<T> securables ) {
        Map<T, Boolean> result = new HashMap<T, Boolean>();
        Map<ObjectIdentity, T> objectIdentities = getObjectIdentities( securables );

        if ( objectIdentities.isEmpty() ) return result;

        /*
         * Take advantage of fast bulk loading of ACLs. Other methods should adopt this if they turn out to be heavily
         * used/slow.
         */
        Map<ObjectIdentity, Acl> acls = aclService
                .readAclsById( new Vector<ObjectIdentity>( objectIdentities.keySet() ) );

        for ( ObjectIdentity oi : acls.keySet() ) {
            Acl a = acls.get( oi );
            boolean p = SecurityUtil.isPrivate( a );
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
    public <T extends Securable> Map<T, Boolean> areShared( Collection<T> securables ) {
        Map<T, Boolean> result = new HashMap<T, Boolean>();
        Map<ObjectIdentity, T> objectIdentities = getObjectIdentities( securables );

        if ( objectIdentities.isEmpty() ) return result;

        Map<ObjectIdentity, Acl> acls = aclService
                .readAclsById( new Vector<ObjectIdentity>( objectIdentities.keySet() ) );

        for ( ObjectIdentity oi : acls.keySet() ) {
            Acl a = acls.get( oi );
            boolean p = SecurityUtil.isShared( a );
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
    public <T extends Securable> Collection<T> choosePrivate( Collection<T> securables ) {
        Collection<T> result = new HashSet<>();
        Map<T, Boolean> arePrivate = arePrivate( securables );

        for ( T s : securables ) {
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
    public <T extends Securable> Collection<T> choosePublic( Collection<T> securables ) {
        Collection<T> result = new HashSet<>();

        Map<T, Boolean> arePrivate = arePrivate( securables );

        for ( T s : securables ) {
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
        auths.add( new SimpleGrantedAuthority( groupAuthority ) );

        this.userManager.createGroup( groupName, auths );
        addUserToGroup( userManager.getCurrentUsername(), groupName );

        // make sure all current and future members of the group will be able to see the group
        // UserGroup group = userService.findGroupByName( groupName );
        // if ( group != null ) { // really shouldn't be null
        // this should be done by the AclAdvice. We can't do it here because permissions aren't yet set up!
        // this.makeReadableByGroup( group, group.getName() );
        // } else {
        // log.error(
        // "Loading group that was just created failed. Read permissions were not granted to group, see bug 2840." );
        // }

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

    /**
     * @param s
     * @return
     */
    @Override
    public MutableAcl getAcl( Securable s ) {
        ObjectIdentity oi = objectIdentityRetrievalStrategy.getObjectIdentity( s );

        try {
            return ( MutableAcl ) aclService.readAclById( oi );
        } catch ( NotFoundException e ) {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#getAcls(java.util.Collection)
     */
    @Override
    public <T extends Securable> Map<T, Acl> getAcls( Collection<T> securables ) {
        if ( securables.isEmpty() ) {
            throw new IllegalArgumentException( "Must provide securables" );
        }

        Map<ObjectIdentity, T> objectIdentities = getObjectIdentities( securables );

        assert !objectIdentities.isEmpty();

        /*
         * Take advantage of fast bulk loading of ACLs.
         */
        Map<ObjectIdentity, Acl> acls = aclService
                .readAclsById( new Vector<ObjectIdentity>( objectIdentities.keySet() ) );

        Map<T, Acl> result = new HashMap<>();
        for ( ObjectIdentity o : acls.keySet() ) {
            T se = objectIdentities.get( o );
            assert se != null;
            result.put( se, acls.get( o ) );
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
            results.add( new AclPrincipalSid( u ) );
        }

        Collection<String> groups = userManager.findAllGroups();

        for ( String g : groups ) {
            List<GrantedAuthority> ga = userManager.findGroupAuthorities( g );
            for ( GrantedAuthority grantedAuthority : ga ) {
                results.add( new AclGrantedAuthoritySid( grantedAuthority.getAuthority() ) );
            }
        }

        return results;
    }

    /**
     * From the group name get the authority which should be underscored with GROUP_
     * 
     * @param The group name e.g. fish
     * @return The authority e.g. GROUP_FISH_...
     */
    @Override
    public String getGroupAuthorityNameFromGroupName( String groupName ) {
        Collection<String> groups = checkForGroupAccessByCurrentuser( groupName );

        if ( !groups.contains( groupName ) && !SecurityUtil.isUserAdmin() ) {
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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#getGroupsEditableBy(java.util.Collection)
     */
    @Override
    @Secured({ "ACL_SECURABLE_COLLECTION_READ" })
    public <T extends Securable> Map<T, Collection<String>> getGroupsEditableBy( Collection<T> securables ) {
        Collection<String> groupNames = getGroupsUserCanView();

        Map<T, Collection<String>> result = new HashMap<T, Collection<String>>();

        List<Permission> write = new ArrayList<Permission>();
        write.add( BasePermission.WRITE );

        List<Permission> admin = new ArrayList<Permission>();
        admin.add( BasePermission.ADMINISTRATION );

        for ( String groupName : groupNames ) {
            Map<T, Boolean> groupHasPermission = this.groupHasPermission( securables, write, groupName );

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
    public <T extends Securable> Map<T, Collection<String>> getGroupsReadableBy( Collection<T> securables ) {

        Map<T, Collection<String>> result = new HashMap<T, Collection<String>>();

        if ( securables.isEmpty() ) return result;

        Collection<String> groupNames = getGroupsUserCanView();

        List<Permission> read = new ArrayList<Permission>();
        read.add( BasePermission.READ );

        List<Permission> admin = new ArrayList<Permission>();
        admin.add( BasePermission.ADMINISTRATION );

        for ( String groupName : groupNames ) {
            Map<T, Boolean> groupHasPermission = this.groupHasPermission( securables, read, groupName );

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
    public <T extends Securable> Map<T, Sid> getOwners( Collection<T> securables ) {
        Map<T, Sid> result = new HashMap<>();
        Map<ObjectIdentity, T> objectIdentities = getObjectIdentities( securables );

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
     * @see gemma.gsec.SecurityService#hasPermission(java.util.List, java.util.List,
     * org.springframework.security.core.Authentication)
     */
    @Override
    public <T extends Securable> List<Boolean> hasPermission( List<T> svos, List<Permission> requiredPermissions,
            Authentication authentication ) {

        List<Boolean> result = new ArrayList<Boolean>();

        if ( svos.isEmpty() ) return result;

        Map<ObjectIdentity, T> objectIdentities = getObjectIdentities( svos );

        /*
         * Take advantage of fast bulk loading of ACLs.
         */
        Map<ObjectIdentity, Acl> acls = aclService
                .readAclsById( new Vector<ObjectIdentity>( objectIdentities.keySet() ) );

        assert !acls.isEmpty();

        List<Sid> sids = sidRetrievalStrategy.getSids( authentication );

        assert !sids.isEmpty();

        for ( T s : svos ) {
            // yes, we have to do it again.
            ObjectIdentity oi = objectIdentityRetrievalStrategy.getObjectIdentity( s );
            Acl acl = acls.get( oi );

            try {
                boolean granted = acl.isGranted( requiredPermissions, sids, false );

                result.add( granted );
            } catch ( NotFoundException ignore ) { // this won't happen?
                /*
                 * The user is anonymous.
                 */
                result.add( false );
            }
        }

        assert result.size() == svos.size();

        return result;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#hasPermission(java.util.Collection, java.util.List,
     * org.springframework.security.core.Authentication)
     */
    @Override
    public Map<SecureValueObject, Boolean> hasPermissionVO( Collection<SecureValueObject> svos,
            List<Permission> requiredPermissions, Authentication authentication ) {

        Map<SecureValueObject, Boolean> result = new HashMap<SecureValueObject, Boolean>();

        if ( svos.isEmpty() ) return result;

        Map<ObjectIdentity, SecureValueObject> objectIdentities = getObjectIdentities( svos );

        /*
         * Take advantage of fast bulk loading of ACLs.
         */

        Map<ObjectIdentity, Acl> acls = aclService
                .readAclsById( new Vector<ObjectIdentity>( objectIdentities.keySet() ) );

        assert !acls.isEmpty();

        List<Sid> sids = sidRetrievalStrategy.getSids( authentication );

        assert !sids.isEmpty();

        for ( ObjectIdentity oi : acls.keySet() ) {
            Acl acl = acls.get( oi );

            try {
                boolean granted = acl.isGranted( requiredPermissions, sids, false );

                result.put( objectIdentities.get( oi ), granted );
            } catch ( NotFoundException ignore ) { // this won't happen?
                /*
                 * The user is anonymous.
                 */
                result.put( objectIdentities.get( oi ), false );
            }
        }
        return result;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.security.SecurityService#hasPermission(ubic.gemma.model.common.auditAndSecurity.SecureValueObject,
     * java.util.List, org.springframework.security.core.Authentication)
     */
    @Override
    public boolean hasPermissionVO( SecureValueObject svo, List<Permission> requiredPermissions,
            Authentication authentication ) {

        List<Sid> sids = sidRetrievalStrategy.getSids( authentication );

        Acl acl = null;

        ObjectIdentity objectIdentity = objectIdentityRetrievalStrategy.getObjectIdentity( svo );

        try {
            // Lookup only ACLs for SIDs we're interested in (this actually get them all)
            acl = aclService.readAclById( objectIdentity, sids );
            // administrative mode = false
            return acl.isGranted( requiredPermissions, sids, false );
        } catch ( NotFoundException ignore ) {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#isEditable(ubic.gemma.model.common.auditAndSecurity.Securable)
     */
    @Override
    @Secured("ACL_SECURABLE_READ")
    public boolean isEditable( Securable s ) {

        if ( !SecurityUtil.isUserLoggedIn() ) {
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

            /*
             * Special case: if we're the administrator, and the owner of the data is GROUP_ADMIN, we are considered the
             * owner.
             */
            if ( owner instanceof AclGrantedAuthoritySid
                    && SecurityUtil.isUserAdmin()
                    && ( ( AclGrantedAuthoritySid ) owner ).getGrantedAuthority().equals(
                            AuthorityConstants.ADMIN_GROUP_AUTHORITY ) ) {
                return true;
            }

            if ( owner instanceof AclPrincipalSid ) {
                String ownerName = ( ( AclPrincipalSid ) owner ).getPrincipal();

                if ( ownerName.equals( userManager.getCurrentUsername() ) ) {
                    return true;
                }

                /*
                 * Special case: if the owner is an administrator, and we're an administrator, we are considered the
                 * owner. Note that the intention is that usually the owner would be a GrantedAuthority (see last case,
                 * below), not a Principal, but this hasn't always been instituted.
                 */
                if ( SecurityUtil.isUserAdmin() ) {
                    Collection<? extends GrantedAuthority> authorities = userManager.loadUserByUsername( ownerName )
                            .getAuthorities();
                    for ( GrantedAuthority grantedAuthority : authorities ) {
                        if ( grantedAuthority.getAuthority().equals( AuthorityConstants.ADMIN_GROUP_AUTHORITY ) ) {
                            return true;
                        }
                    }

                    return false;
                }

            }

            return false;

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

        Sid anonSid = new AclGrantedAuthoritySid( new SimpleGrantedAuthority(
                AuthenticatedVoter.IS_AUTHENTICATED_ANONYMOUSLY ) );

        List<Sid> sids = new Vector<Sid>();
        sids.add( anonSid );

        ObjectIdentity oi = objectIdentityRetrievalStrategy.getObjectIdentity( s );

        /*
         * Note: in theory, it should pay attention to the sid we ask for and return nothing if there is no acl.
         * However, the implementation actually ignores the sid argument.
         */
        try {
            Acl acl = this.aclService.readAclById( oi, sids );

            assert acl != null;

            return SecurityUtil.isPrivate( acl );
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

            return SecurityUtil.isShared( acl );
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
        if ( owner != null && owner instanceof AclPrincipalSid
                && ( ( AclPrincipalSid ) owner ).getPrincipal().equals( userName ) ) {
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

        acl.setOwner( new AclPrincipalSid( userName ) );
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
    @Transactional
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
    @Transactional
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

        // will fail until flush...
        if ( isPublic( object ) ) {
            // throw new IllegalStateException( "Failed to make object private: " + object );
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.SecurityService#makePublic(java.util.Collection)
     */
    @Override
    @Transactional
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

        acl.insertAce( acl.getEntries().size(), BasePermission.READ, new AclGrantedAuthoritySid(
                new SimpleGrantedAuthority( AuthorityConstants.IS_AUTHENTICATED_ANONYMOUSLY ) ), true );

        aclService.updateAcl( acl );

        // this will fail if the acl changes haven't been flushed ...
        if ( isPrivate( object ) ) {
            // throw new IllegalStateException( "Failed to make object public: " + object );
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
    @Transactional
    public void makeReadableByGroup( Securable s, String groupName ) throws AccessDeniedException {

        if ( StringUtils.isBlank( groupName ) ) {
            throw new IllegalArgumentException( "'group' cannot be null" );
        }

        Collection<String> groups = checkForGroupAccessByCurrentuser( groupName );

        if ( !groups.contains( groupName ) && !SecurityUtil.isUserAdmin() ) {
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
    @Transactional
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
    @Transactional
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
    @Transactional
    public void makeWriteableByGroup( Securable s, String groupName ) throws AccessDeniedException {

        if ( StringUtils.isBlank( groupName ) ) {
            throw new IllegalArgumentException( "'group' cannot be null" );
        }

        Collection<String> groups = checkForGroupAccessByCurrentuser( groupName );

        if ( !groups.contains( groupName ) && !SecurityUtil.isUserAdmin() ) {
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

        a.setOwner( new AclPrincipalSid( userName ) );

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

        acl.insertAce( acl.getEntries().size(), permission, new AclGrantedAuthoritySid( userManager.getRolePrefix()
                + ga ), true );
        aclService.updateAcl( acl );
    }

    /**
     * @param s
     * @param permission
     * @param principal i.e. username
     */
    private void addPrincipalAuthority( Securable s, Permission permission, String principal ) {
        MutableAcl acl = getAcl( s );
        acl.insertAce( acl.getEntries().size(), permission, new AclPrincipalSid( principal ), true );
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
     * @return groups that the current user can view. For administrators, this is all groups.
     */
    private Collection<String> getGroupsUserCanView() {
        Collection<String> groupNames;
        try {
            // administrator...
            groupNames = userManager.findAllGroups();
        } catch ( AccessDeniedException e ) {
            // I'm not sure this actually happens. Usermanager.findAllGroups should just show all of the user's viewable
            // groups.
            groupNames = userManager.findGroupsForUser( userManager.getCurrentUsername() );
        }
        return groupNames;
    }

    /**
     * @param securables
     * @return
     */
    private <T extends Securable> Map<ObjectIdentity, T> getObjectIdentities( Collection<T> securables ) {
        Map<ObjectIdentity, T> result = new HashMap<>();
        for ( T s : securables ) {
            result.put( objectIdentityRetrievalStrategy.getObjectIdentity( s ), s );
        }
        return result;
    }

    private <T extends Securable> Map<T, Boolean> groupHasPermission( Collection<T> securables,
            List<Permission> requiredPermissions, String groupName ) {
        Map<T, Boolean> result = new HashMap<>();
        Map<ObjectIdentity, T> objectIdentities = getObjectIdentities( securables );

        List<GrantedAuthority> auths = userManager.findGroupAuthorities( groupName );

        List<Sid> sids = new ArrayList<Sid>();
        for ( GrantedAuthority a : auths ) {
            AclGrantedAuthoritySid sid = new AclGrantedAuthoritySid( new SimpleGrantedAuthority(
                    userManager.getRolePrefix() + a.getAuthority() ) );
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
            AclGrantedAuthoritySid sid = new AclGrantedAuthoritySid( new SimpleGrantedAuthority(
                    userManager.getRolePrefix() + a.getAuthority() ) );
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
            acl = aclService.readAclById( objectIdentity, sids );
            // administrative mode = true
            return acl.isGranted( requiredPermissions, sids, true );
        } catch ( NotFoundException ignore ) {
            return false;
        }
    }

    private <T extends Securable> void populateGroupsEditableBy( Map<T, Collection<String>> result, String groupName,
            Map<T, Boolean> groupHasPermission ) {
        for ( T s : groupHasPermission.keySet() ) {
            if ( groupHasPermission.get( s ) ) {
                if ( !result.containsKey( s ) ) {
                    result.put( s, new HashSet<String>() );
                }
                result.get( s ).add( groupName );
            }

        }
    }

    /**
     * Wrapper method that calls removeOneGrantedAuthority to ensure that only one ace at a time is updated. A bit
     * clunky but it ensures that the code is called as a complete unit, that is an update is performed and the array
     * retrieved again after update. The reason is that we remove them by the entry index, which changes ... so we have
     * to do it "iteratively".
     * 
     * @param The object to remove the permissions from
     * @param permission Permission to change.
     * @param authority e.g. "GROUP_JOESLAB"
     */
    private void removeGrantedAuthority( Securable object, Permission permission, String authority ) {
        int numberOfAclsToRemove = 1;
        // for 0 or 1 acls should only call once
        while ( numberOfAclsToRemove > 0 ) {
            numberOfAclsToRemove = removeOneGrantedAuthority( object, permission, authority );
        }
    }

    /**
     * Method removes just one ace and then informs calling method the number of aces to remove
     * 
     * @param object The object to remove the permissions from
     * @param permission The permission to remove
     * @param authority e.g. "GROUP_JOESLAB"
     * @return Number of ace records that need removing
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
            if ( sid instanceof AclGrantedAuthoritySid
                    && ( ( AclGrantedAuthoritySid ) sid ).getGrantedAuthority().equals( authority ) ) {
                log.info( "Removing: " + permission + " from " + object + " granted to " + sid );
                toremove.add( i );
            } else {
                log.debug( "Keeping: " + permission + " on " + object + " granted to " + sid );
            }
        }

        if ( toremove.isEmpty() ) {
            // this can happen commonly, no big deal.
            if ( log.isDebugEnabled() ) log.debug( "No changes, didn't remove: " + authority );
        } else if ( toremove.size() >= 1 ) {
            numberAclsToRemove = toremove.size() - 1;
            // take the first acl
            acl.deleteAce( toremove.get( 0 ) );
            aclService.updateAcl( acl );
        }

        return numberAclsToRemove;

    }

}