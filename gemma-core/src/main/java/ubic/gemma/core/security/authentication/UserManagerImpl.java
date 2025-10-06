/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.core.security.authentication;

import gemma.gsec.AuthorityConstants;
import gemma.gsec.authentication.UserDetailsImpl;
import gemma.gsec.authentication.UserExistsException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.intercept.RunAsUserToken;
import org.springframework.security.access.vote.AuthenticatedVoter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserCache;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.cache.NullUserCache;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.GroupAuthority;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserGroup;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation for Spring Security, plus some other handy methods.
 *
 * @author pavlidis
 */
@SuppressWarnings("unused")
@Service("userManager")
public class UserManagerImpl implements UserManager {

    private final Log logger = LogFactory.getLog( this.getClass() );

    private boolean enableAuthorities = false;

    private boolean enableGroups = true;

    @Autowired(required = false)
    private UserCache userCache = new NullUserCache();

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationTrustResolver authenticationTrustResolver;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public String changePasswordForUser( String email, String username, String newPassword )
            throws AuthenticationException {
        Authentication currentAuthentication = SecurityContextHolder.getContext().getAuthentication();

        if ( currentAuthentication == null ) {
            // This would indicate bad coding somewhere
            throw new AccessDeniedException(
                    "Can't change password as no Authentication object found in context " + "for current user." );
        }

        User u = userService.findByEmail( email );

        if ( u == null ) {
            throw new UsernameNotFoundException( "No user found for that email address." );
        }

        String foundUsername = u.getUserName();

        if ( !foundUsername.equals( username ) ) {
            throw new AccessDeniedException( "Wrong user name was provided for the email address." );
        }

        logger.debug( "Changing password for user '" + username + "'" );

        u.setPassword( passwordEncoder.encodePassword( newPassword, username ) );
        u.setEnabled( false );
        u.setSignupToken( this.generateSignupToken( username ) );
        u.setSignupTokenDatestamp( new Date() );
        userService.update( u );

        userCache.removeUserFromCache( username );

        return u.getSignupToken();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<String> findAllUsers() {
        Collection<gemma.gsec.model.User> users = userService.loadAll();

        List<String> result = new ArrayList<>();
        for ( gemma.gsec.model.User u : users ) {
            result.add( u.getUserName() );
        }
        return result;
    }

    @Override
    @Transactional
    public UserDetailsImpl createUser( String username, String email, String password ) {
        Date now = new Date();
        String key = generateSignupToken( username );
        String encodedPassword = passwordEncoder.encodePassword( password, username );
        UserDetailsImpl u = new UserDetailsImpl( encodedPassword, username, false, null, email, key, now );
        createUser( u );
        return u;
    }

    @Override
    @Transactional(readOnly = true)
    public User findByEmail( String emailAddress ) {
        return userService.findByEmail( emailAddress );
    }

    @Override
    @Transactional(readOnly = true)
    public User findByUserName( String userName ) {
        return this.userService.findByUserName( userName );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<String> findGroupsForUser( String userName ) {

        Collection<String> result = new HashSet<>();

        if ( !this.loggedIn() ) {
            return result;
        }

        User u = this.loadUser( userName );
        Collection<gemma.gsec.model.UserGroup> groups = userService.findGroupsForUser( u );

        for ( gemma.gsec.model.UserGroup g : groups ) {
            result.add( g.getName() );
        }

        return result;
    }

    @Override
    public String generateSignupToken( String username ) {
        return RandomStringUtils.secureStrong().nextAlphanumeric( 32 ).toUpperCase();
    }

    @Override
    @Transactional(readOnly = true)
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if ( auth == null || !auth.isAuthenticated() ) {
            throw new IllegalStateException( "Not authenticated!" );
        }
        if ( authenticationTrustResolver.isAnonymous( auth ) ) {
            return null;
        }
        // check if we have an anonymous user running with elevated privileges (i.e. during registration)
        if ( auth instanceof RunAsUserToken ) {
            RunAsUserToken runAsAuth = ( RunAsUserToken ) auth;
            if ( AnonymousAuthenticationToken.class.isAssignableFrom( runAsAuth.getOriginalAuthentication() ) ) {
                return null;
            }
        }
        return getUserForUserName( getUsernameFromAuth( auth ) );
    }

    @Override
    public String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if ( auth == null || !auth.isAuthenticated() ) {
            throw new IllegalStateException( "Not authenticated!" );
        }
        return getUsernameFromAuth( auth );
    }

