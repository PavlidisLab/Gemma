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
package gemma.gsec;

import gemma.gsec.acl.domain.AclService;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import gemma.gsec.authentication.GroupManager;
import gemma.gsec.authentication.UserDetailsManager;
import gemma.gsec.authentication.UserService;
import gemma.gsec.model.Securable;
import gemma.gsec.model.UserGroup;
import gemma.gsec.util.SecurityUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.vote.AuthenticatedVoter;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Methods for changing security on objects, creating and modifying groups, checking security on objects.
 * <p>
 * We removed the ACL filtering/checking from most of these methods, because it results in basically checking
 * permissions inside of methods that are checking permissions etc. We assume that anybody with read access on an object
 * can know something about its security state.
 *
 * @author keshav
 * @author paul
 * @version $Id: SecurityServiceImpl.java,v 1.28 2013/12/12 00:10:12 paul Exp $
 */
public class SecurityServiceImpl implements SecurityService {

    private final Log log = LogFactory.getLog( SecurityServiceImpl.class );

    private final AclService aclService;
    private final SessionRegistry sessionRegistry;
    private final ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy;
    private final SidRetrievalStrategy sidRetrievalStrategy;
    private final UserDetailsManager userDetailsManager;
    private final GroupManager groupManager;
    private final UserService userService;

    public SecurityServiceImpl( AclService aclService, SessionRegistry sessionRegistry, ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy, SidRetrievalStrategy sidRetrievalStrategy, UserDetailsManager userDetailsManager, GroupManager groupManager, UserService userService ) {
        this.aclService = aclService;
        this.sessionRegistry = sessionRegistry;
        this.objectIdentityRetrievalStrategy = objectIdentityRetrievalStrategy;
        this.sidRetrievalStrategy = sidRetrievalStrategy;
        this.userDetailsManager = userDetailsManager;
        this.groupManager = groupManager;
        this.userService = userService;
    }


