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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserGroup;
import ubic.gemma.persistence.service.common.auditAndSecurity.UserDao;
import ubic.gemma.persistence.service.common.auditAndSecurity.UserGroupDao;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author pavlidis
 */
@SuppressWarnings("CollectionAddAllCanBeReplacedWithConstructor") // Not possible due to type safety
@Service
@Transactional
public class UserServiceImpl implements UserService {

    @Autowired
    UserDao userDao;

    @Autowired
    UserGroupDao userGroupDao;

    @Autowired
    private AclService aclService;

    @Autowired
    private SecurityService securityService;

    @Override
    public void addGroupAuthority( gemma.gsec.model.UserGroup group, String authority ) {
        this.userGroupDao.addAuthority( ( ubic.gemma.model.common.auditAndSecurity.UserGroup ) group, authority );
    }

    @Override
    public void addUserToGroup( gemma.gsec.model.UserGroup group, gemma.gsec.model.User user ) {
        // add user to list of members
        group.getGroupMembers().add( user );
        this.userGroupDao.update( ( ubic.gemma.model.common.auditAndSecurity.UserGroup ) group );
    }

    @Override
    public User create( final gemma.gsec.model.User user ) throws UserExistsException {

        if ( user.getUserName() == null ) {
            throw new IllegalArgumentException( "UserName cannot be null" );
        }

        if ( this.userDao.findByUserName( user.getUserName() ) != null ) {
            throw new UserExistsException( "User '" + user.getUserName() + "' already exists!" );
        }

        if ( this.findByEmail( user.getEmail() ) != null ) {
            throw new UserExistsException( "A user with email address '" + user.getEmail() + "' already exists." );
        }

        try {
            this.userDao.create( ( ubic.gemma.model.common.auditAndSecurity.User ) user );
            return ( ubic.gemma.model.common.auditAndSecurity.User ) user;
        } catch ( DataIntegrityViolationException | InvalidDataAccessResourceUsageException e ) {
            throw new UserExistsException( "User '" + user.getUserName() + "' already exists!" );
        }

    }

    @Override
    public UserGroup create( gemma.gsec.model.UserGroup group ) {
        this.userGroupDao.create( ( ubic.gemma.model.common.auditAndSecurity.UserGroup ) group );
        return ( ubic.gemma.model.common.auditAndSecurity.UserGroup ) group;
    }

    @Override
    public void delete( gemma.gsec.model.User user ) {
        for ( UserGroup group : this.userDao.loadGroups( ( ubic.gemma.model.common.auditAndSecurity.User ) user ) ) {
            group.getGroupMembers().remove( user );
            this.userGroupDao.update( ( ubic.gemma.model.common.auditAndSecurity.UserGroup ) group );
        }

        this.userDao.remove( ( ubic.gemma.model.common.auditAndSecurity.User ) user );
    }

    @Override
    public void delete( gemma.gsec.model.UserGroup group ) {
        String groupName = group.getName();

        if ( !this.groupExists( groupName ) ) {
            throw new IllegalArgumentException( "No group with that name: " + groupName );
        }

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
    public User findByEmail( final String email ) {
        return this.userDao.findByEmail( email );

    }

    @Override
    public ubic.gemma.model.common.auditAndSecurity.User findByUserName( final String userName ) {
        return this.userDao.findByUserName( userName );
    }

    @Override
    public UserGroup findGroupByName( String name ) {
        return this.userGroupDao.findByName( name );
    }

    @Override
    public boolean groupExists( String name ) {
        return this.userGroupDao.findByName( name ) != null;
    }

    @Override
    public Collection<gemma.gsec.model.UserGroup> findGroupsForUser( gemma.gsec.model.User user ) {
        Collection<gemma.gsec.model.UserGroup> ret = new ArrayList<>();
        ret.addAll( this.userGroupDao.findGroupsForUser( ( ubic.gemma.model.common.auditAndSecurity.User ) user ) );
        return ret;
    }

    @Override
    public Collection<gemma.gsec.model.UserGroup> listAvailableGroups() {
        Collection<gemma.gsec.model.UserGroup> ret = new ArrayList<>();
        ret.addAll( this.userGroupDao.loadAll() );
        return ret;
    }

    @Override
    public User load( final Long id ) {
        return this.userDao.load( id );
    }

    @Override
    public Collection<gemma.gsec.model.User> loadAll() {
        Collection<gemma.gsec.model.User> ret = new ArrayList<>();
        ret.addAll( this.userDao.loadAll() );
        return ret;
    }

    @Override
    public Collection<gemma.gsec.model.GroupAuthority> loadGroupAuthorities( gemma.gsec.model.User user ) {
        Collection<gemma.gsec.model.GroupAuthority> ret = new ArrayList<>();
        ret.addAll( this.userDao.loadGroupAuthorities( ( ubic.gemma.model.common.auditAndSecurity.User ) user ) );
        return ret;
    }

    @Override
    public void removeGroupAuthority( gemma.gsec.model.UserGroup group, String authority ) {
        this.userGroupDao.removeAuthority( ( ubic.gemma.model.common.auditAndSecurity.UserGroup ) group, authority );
    }

    @Override
    public void removeUserFromGroup( gemma.gsec.model.User user, gemma.gsec.model.UserGroup group ) {
        group.getGroupMembers().remove( user );

        String userName = user.getName();
        String groupName = group.getName();

        if ( AuthorityConstants.REQUIRED_ADMINISTRATOR_USER_NAME.equals( userName )
                && AuthorityConstants.ADMIN_GROUP_NAME.equals( groupName ) ) {
            throw new IllegalArgumentException( "You cannot remove the administrator from the ADMIN group!" );
        }

        if ( AuthorityConstants.USER_GROUP_NAME.equals( groupName ) ) {
            throw new IllegalArgumentException( "You cannot remove users from the USER group!" );
        }
        this.userGroupDao.update( ( ubic.gemma.model.common.auditAndSecurity.UserGroup ) group );

        /*
         * TODO: if the group is empty, should we remove it? Not if it is GROUP_USER or ADMIN, but perhaps otherwise.
         */
    }

    @Override
    public void update( final gemma.gsec.model.User user ) {
        this.userDao.update( ( ubic.gemma.model.common.auditAndSecurity.User ) user );
    }

    @Override
    public void update( gemma.gsec.model.UserGroup group ) {
        this.userGroupDao.update( ( ubic.gemma.model.common.auditAndSecurity.UserGroup ) group );
    }

}
