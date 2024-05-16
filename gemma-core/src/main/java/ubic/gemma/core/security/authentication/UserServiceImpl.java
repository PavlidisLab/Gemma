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

import gemma.gsec.AuthorityConstants;
import gemma.gsec.SecurityService;
import gemma.gsec.acl.domain.AclGrantedAuthoritySid;
import gemma.gsec.acl.domain.AclService;
import gemma.gsec.authentication.UserExistsException;
import gemma.gsec.util.SecurityUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
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

import static java.util.Objects.requireNonNull;

/**
 * @author pavlidis
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserGroupDao userGroupDao;

    @Autowired
    private AclService aclService;

    @Autowired
    private SecurityService securityService;

    @Override
    @Transactional
    public void addGroupAuthority( gemma.gsec.model.UserGroup group, String authority ) {
        group = requireNonNull( userGroupDao.load( group.getId() ) );
        for ( gemma.gsec.model.GroupAuthority ga : group.getAuthorities() ) {
            if ( ga.getAuthority().equals( authority ) ) {
                return;
            }
        }
        GroupAuthority ga = GroupAuthority.Factory.newInstance();
        ga.setAuthority( authority );
        group.getAuthorities().add( ga );
        update( group );
    }

    @Override
    @Transactional
    public void addUserToGroup( gemma.gsec.model.UserGroup group, gemma.gsec.model.User user ) {
        group = requireNonNull( userGroupDao.load( group.getId() ) );
        user = requireNonNull( userDao.load( user.getId() ) );
        // add user to list of members
        group.getGroupMembers().add( user );
    }

    @Override
    @Transactional
    public User create( final gemma.gsec.model.User user ) throws UserExistsException {
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
        } catch ( DataIntegrityViolationException | InvalidDataAccessResourceUsageException e ) {
            throw new UserExistsException( "User '" + user.getUserName() + "' already exists!" );
        }
    }

    @Override
    @Transactional
    public UserGroup create( gemma.gsec.model.UserGroup group ) {
        return this.userGroupDao.create( ( UserGroup ) group );
    }

    @Override
    @Transactional
    public void delete( gemma.gsec.model.User user ) {
        user = requireNonNull( userDao.load( user.getId() ), "No user with ID: " + user.getId() );
        for ( UserGroup group : this.userDao.loadGroups( ( User ) user ) ) {
            group.getGroupMembers().remove( user );
        }
        this.userDao.remove( ( User ) user );
    }

    @Override
    @Transactional
    public void delete( gemma.gsec.model.UserGroup group ) {
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

        if ( !securityService.isOwnedByCurrentUser( this.findGroupByName( groupName ) ) && !SecurityUtil
                .isUserAdmin() ) {
            throw new AccessDeniedException( "Only administrator or owner of a group can remove it" );
        }

        String authority = securityService.getGroupAuthorityNameFromGroupName( groupName );

        this.userGroupDao.remove( ( ubic.gemma.model.common.auditAndSecurity.UserGroup ) group );

        /*
         * clean up acls that use this group...do that last!
         */
        aclService.deleteSid( new AclGrantedAuthoritySid( authority ) );
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
    public Collection<gemma.gsec.model.UserGroup> findGroupsForUser( gemma.gsec.model.User user ) {
        return new ArrayList<>( this.userGroupDao.findGroupsForUser( ( User ) user ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<gemma.gsec.model.UserGroup> listAvailableGroups() {
        return new ArrayList<>( this.userGroupDao.loadAll() );
    }

    @Override
    @Transactional(readOnly = true)
    public User load( final Long id ) {
        return this.userDao.load( id );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<gemma.gsec.model.User> loadAll() {
        return new ArrayList<>( this.userDao.loadAll() );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<gemma.gsec.model.GroupAuthority> loadGroupAuthorities( gemma.gsec.model.User user ) {
        return new ArrayList<>( this.userDao.loadGroupAuthorities( ( User ) user ) );
    }

    @Override
    @Transactional
    public void removeGroupAuthority( gemma.gsec.model.UserGroup group, String authority ) {
        group = requireNonNull( userGroupDao.load( group.getId() ) );
        group.getAuthorities().removeIf( ga -> ga.getAuthority().equals( authority ) );
    }

    @Override
    @Transactional
    public void removeUserFromGroup( gemma.gsec.model.User user, gemma.gsec.model.UserGroup group ) {
        group = requireNonNull( userGroupDao.load( group.getId() ) );
        user = requireNonNull( userDao.load( user.getId() ) );

        String userName = user.getUserName();
        String groupName = group.getName();

        if ( AuthorityConstants.REQUIRED_ADMINISTRATOR_USER_NAME.equals( userName )
                && AuthorityConstants.ADMIN_GROUP_NAME.equals( groupName ) ) {
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
    public void update( final gemma.gsec.model.User user ) {
        this.userDao.update( ( User ) user );
    }

    @Override
    @Transactional
    public void update( gemma.gsec.model.UserGroup group ) {
        this.userGroupDao.update( ( UserGroup ) group );
    }
}