    private String getUsernameFromAuth( Authentication auth ) {
        if ( auth.getPrincipal() instanceof UserDetails ) {
            return ( ( UserDetails ) auth.getPrincipal() ).getUsername();
        }
        return auth.getPrincipal().toString();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean groupExists( String groupName ) {
        return userService.groupExists( groupName );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<User> loadAll() {
        return this.userService.loadAll().stream()
                .map( u -> ( User ) u )
                .peek( u -> Hibernate.initialize( u.getGroups() ) )
                .collect( Collectors.toList() );
    }

    @Override
    public boolean loggedIn() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && !authenticationTrustResolver.isAnonymous( authentication );
    }

    @Override
    public void reauthenticate( String username, String password ) {
        logger.warn( "Not implemented." );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean userWithEmailExists( String emailAddress ) {
        return userService.findByEmail( emailAddress ) != null;
    }

    @Override
    @Transactional
    public boolean validateSignupToken( String username, String key ) {

        UserDetailsImpl u = ( UserDetailsImpl ) this.loadUserByUsername( username );

        if ( u.isEnabled() ) {
            logger.warn( "User is already enabled, skipping token validation" );
            return true;
        }

        String storedTok = u.getSignupToken();
        Date storedDate = u.getSignupTokenDatestamp();

        if ( storedTok == null || storedDate == null ) {
            throw new IllegalArgumentException( "User does not have a token" );
        }

        Date oneWeekAgo = DateUtils.addWeeks( new Date(), -2 );

        if ( !storedTok.equals( key ) || storedDate.before( oneWeekAgo ) ) {
            return false;
        }

        u.setEnabled( true );

        this.updateUser( u );

        return true;
    }

    @Override
    @Transactional
    public void createUser( UserDetails user ) {

        /*
         * UserDetails is not an entity, so this method is not directly managed by the Audit or ACL advice. However, it
         * runs in a transaction and calls two service methods which are intercepted. This means it is intercepted
         * before the transaction is flushed.
         */

        this.validateUserName( user.getUsername() );

        User u = ubic.gemma.model.common.auditAndSecurity.User.Factory.newInstance( user.getUsername() );
        u.setPassword( user.getPassword() );
        u.setEnabled( user.isEnabled() );

        if ( user instanceof UserDetailsImpl ) {
            u.setSignupToken( ( ( UserDetailsImpl ) user ).getSignupToken() );
            u.setSignupTokenDatestamp( ( ( UserDetailsImpl ) user ).getSignupTokenDatestamp() );
        }

        if ( user instanceof UserDetailsImpl ) {
            u.setEmail( ( ( UserDetailsImpl ) user ).getEmail() );
        }

        try {
            u = userService.create( u );
        } catch ( UserExistsException e ) {
            throw new RuntimeException( e );
        }

        // Add the user to the default user group.
        UserGroup g = this.loadGroup( AuthorityConstants.USER_GROUP_NAME );
        userService.addUserToGroup( g, u );

        /*
         * We don't log the user in automatically, because we require that new users click a confirmation link in an
         * email.
         */
    }

    @Override
    @Transactional
    public void updateUser( UserDetails user ) {
        String username = user.getUsername();
        User u = userService.findByUserName( username );
        if ( u == null )
            throw new IllegalArgumentException( "No user could be loaded with name=" + user );

        u.setPassword( user.getPassword() );
        u.setEnabled( user.isEnabled() );
        if ( user instanceof UserDetailsImpl ) {
            u.setEmail( ( ( UserDetailsImpl ) user ).getEmail() );
        }

        userService.update( u );

        userCache.removeUserFromCache( user.getUsername() );
    }

    @Override
    @Transactional
    public void updateUserGroups( UserDetails userDetails, Collection<String> groups ) {
        User u = userService.findByUserName( userDetails.getUsername() );
        if ( u == null ) {
            throw new IllegalArgumentException( String.format( "Unknown user with username %s.", userDetails.getUsername() ) );
        }
        Set<UserGroup> newGroups = new HashSet<>();
        for ( String groupName : groups ) {
            UserGroup group = userService.findGroupByName( groupName );
            if ( group == null ) {
                throw new IllegalArgumentException( String.format( "Unknown group with name %s.", groupName ) );
            }
            newGroups.add( group );
        }
        u.getGroups().clear();
        u.getGroups().addAll( newGroups );
        System.out.println( newGroups );
        userService.update( u );
    }

    @Override
    @Transactional
    public void deleteUser( String username ) {
        User user = this.loadUser( username );
        userService.delete( user );
        userCache.removeUserFromCache( username );
    }

    @Override
    @Transactional
    public void changePassword( String oldPassword, String newPassword ) throws AuthenticationException {
        Authentication currentAuthentication = SecurityContextHolder.getContext().getAuthentication();

        if ( currentAuthentication == null ) {
            // This would indicate bad coding somewhere
            throw new AccessDeniedException(
                    "Can't change password as no Authentication object found in context " + "for current user." );
        }

        String username = currentAuthentication.getName();

        logger.debug( "Changing password for user '" + username + "'" );

        User u = this.loadUser( username );
        u.setPassword( passwordEncoder.encodePassword( newPassword, username ) );
        userService.update( u );

        SecurityContextHolder.getContext()
                .setAuthentication( this.createNewAuthentication( currentAuthentication, u.getPassword() ) );

        userCache.removeUserFromCache( username );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean userExists( String username ) {
        return userService.findByUserName( username ) != null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findAllGroups() {
        Collection<gemma.gsec.model.UserGroup> groups = userService.listAvailableGroups();

        List<String> result = new ArrayList<>();
        for ( gemma.gsec.model.UserGroup group : groups ) {
            result.add( group.getName() );
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findUsersInGroup( String groupName ) {

        UserGroup group = this.loadGroup( groupName );

        Collection<User> groupMembers = group.getGroupMembers();

        List<String> result = new ArrayList<>();
        for ( gemma.gsec.model.User u : groupMembers ) {
            result.add( u.getUserName() );
        }
        return result;

    }

    @Override
    @Transactional
    public void createGroup( String groupName, List<GrantedAuthority> authorities ) {

        UserGroup g = ubic.gemma.model.common.auditAndSecurity.UserGroup.Factory.newInstance();
        g.setName( groupName );
        for ( GrantedAuthority ga : authorities ) {
            g.getAuthorities().add( ubic.gemma.model.common.auditAndSecurity.GroupAuthority.Factory
                    .newInstance( ga.getAuthority() ) );
        }

        userService.create( g );
    }

    @Override
    @Transactional
    public void deleteGroup( String groupName ) {
        UserGroup group = this.loadGroup( groupName );
        userService.delete( group );
    }

    @Override
    @Transactional
    public void renameGroup( String oldName, String newName ) {

        UserGroup group = userService.findGroupByName( oldName );

        group.setName( newName );

        userService.update( group );
    }

    @Override
    @Transactional
    public void addUserToGroup( String username, String groupName ) {
        User u = this.loadUser( username );
        UserGroup g = this.loadGroup( groupName );
        userService.addUserToGroup( g, u );
    }

    @Override
    @Transactional
    public void removeUserFromGroup( String username, String groupName ) {

        User user = userService.findByUserName( username );
        UserGroup group = userService.findGroupByName( groupName );

        if ( user == null || group == null ) {
            throw new IllegalArgumentException( "User or group could not be read" );
        }

        userService.removeUserFromGroup( user, group );
    }

    @Override
    @Transactional(readOnly = true)
    public List<GrantedAuthority> findGroupAuthorities( String groupName ) {

        String groupToSearch = groupName;
        if ( groupName.startsWith( AuthorityConstants.ROLE_PREFIX ) ) {
            groupToSearch = groupToSearch.replaceFirst( AuthorityConstants.ROLE_PREFIX, "" );
        }

        UserGroup group = this.loadGroup( groupToSearch );

        List<GrantedAuthority> result = new ArrayList<>();
        for ( gemma.gsec.model.GroupAuthority ga : group.getAuthorities() ) {
            result.add( new SimpleGrantedAuthority( ga.getAuthority() ) );
        }

        return result;
    }

    @Override
    @Transactional
    public void addGroupAuthority( String groupName, GrantedAuthority authority ) {
        UserGroup g = this.loadGroup( groupName );

        for ( gemma.gsec.model.GroupAuthority ga : g.getAuthorities() ) {
            if ( ga.getAuthority().equals( authority.getAuthority() ) ) {
                logger.warn( "Group already has authority" + authority.getAuthority() );
                return;
            }
        }

        GroupAuthority auth = GroupAuthority.Factory.newInstance();
        auth.setAuthority( authority.getAuthority() );

        g.getAuthorities().add( auth );

        userService.update( g );
    }

    @Override
    @Transactional
    public void removeGroupAuthority( String groupName, GrantedAuthority authority ) {

        UserGroup group = this.loadGroup( groupName );

        userService.removeGroupAuthority( group, authority.getAuthority() );
    }

    public boolean isEnableAuthorities() {
        return enableAuthorities;
    }

    public void setEnableAuthorities( boolean enableAuthorities ) {
        this.enableAuthorities = enableAuthorities;
    }

    public boolean isEnableGroups() {
        return enableGroups;
    }

    public void setEnableGroups( boolean enableGroups ) {
        this.enableGroups = enableGroups;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername( String username ) throws UsernameNotFoundException {
        User user = this.loadUser( username );

        Set<GrantedAuthority> dbAuthsSet = new HashSet<>();

        if ( enableAuthorities ) {
            dbAuthsSet.addAll( this.loadUserAuthorities( user.getUserName() ) );
        }

        if ( enableGroups ) {
            dbAuthsSet.addAll( this.loadGroupAuthorities( user ) );
        }

        if ( dbAuthsSet.isEmpty() ) {
            throw new UsernameNotFoundException( "User " + username + " has no GrantedAuthority" );
        }

        List<GrantedAuthority> dbAuths = new ArrayList<>( dbAuthsSet );

        // addCustomAuthorities( user.getUsername(), dbAuths );

        return this.createUserDetails( username, new UserDetailsImpl( user ), dbAuths );
    }

    protected List<UserDetails> loadUsersByUsername( String username ) {
        List<UserDetails> result = new ArrayList<>();
        User u = this.loadUser( username );

        UserDetails ud = new UserDetailsImpl( u );

        result.add( ud );
        return result;
    }

    private Authentication createNewAuthentication( Authentication currentAuth, String newPassword ) {
        UserDetails user = this.loadUserByUsername( currentAuth.getName() );

        UsernamePasswordAuthenticationToken newAuthentication = new UsernamePasswordAuthenticationToken( user,
                user.getPassword(), user.getAuthorities() );
        newAuthentication.setDetails( currentAuth.getDetails() );

        return newAuthentication;
    }

    private UserDetails createUserDetails( String username, UserDetailsImpl userFromUserQuery,
            List<GrantedAuthority> combinedAuthorities ) {
        return new UserDetailsImpl( userFromUserQuery.getPassword(), username, userFromUserQuery.isEnabled(),
                combinedAuthorities, userFromUserQuery.getEmail(), userFromUserQuery.getSignupToken(),
                userFromUserQuery.getSignupTokenDatestamp() );
    }

    private List<GrantedAuthority> loadGroupAuthorities( User user ) {
        Collection<gemma.gsec.model.GroupAuthority> authorities = userService.loadGroupAuthorities( user );

        List<GrantedAuthority> result = new ArrayList<>();
        for ( gemma.gsec.model.GroupAuthority ga : authorities ) {
            String roleName = AuthorityConstants.ROLE_PREFIX + ga.getAuthority();
            result.add( new SimpleGrantedAuthority( roleName ) );
        }

        return result;
    }

    private List<GrantedAuthority> loadUserAuthorities( @SuppressWarnings("unused") String username ) {
        throw new UnsupportedOperationException( "Use the group-based authorities instead" );
    }

    /**
     * @param username username
     * @return user, or null if the user is anonymous.
     * @throws UsernameNotFoundException if the user does not exist in the system
     */
    private User getUserForUserName( String username ) throws UsernameNotFoundException {
        User u = userService.findByUserName( username );
        if ( u == null ) {
            throw new UsernameNotFoundException( username + " not found" );
        }
        return u;
    }

    private UserGroup loadGroup( String groupName ) {
        UserGroup group = userService.findGroupByName( groupName );

        if ( group == null ) {
            throw new UsernameNotFoundException( "Group could not be read" );
        }

        return group;
    }

    private User loadUser( String username ) {
        User user = userService.findByUserName( username );
        if ( user == null ) {
            throw new UsernameNotFoundException( "User with name " + username + " could not be loaded" );
        }
        return user;
    }

    private void validateUserName( String username ) {
        if ( StringUtils.isBlank( username )
                || username.startsWith( AuthorityConstants.ROLE_PREFIX )
                || StringUtils.startsWithIgnoreCase( username, "admin" )
                || StringUtils.startsWithIgnoreCase( username, "user" )
                || StringUtils.startsWithIgnoreCase( username, "agent" )
                || username.equals( AuthenticatedVoter.IS_AUTHENTICATED_ANONYMOUSLY ) ) {
            throw new IllegalArgumentException( "Username=" + username + " is not allowed" );
        }
    }
}
