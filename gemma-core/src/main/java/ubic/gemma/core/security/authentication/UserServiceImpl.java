/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
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
package ubic.gemma.core.security.authentication;

import ubic.gemma.core.security.AuthorityConstants;
import ubic.gemma.core.security.SecurityService;
import ubic.gemma.core.security.acl.domain.AclService;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import ubic.gemma.core.security.authentication.UserExistsException;
import ubic.gemma.core.security.util.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.GroupAuthority;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserGroup;
import ubic.gemma.persistence.service.common.auditAndSecurity.UserDao;
import ubic.gemma.persistence.service.common.auditAndSecurity.UserGroupDao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * @author pavlidis
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService, ApplicationContextAware {

    private static final String ADMINISTRATOR_USER_NAME = "administrator";

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserGroupDao userGroupDao;

    @Autowired
    private AclService aclService;

    // FIXME: remove SecurityService from here, it depends on UserService, we're using afterPropertiesSet() as a
    //        workaround to prevent circular dependency
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext( ApplicationContext applicationContext ) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    @Transactional
    public void addGroupAuthority( ubic.gemma.core.security.model.UserGroup group, String authority ) {
        group = requireNonNull( userGroupDao.load( group.getId() ) );
        for ( ubic.gemma.core.security.model.GroupAuthority ga : group.getAuthorities() ) {
            if ( ga.getAuthority().equals( authority ) ) {
                log.warn( "Group already has authority '" + authority + "', ignoring." );
                return;
            }
        }
        ( ( UserGroup ) group ).getAuthorities().add( GroupAuthority.Factory.newInstance( authority ) );
        // will be created in cascade
        update( group );
    }

    @Override
    @Transactional
    public void addUserToGroup( ubic.gemma.core.security.model.UserGroup group, ubic.gemma.core.security.model.User user ) {
        group = requireNonNull( userGroupDao.load( group.getId() ) );
        user = requireNonNull( userDao.load( user.getId() ) );
        // add user to list of members
        ( ( UserGroup ) group ).getGroupMembers().add( ( User ) user );
    }

    @Override
    @Transactional
    public User create( final ubic.gemma.core.security.model.User user ) throws UserExistsException {
        if ( StringUtils.isBlank( user.getUserName() ) ) {
            throw new IllegalArgumentException( "Username cannot be blank" );
        }

        if ( this.findByUserName( user.getUserName() ) != null ) {
            throw new UserExistsException( "User '" + user.getUserName() + "' already exists!" );
        }

        if ( this.findByEmail( user.getEmail() ) != null ) {
            throw new UserExistsException( "A user with email address '" + user.getEmail() + "' already exists." );
        }

        try {
            return this.userDao.create( ( User ) user );
        } catch ( ConstraintViolationException e ) {
            throw new UserExistsException( "User '" + user.getUserName() + "' already exists!" );
        }
    }

    @Override
    @Transactional
    public UserGroup create( ubic.gemma.core.security.model.UserGroup group ) {
        return this.userGroupDao.create( ( UserGroup ) group );
    }

    @Override
    @Transactional
    public void delete( ubic.gemma.core.security.model.User user ) {
        user = requireNonNull( userDao.load( user.getId() ), "No user with ID: " + user.getId() );
        for ( UserGroup group : this.userDao.loadGroups( ( User ) user ) ) {
            group.getGroupMembers().remove( user );
        }
        this.userDao.remove( ( User ) user );
    }

    @Override
    @Transactional
    public void delete( ubic.gemma.core.security.model.UserGroup group ) {
        group = requireNonNull( userGroupDao.load( group.getId() ), "No group with that name: " + group.getName() );

        String groupName = group.getName();

        /*
         * make sure this isn't one of the special groups
         */
        if ( groupName.equals( AuthorityConstants.USER_GROUP_NAME ) || groupName
                .equals( AuthorityConstants.ADMIN_GROUP_NAME ) || groupName
                .equals( AuthorityConstants.AGENT_GROUP_NAME ) ) {
            throw new IllegalArgumentException( "Cannot remove that group, it is required for system operation." );
        }

        SecurityService securityService = applicationContext.getBean( SecurityService.class );

        if ( !securityService.isOwnedByCurrentUser( this.findGroupByName( groupName ) ) && !SecurityUtil
                .isUserAdmin() ) {
            throw new AccessDeniedException( "Only administrator or owner of a group can remove it" );
        }

        List<String> authority = securityService.getGroupAuthoritiesNameFromGroupName( groupName );

        this.userGroupDao.remove( ( UserGroup ) group );

        /*
         * clean up acls that use this group...do that last!
         */
        for ( String a : authority ) {
            aclService.deleteSid( new GrantedAuthoritySid( a ) );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public User findByEmail( final String email ) {
        return this.userDao.findByEmail( email );

    }

    @Override
    @Transactional(readOnly = true)
    public ubic.gemma.model.common.auditAndSecurity.User findByUserName( final String userName ) {
        return this.userDao.findByUserName( userName );
    }

    @Override
    @Transactional(readOnly = true)
    public UserGroup findGroupByName( String name ) {
        return this.userGroupDao.findByName( name );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean groupExists( String name ) {
        return this.userGroupDao.findByName( name ) != null;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ubic.gemma.core.security.model.UserGroup> findGroupsForUser( ubic.gemma.core.security.model.User user ) {
        return new ArrayList<>( this.userGroupDao.findGroupsForUser( ( User ) user ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ubic.gemma.core.security.model.UserGroup> listAvailableGroups() {
        return new ArrayList<>( this.userGroupDao.loadAll() );
    }

    @Override
    @Transactional(readOnly = true)
    public User load( final Long id ) {
        return this.userDao.load( id );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ubic.gemma.core.security.model.User> loadAll() {
        return new ArrayList<>( this.userDao.loadAll() );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ubic.gemma.core.security.model.GroupAuthority> loadGroupAuthorities( ubic.gemma.core.security.model.User user ) {
        return new ArrayList<>( this.userDao.loadGroupAuthorities( ( User ) user ) );
    }

    @Override
    @Transactional
    public void removeGroupAuthority( ubic.gemma.core.security.model.UserGroup group, String authority ) {
        group = requireNonNull( userGroupDao.load( group.getId() ) );
        group.getAuthorities().removeIf( ga -> ga.getAuthority().equals( authority ) );
    }

    @Override
    @Transactional
    public void removeUserFromGroup( ubic.gemma.core.security.model.User user, ubic.gemma.core.security.model.UserGroup group ) {
        group = requireNonNull( userGroupDao.load( group.getId() ) );
        user = requireNonNull( userDao.load( user.getId() ) );

        String userName = user.getUserName();
        String groupName = group.getName();

        if ( ADMINISTRATOR_USER_NAME.equals( userName ) && AuthorityConstants.ADMIN_GROUP_NAME.equals( groupName ) ) {
            throw new IllegalArgumentException( "You cannot remove the administrator from the ADMIN group!" );
        }

        if ( AuthorityConstants.USER_GROUP_NAME.equals( groupName ) ) {
            throw new IllegalArgumentException( "You cannot remove users from the USER group!" );
        }

        group.getGroupMembers().remove( user );

        /*
         * TODO: if the group is empty, should we remove it? Not if it is GROUP_USER or ADMIN, but perhaps otherwise.
         */
    }

    @Override
    @Transactional
    public void update( final ubic.gemma.core.security.model.User user ) {
        this.userDao.update( ( User ) user );
    }

    @Override
    @Transactional
    public void update( ubic.gemma.core.security.model.UserGroup group ) {
        this.userGroupDao.update( ( UserGroup ) group );
    }
}
