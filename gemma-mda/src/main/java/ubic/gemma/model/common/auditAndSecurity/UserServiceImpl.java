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
package ubic.gemma.model.common.auditAndSecurity;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.stereotype.Service;

/**
 * @see ubic.gemma.model.common.auditAndSecurity.UserService
 * @author pavlidis
 * @version $Id$
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserGroupDao userGroupDao;

    public void addGroupAuthority( UserGroup group, String authority ) {
        this.userGroupDao.addAuthority( group, authority );
    }

    public void addUserToGroup( UserGroup group, User user) {
        group.getGroupMembers().add( user );
        this.userGroupDao.update( group );
    }

    public UserGroup create( UserGroup group ) {
        return this.userGroupDao.create( group );
    }

    public void delete( User user ) {
        for ( UserGroup group : this.userDao.loadGroups( user ) ) {                     
            group.getGroupMembers().remove( user );            
            this.userGroupDao.update( group );            
        }

        this.userDao.remove( user );
    }

    public void delete( UserGroup group ) {
        this.userGroupDao.remove( group );
    }

    public UserGroup findGroupByName( String name ) {
        return this.userGroupDao.findByUserGroupName( name );
    }

    public Collection<UserGroup> findGroupsForUser( User user ) {
        return this.userGroupDao.findGroupsForUser( user );
    }
 
    public Collection<UserGroup> listAvailableGroups() {
        return ( Collection<UserGroup> ) this.userGroupDao.loadAll();
    }

    public Collection<GroupAuthority> loadGroupAuthorities( User u ) {
        return this.userDao.loadGroupAuthorities( u );
    }

    public void removeGroupAuthority( UserGroup group, String authority ) {
        this.userGroupDao.removeAuthority( group, authority );
    }

    public void removeUserFromGroup( User user, UserGroup group ) {
        group.getGroupMembers().remove( user );
        this.userGroupDao.update( group );

        /*
         * TODO: if the group is empty, should we delete it? Not if it is GROUP_USER or ADMIN, but perhaps otherwise.
         */
    }

    public void update( UserGroup group ) {
        this.userGroupDao.update( group );
    }


    public boolean groupExists( String name ) {
        return this.userGroupDao.findByUserGroupName( name ) != null;
    }

    
    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserService#create(ubic.gemma.model.common.auditAndSecurity.User)
     */
    public User create( final User user ) throws UserExistsException {
        try {

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
                return this.userDao.create( user );
            } catch ( DataIntegrityViolationException e ) {
                throw new UserExistsException( "User '" + user.getUserName() + "' already exists!" );
            } catch ( InvalidDataAccessResourceUsageException e ) {
                // shouldn't happen if we don't have duplicates in the first place...but just in case.
                throw new UserExistsException( "User '" + user.getUserName() + "' already exists!" );
            }
            
        } catch ( UserExistsException ex ) {
            throw ex;
        } catch ( Throwable th ) {
            throw new UserServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.UserService.create(ubic.gemma.model.common.auditAndSecurity.User user)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserService#findByEmail(java.lang.String)
     */
    public User findByEmail( final String email ) {
        try {
            
            return this.userDao.findByEmail( email );
            
        } catch ( Throwable th ) {
            throw new UserServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.UserService.findByEmail(java.lang.String email)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserService#findByUserName(java.lang.String)
     */
    public User findByUserName( final String userName ) {
        try {
        
            return this.userDao.findByUserName( userName );

        } catch ( Throwable th ) {
            throw new UserServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.UserService.findByUserName(java.lang.String userName)' --> "
                            + th, th );
        }
    }


    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserService#load(java.lang.Long)
     */
    public User load( final Long id ) {
        try {
        
            return this.userDao.load( id );
        
        } catch ( Throwable th ) {
            throw new UserServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.UserService.load(java.lang.Long id)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserService#loadAll()
     */
    public java.util.Collection<User> loadAll() {
        try {
            
            return ( Collection<User> ) this.userDao.loadAll();
            
        } catch ( Throwable th ) {
            throw new UserServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.UserService.loadAll()' --> " + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserService#update(ubic.gemma.model.common.auditAndSecurity.User)
     */
    public void update( final User user ) {
        try {
            
            this.userDao.update( user );
            
        } catch ( Throwable th ) {
            throw new UserServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.UserService.update(ubic.gemma.model.common.auditAndSecurity.User user)' --> "
                            + th, th );
        }
    }

//    /**
//     * Gets the reference to <code>user</code>'s DAO.
//     */
//    protected UserDao getUserDao() {
//        return this.userDao;
//    }
//  /**
//  * @return the userGroupDao
//  */
// public UserGroupDao getUserGroupDao() {
//     return userGroupDao;
// }
  /**
  * Sets the reference to <code>user</code>'s DAO.
  */
 public void setUserDao( ubic.gemma.model.common.auditAndSecurity.UserDao userDao ) {
     this.userDao = userDao;
 }

 /**
  * @param userGroupDao the userGroupDao to set
  */
 public void setUserGroupDao( UserGroupDao userGroupDao ) {
     this.userGroupDao = userGroupDao;
 }

    
}