    @Override
    @Transactional(readOnly = true)
    public <T extends Securable> Map<T, Boolean> arePublic( Collection<T> securables ) {
        return arePrivate( securables )
            .entrySet().stream()
            .collect( Collectors.toMap( Map.Entry::getKey, e -> !e.getValue() ) );
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends Securable> Map<T, Boolean> arePrivate( Collection<T> securables ) {
        Map<T, Boolean> result = new HashMap<>( securables.size() );
        Map<ObjectIdentity, T> objectIdentities = getObjectIdentities( securables );

        if ( objectIdentities.isEmpty() ) return result;

        /*
         * Take advantage of fast bulk loading of ACLs. Other methods should adopt this if they turn out to be heavily
         * used/slow.
         */
        Map<ObjectIdentity, Acl> acls;
        try {
            acls = aclService.readAclsById( new Vector<>( objectIdentities.keySet() ) );
        } catch ( NotFoundException e ) {
            acls = Collections.emptyMap();
        }

        for ( ObjectIdentity oi : acls.keySet() ) {
            Acl a = acls.get( oi );
            result.put( objectIdentities.get( oi ), a != null && SecurityUtil.isPrivate( a ) );
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends Securable> Map<T, Boolean> areShared( Collection<T> securables ) {
        Map<T, Boolean> result = new HashMap<>( securables.size() );
        Map<ObjectIdentity, T> objectIdentities = getObjectIdentities( securables );

        if ( objectIdentities.isEmpty() ) return result;

        Map<ObjectIdentity, Acl> acls;
        try {
            acls = aclService.readAclsById( new Vector<>( objectIdentities.keySet() ) );
        } catch ( NotFoundException e ) {
            acls = Collections.emptyMap();
        }

        for ( ObjectIdentity oi : acls.keySet() ) {
            Acl a = acls.get( oi );
            result.put( objectIdentities.get( oi ), a != null && SecurityUtil.isShared( a ) );
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends Securable> Collection<T> choosePrivate( Collection<T> securables ) {
        Collection<T> result = new HashSet<>();

        if ( securables.isEmpty() ) return result;

        Map<T, Boolean> arePrivate = arePrivate( securables );

        for ( T s : securables ) {
            if ( arePrivate.get( s ) ) result.add( s );
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends Securable> Collection<T> choosePublic( Collection<T> securables ) {
        Collection<T> result = new HashSet<>();

        if ( securables.isEmpty() ) return result;

        Map<T, Boolean> arePrivate = arePrivate( securables );

        for ( T s : securables ) {
            if ( !arePrivate.get( s ) ) result.add( s );
        }
        return result;
    }


    @Override
    @Transactional(readOnly = true)
    public Collection<String> readableBy( Securable s ) {
        Collection<String> allUsers = userDetailsManager.findAllUsers();

        Collection<String> result = new HashSet<>();

        for ( String u : allUsers ) {
            if ( isReadableByUser( s, u ) ) {
                result.add( u );
            }
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<String> editableBy( Securable s ) {

        Collection<String> allUsers = userDetailsManager.findAllUsers();

        Collection<String> result = new HashSet<>();

        for ( String u : allUsers ) {
            if ( isEditableByUser( s, u ) ) {
                result.add( u );
            }
        }

        return result;

    }

    @Override
    public int getAuthenticatedUserCount() {
        return this.sessionRegistry.getAllPrincipals().size();
    }

    @Override
    public Collection<String> getAuthenticatedUserNames() {
        List<Object> allPrincipals = this.sessionRegistry.getAllPrincipals();
        Collection<String> result = new HashSet<>();
        for ( Object o : allPrincipals ) {
            result.add( o.toString() );
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Sid> getAvailableSids() {

        Collection<Sid> results = new HashSet<>();

        Collection<String> users = userDetailsManager.findAllUsers();

        for ( String u : users ) {
            results.add( new PrincipalSid( u ) );
        }

        Collection<String> groups = groupManager.findAllGroups();

        for ( String g : groups ) {
            List<GrantedAuthority> ga = groupManager.findGroupAuthorities( g );
            for ( GrantedAuthority grantedAuthority : ga ) {
                results.add( new GrantedAuthoritySid( grantedAuthority.getAuthority() ) );
            }
        }

        return results;
    }

    /**
     * From the group name get the authority which should be underscored with GROUP_
     *
     * @param groupName The group name e.g. fish
     * @return The authority e.g. GROUP_FISH_...
     */
    @Override
    @Transactional(readOnly = true)
    public List<String> getGroupAuthoritiesNameFromGroupName( String groupName ) {
        Collection<String> groups = checkForGroupAccessByCurrentUser( groupName );
        if ( !groups.contains( groupName ) && !SecurityUtil.isUserAdmin() ) {
            throw new AccessDeniedException( "User doesn't have access to that group" );
        }
        return groupManager.findGroupAuthorities( groupName ).stream()
            .map( ga -> AuthorityConstants.ROLE_PREFIX + ga.getAuthority() )
            .collect( Collectors.toList() );
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends Securable> Map<T, Collection<String>> getGroupsEditableBy( Collection<T> securables ) {
        Collection<String> groupNames = getGroupsUserCanView( userDetailsManager.getCurrentUsername() );

        Map<T, Collection<String>> result = new HashMap<>( securables.size() );

        for ( String groupName : groupNames ) {
            populateGroupPermissions( result, groupName,
                groupHasPermission( securables, Collections.singletonList( BasePermission.WRITE ), groupName ) );
            populateGroupPermissions( result, groupName,
                groupHasPermission( securables, Collections.singletonList( BasePermission.ADMINISTRATION ), groupName ) );
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<String> getGroupsEditableBy( Securable s ) {
        Collection<String> groupNames = getGroupsUserCanView( userDetailsManager.getCurrentUsername() );

        Collection<String> result = new HashSet<>();

        for ( String string : groupNames ) {
            if ( this.isEditableByGroup( s, string ) ) {
                result.add( string );
            }
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends Securable> Map<T, Collection<String>> getGroupsReadableBy( Collection<T> securables ) {

        Map<T, Collection<String>> result = new HashMap<>();

        if ( securables.isEmpty() ) return result;

        Collection<String> groupNames = getGroupsUserCanView( userDetailsManager.getCurrentUsername() );

        for ( String groupName : groupNames ) {
            populateGroupPermissions( result, groupName,
                groupHasPermission( securables, Collections.singletonList( BasePermission.READ ), groupName ) );
            populateGroupPermissions( result, groupName,
                groupHasPermission( securables, Collections.singletonList( BasePermission.ADMINISTRATION ), groupName ) );
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<String> getGroupsReadableBy( Securable s ) {
        Collection<String> groupNames = getGroupsUserCanView( userDetailsManager.getCurrentUsername() );

        Collection<String> result = new HashSet<>();

        for ( String string : groupNames ) {
            if ( this.isReadableByGroup( s, string ) ) {
                result.add( string );
            }
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<String> getGroupsUserCanEdit( String userName ) {
        Collection<String> groupNames = getGroupsUserCanView( userName );

        Collection<String> result = new HashSet<>();
        for ( String gname : groupNames ) {
            UserGroup g = userService.findGroupByName( gname );
            if ( this.isEditableByUser( g, userName ) ) {
                result.add( gname );
            }
        }

        return result;

    }

    @Override
    @Transactional(readOnly = true)
    public Sid getOwner( Securable s ) {
        return getAcl( s ).getOwner();
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends Securable> Map<T, Sid> getOwners( Collection<T> securables ) {
        Map<T, Sid> result = new HashMap<>();
        Map<ObjectIdentity, T> objectIdentities = getObjectIdentities( securables );

        if ( securables.isEmpty() ) return result;

        /*
         * Take advantage of fast bulk loading of ACLs. Other methods sohuld adopt this if they turn out to be heavily
         * used/slow.
         */
        Map<ObjectIdentity, Acl> acls;
        try {
            acls = aclService.readAclsById( new Vector<>( objectIdentities.keySet() ) );
        } catch ( NotFoundException e ) {
            acls = Collections.emptyMap();
        }

        for ( ObjectIdentity oi : acls.keySet() ) {
            Acl a = acls.get( oi );
            if ( a != null ) {
                Sid owner = a.getOwner();
                result.put( objectIdentities.get( oi ), owner );
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isOwnedByCurrentUser( Securable s ) {
        if ( !SecurityUtil.isUserLoggedIn() ) {
            return false;
        }

        try {
            Acl acl = getAcl( s );

            Sid owner = acl.getOwner();
            if ( owner == null ) return false;

            /*
             * Special case: if we're the administrator, and the owner of the data is GROUP_ADMIN, we are considered the
             * owner.
             */
            String ownerAuthority = gemma.gsec.acl.domain.Sids.grantedAuthority( owner );
            if ( ownerAuthority != null
                && SecurityUtil.isUserAdmin()
                && ownerAuthority.equals( AuthorityConstants.ADMIN_GROUP_AUTHORITY ) ) {
                return true;
            }

            String ownerName = gemma.gsec.acl.domain.Sids.principalName( owner );
            if ( ownerName != null ) {

                if ( ownerName.equals( userDetailsManager.getCurrentUsername() ) ) {
                    return true;
                }

                /*
                 * Special case: if the owner is an administrator, and we're an administrator, we are considered the
                 * owner. Note that the intention is that usually the owner would be a GrantedAuthority (see last case,
                 * below), not a Principal, but this hasn't always been instituted.
                 */
                if ( SecurityUtil.isUserAdmin() ) {
                    try {
                        Collection<? extends GrantedAuthority> authorities = userDetailsManager.loadUserByUsername( ownerName )
                            .getAuthorities();
                        for ( GrantedAuthority grantedAuthority : authorities ) {
                            if ( grantedAuthority.getAuthority().equals( AuthorityConstants.ADMIN_GROUP_AUTHORITY ) ) {
                                return true;
                            }
                        }
                    } catch ( UsernameNotFoundException e ) {
                        log.warn( "Owner " + ownerName + " could not be retrieved to check role: " + e.getMessage() );
                        return false;
                    }

                    return false;
                }

            }

            return false;

        } catch ( NotFoundException nfe ) {
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isReadableByCurrentUser( Securable s ) {
        if ( !SecurityUtil.isUserLoggedIn() ) {
            return false;
        }
        return isReadableByUser( s, userDetailsManager.getCurrentUsername() );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isReadableByUser( Securable s, String userName ) {
        return hasPermission( s, Collections.singletonList( BasePermission.READ ), userName )
            || hasPermission( s, Collections.singletonList( BasePermission.ADMINISTRATION ), userName );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isReadableByGroup( Securable s, String groupName ) {
        return groupHasPermission( s, Collections.singletonList( BasePermission.READ ), groupName )
            || groupHasPermission( s, Collections.singletonList( BasePermission.ADMINISTRATION ), groupName );
    }


    @Override
    @Transactional(readOnly = true)
    public boolean isEditableByCurrentUser( Securable s ) {
        if ( !SecurityUtil.isUserLoggedIn() ) {
            return false;
        }
        return isEditableByUser( s, userDetailsManager.getCurrentUsername() );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEditableByUser( Securable s, String userName ) {
        return hasPermission( s, Collections.singletonList( BasePermission.WRITE ), userName )
            || hasPermission( s, Collections.singletonList( BasePermission.ADMINISTRATION ), userName );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEditableByGroup( Securable s, String groupName ) {
        return groupHasPermission( s, Collections.singletonList( BasePermission.WRITE ), groupName )
            || groupHasPermission( s, Collections.singletonList( BasePermission.ADMINISTRATION ), groupName );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPublic( Securable s ) {
        return !isPrivate( s );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPrivate( Securable s ) {
        /*
         * Note: in theory, it should pay attention to the sid we ask for and return nothing if there is no acl.
         * However, the implementation actually ignores the sid argument.
         */
        try {
            Acl acl = getAcl( s );
            return SecurityUtil.isPrivate( acl );
        } catch ( NotFoundException nfe ) {
            return true;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isShared( Securable s ) {
        /*
         * Implementation note: this code mimics AclEntryVoter.vote, but in adminsitrative mode so no auditing etc
         * happens.
         */

        /*
         * Note: in theory, it should pay attention to the sid we ask for and return nothing if there is no acl.
         * However, the implementation actually ignores the sid argument. See BasicLookupStrategy
         */
        try {
            Acl acl = getAcl( s );
            return SecurityUtil.isShared( acl );
        } catch ( NotFoundException nfe ) {
            return true;
        }
    }

    @Override
    @Transactional
    public void makeOwnedByUser( Securable s, String userName ) {
        MutableAcl acl = getAcl( s );

        String ownerName = gemma.gsec.acl.domain.Sids.principalName( acl.getOwner() );
        if ( ownerName != null && ownerName.equals( userName ) ) {
            /*
             * Already owned by the given user -- note we don't check if the user exists here.
             */
            return;
        }

        // make sure user exists and is enabled.
        UserDetails user = this.userDetailsManager.loadUserByUsername( userName );
        if ( !user.isEnabled() || !user.isAccountNonExpired() || !user.isAccountNonLocked() ) {
            throw new IllegalArgumentException( "User  " + userName + " has a disabled account" );
        }

        acl.setOwner( new org.springframework.security.acls.domain.PrincipalSid( userName ) );
        aclService.updateAcl( acl );

        /*
         * FIXME: I don't know if these are necessary if you are the owner.
         */
        addPrincipalAuthority( s, BasePermission.WRITE, userName );
        addPrincipalAuthority( s, BasePermission.READ, userName );
    }

    @Override
    @Transactional
    public void makePrivate( Collection<? extends Securable> objs ) {
        objs.forEach( this::makePrivate );
    }

    @Override
    @Transactional
    public void makePrivate( Securable object ) {
        if ( isPrivate( object ) ) {
            log.warn( "Object is already private: " + object );
            return;
        }

        /*
         * Remove ACE for IS_AUTHENTICATED_ANOYMOUSLY, if it's there.
         */
        String authorityToRemove = AuthenticatedVoter.IS_AUTHENTICATED_ANONYMOUSLY;

        removeGrantedAuthority( object, BasePermission.READ, authorityToRemove );

        // will fail until flush...
        // if ( isPublic( object ) ) {
        //     throw new IllegalStateException( "Failed to make object private: " + object );
        // }

    }

    @Override
    @Transactional
    public void makePublic( Collection<? extends Securable> objs ) {
        objs.forEach( this::makePublic );
    }

    @Override
    @Transactional
    public void makePublic( Securable object ) {
        if ( isPublic( object ) ) {
            log.warn( "Object is already public: " + object );
            return;
        }

        /*
         * Add an ACE for IS_AUTHENTICATED_ANOYMOUSLY.
         */

        MutableAcl acl = getAcl( object );

        acl.insertAce( acl.getEntries().size(), BasePermission.READ, new GrantedAuthoritySid(
            new SimpleGrantedAuthority( AuthenticatedVoter.IS_AUTHENTICATED_ANONYMOUSLY ) ), true );

        aclService.updateAcl( acl );

        // this will fail if the acl changes haven't been flushed ...
        // if ( isPrivate( object ) ) {
        //     throw new IllegalStateException( "Failed to make object public: " + object );
        // }

    }

    @Override
    @Transactional
    public void makeReadableByGroup( Securable s, String groupName ) throws AccessDeniedException {
        if ( StringUtils.isEmpty( groupName.trim() ) ) {
            throw new IllegalArgumentException( "The group name cannot be empty." );
        }

        Collection<String> groups = checkForGroupAccessByCurrentUser( groupName );

        if ( !groups.contains( groupName ) && !SecurityUtil.isUserAdmin() ) {
            throw new AccessDeniedException( "User doesn't have access to that group" );
        }

        if ( isReadableByGroup( s, groupName ) ) {
            return;
        }

        addGroupAuthority( s, BasePermission.READ, groupName );

    }

    @Override
    @Transactional
    public void makeUnreadableByGroup( Securable s, String groupName ) throws AccessDeniedException {
        if ( StringUtils.isEmpty( groupName.trim() ) ) {
            throw new IllegalArgumentException( "'group' cannot be null" );
        }

        for ( String authority : getGroupAuthoritiesNameFromGroupName( groupName ) ) {
            removeGrantedAuthority( s, BasePermission.READ, authority );
            removeGrantedAuthority( s, BasePermission.WRITE, authority );
        }
    }

    @Override
    @Transactional
    public void makeUneditableByGroup( Securable s, String groupName ) throws AccessDeniedException {
        if ( StringUtils.isEmpty( groupName.trim() ) ) {
            throw new IllegalArgumentException( "'group' cannot be null" );
        }

        for ( String authority : getGroupAuthoritiesNameFromGroupName( groupName ) ) {
            removeGrantedAuthority( s, BasePermission.WRITE, authority );
        }
    }

    @Override
    @Transactional
    public void makeEditableByGroup( Securable s, String groupName ) throws AccessDeniedException {
        if ( StringUtils.isEmpty( groupName.trim() ) ) {
            throw new IllegalArgumentException( "'group' cannot be null" );
        }

        Collection<String> groups = checkForGroupAccessByCurrentUser( groupName );

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

    @Override
    @Transactional
    public void setOwner( Securable s, String userName ) {
        // make sure user exists and is enabled.
        UserDetails user = this.userDetailsManager.loadUserByUsername( userName );
        if ( !user.isEnabled() || !user.isAccountNonExpired() || !user.isAccountNonLocked() ) {
            throw new IllegalArgumentException( "User  " + userName + " has a disabled account" );
        }

        MutableAcl a = getAcl( s );
        a.setOwner( new PrincipalSid( userName ) );
        this.aclService.updateAcl( a );
    }

    @Override
    @Transactional
    public void createGroup( String groupName ) {

        /*
         * Nice if we can get around this uniqueness constraint...but I guess it's not easy.
         */
        if ( groupManager.groupExists( groupName ) ) {
            throw new IllegalArgumentException( "A group already exists with that name: " + groupName );
        }

        /*
         * We do make the groupAuthority unique.
         */
        String groupAuthority = groupName.toUpperCase() + "_" + randomGroupNameSuffix();

        this.groupManager.createGroup( groupName, Collections.singletonList( new SimpleGrantedAuthority( groupAuthority ) ) );
        addUserToGroup( userDetailsManager.getCurrentUsername(), groupName );

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

    @Override
    @Transactional
    public void addUserToGroup( String userName, String groupName ) {
        this.groupManager.addUserToGroup( userName, groupName );
    }

    @Override
    @Transactional
    public void removeUserFromGroup( String userName, String groupName ) {
        this.groupManager.removeUserFromGroup( userName, groupName );
    }

    private static final String ALLOWED_CHARS_IN_GROUP_NAME_SUFFIX = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";

    private String randomGroupNameSuffix() {
        Random random = new Random();
        char[] buffer = new char[32];
        for ( int i = 0; i < buffer.length; i++ ) {
            buffer[i] = ALLOWED_CHARS_IN_GROUP_NAME_SUFFIX.charAt( random.nextInt( ALLOWED_CHARS_IN_GROUP_NAME_SUFFIX.length() ) );
        }
        return new String( buffer );
    }

    /**
     * Provide permission to the given group on the given securable.
     *
     * @param groupName e.g. "GROUP_JOESLAB"
     */
    private void addGroupAuthority( Securable s, Permission permission, String groupName ) {
        List<GrantedAuthority> groupAuthorities = groupManager.findGroupAuthorities( groupName );
        if ( groupAuthorities == null || groupAuthorities.isEmpty() ) {
            throw new IllegalStateException( "Group has no authorities" );
        }
        MutableAcl acl = getAcl( s );
        for ( GrantedAuthority ga : groupAuthorities ) {
            acl.insertAce( acl.getEntries().size(), permission, new GrantedAuthoritySid( AuthorityConstants.ROLE_PREFIX + ga ), true );
        }
        aclService.updateAcl( acl );
    }

    /**
     * @param principal i.e. username
     */
    private void addPrincipalAuthority( Securable s, Permission permission, String principal ) {
        MutableAcl acl = getAcl( s );
        acl.insertAce( acl.getEntries().size(), permission, new PrincipalSid( principal ), true );
        aclService.updateAcl( acl );
    }

    /**
     * Check if the current user can access the given group.
     */
    private Collection<String> checkForGroupAccessByCurrentUser( String groupName ) {
        if ( groupName.equals( AuthorityConstants.ADMIN_GROUP_NAME ) ) {
            throw new AccessDeniedException( "Attempt to mess with ADMIN privileges denied" );
        }
        return groupManager.findGroupsForUser( userDetailsManager.getCurrentUsername() );
    }

    /**
     * @return groups that the current user can view. For administrators, this is all groups.
     */
    private Collection<String> getGroupsUserCanView( String userName ) {
        Collection<String> groupNames;
        try {
            // administrator...
            groupNames = groupManager.findAllGroups();
        } catch ( AccessDeniedException e ) {
            // I'm not sure this actually happens. Usermanager.findAllGroups should just show all of the user's viewable
            // groups.
            groupNames = groupManager.findGroupsForUser( userName );
        }
        return groupNames;
    }

    private <T extends Securable> Map<T, Boolean> groupHasPermission( Collection<T> securables,
        List<Permission> requiredPermissions, String groupName ) {
        Map<T, Boolean> result = new HashMap<>();
        Map<ObjectIdentity, T> objectIdentities = getObjectIdentities( securables );
        Map<ObjectIdentity, Acl> acls;
        try {
            acls = aclService.readAclsById( new Vector<>( objectIdentities.keySet() ) );
        } catch ( NotFoundException e ) {
            acls = Collections.emptyMap();
        }
        for ( ObjectIdentity oi : acls.keySet() ) {
            Acl a = acls.get( oi );
            try {
                result.put( objectIdentities.get( oi ), a != null && a.isGranted( requiredPermissions, getGroupSids( groupName ), true ) );
            } catch ( NotFoundException ignore ) {
                result.put( objectIdentities.get( oi ), false );
            }
        }
        return result;
    }

    private boolean groupHasPermission( Securable domainObject, List<Permission> requiredPermissions, String groupName ) {
        try {
            // Lookup only ACLs for SIDs we're interested in (this actually get them all)
            Acl acl = getAcl( domainObject );
            // administrative mode = true
            return acl.isGranted( requiredPermissions, getGroupSids( groupName ), true );
        } catch ( NotFoundException ignore ) {
            return false;
        }
    }

    /*
     * Private method that really doesn't work unless you are admin
     */
    private boolean hasPermission( Securable domainObject, List<Permission> requiredPermissions, String userName ) {
        // Obtain the SIDs applicable to the principal
        UserDetails user = userDetailsManager.loadUserByUsername( userName );
        Authentication authentication = new UsernamePasswordAuthenticationToken( userName, user.getPassword(),
            user.getAuthorities() );
        List<Sid> sids = sidRetrievalStrategy.getSids( authentication );

        try {
            Acl acl = getAcl( domainObject );
            // administrative mode = true
            return acl.isGranted( requiredPermissions, sids, true );
        } catch ( NotFoundException ignore ) {
            return false;
        }
    }

    private <T extends Securable> void populateGroupPermissions( Map<T, Collection<String>> result, String groupName, Map<T, Boolean> groupHasPermission ) {
        for ( T s : groupHasPermission.keySet() ) {
            if ( groupHasPermission.get( s ) ) {
                result.computeIfAbsent( s, k -> new HashSet<>() ).add( groupName );
            }
        }
    }

    /**
     * Wrapper method that calls removeOneGrantedAuthority to ensure that only one ace at a time is updated. A bit
     * clunky but it ensures that the code is called as a complete unit, that is an update is performed and the array
     * retrieved again after update. The reason is that we remove them by the entry index, which changes ... so we have
     * to do it "iteratively".
     *
     * @param object     The object to remove the permissions from
     * @param permission Permission to change.
     * @param authority  e.g. "GROUP_JOESLAB"
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
     * @param object     The object to remove the permissions from
     * @param permission The permission to remove
     * @param authority  e.g. "GROUP_JOESLAB"
     * @return Number of ace records that need removing
     */
    private int removeOneGrantedAuthority( Securable object, Permission permission, String authority ) {
        int numberAclsToRemove = 0;

        MutableAcl acl = getAcl( object );

        List<Integer> toremove = new Vector<>();
        for ( int i = 0; i < acl.getEntries().size(); i++ ) {
            AccessControlEntry entry = acl.getEntries().get( i );

            if ( !entry.getPermission().equals( permission ) ) {
                continue;
            }

            Sid sid = entry.getSid();
            String ga = gemma.gsec.acl.domain.Sids.grantedAuthority( sid );
            if ( ga != null && ga.equals( authority ) ) {
                log.info( "Removing: " + permission + " from " + object + " granted to " + sid );
                toremove.add( i );
            } else {
                log.debug( "Keeping: " + permission + " on " + object + " granted to " + sid );
            }
        }

        if ( toremove.isEmpty() ) {
            // this can happen commonly, no big deal.
            if ( log.isDebugEnabled() ) log.debug( "No changes, didn't remove: " + authority );
        } else {
            numberAclsToRemove = toremove.size() - 1;
            // take the first acl
            acl.deleteAce( toremove.get( 0 ) );
            aclService.updateAcl( acl );
        }

        return numberAclsToRemove;

    }

    private List<Sid> getGroupSids( String groupName ) {
        List<GrantedAuthority> auths = groupManager.findGroupAuthorities( groupName );
        List<Sid> sids = new ArrayList<>( auths.size() );
        for ( GrantedAuthority a : auths ) {
            // Use Spring Security's GrantedAuthoritySid so equals() matches the Spring-typed ACE sids
            // produced by BasicLookupStrategy when reading ACLs from the canonical schema.
            sids.add( new org.springframework.security.acls.domain.GrantedAuthoritySid(
                    new SimpleGrantedAuthority( AuthorityConstants.ROLE_PREFIX + a.getAuthority() ) ) );
        }
        return sids;
    }

    private MutableAcl getAcl( Securable s ) {
        ObjectIdentity oi = objectIdentityRetrievalStrategy.getObjectIdentity( s );
        return ( MutableAcl ) aclService.readAclById( oi );
    }

    private <T extends Securable> Map<ObjectIdentity, T> getObjectIdentities( Collection<T> securables ) {
        Map<ObjectIdentity, T> result = new HashMap<>();
        for ( T s : securables ) {
            result.put( objectIdentityRetrievalStrategy.getObjectIdentity( s ), s );
        }
        return result;
    }
}