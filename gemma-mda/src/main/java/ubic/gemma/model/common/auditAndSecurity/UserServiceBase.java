/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.model.common.auditAndSecurity;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.common.auditAndSecurity.UserService</code>, provides access to
 * all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.common.auditAndSecurity.UserService
 */
public abstract class UserServiceBase implements ubic.gemma.model.common.auditAndSecurity.UserService {

    @Autowired
    private ubic.gemma.model.common.auditAndSecurity.UserDao userDao;

    @Autowired
    private UserGroupDao userGroupDao;

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserService#create(ubic.gemma.model.common.auditAndSecurity.User)
     */
    public ubic.gemma.model.common.auditAndSecurity.User create(
            final ubic.gemma.model.common.auditAndSecurity.User user )
            throws ubic.gemma.model.common.auditAndSecurity.UserExistsException {
        try {
            return this.handleCreate( user );
        } catch ( ubic.gemma.model.common.auditAndSecurity.UserExistsException ex ) {
            throw ex;
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.UserServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.UserService.create(ubic.gemma.model.common.auditAndSecurity.User user)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserService#findByEmail(java.lang.String)
     */
    public ubic.gemma.model.common.auditAndSecurity.User findByEmail( final java.lang.String email ) {
        try {
            return this.handleFindByEmail( email );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.UserServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.UserService.findByEmail(java.lang.String email)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserService#findByUserName(java.lang.String)
     */
    public ubic.gemma.model.common.auditAndSecurity.User findByUserName( final java.lang.String userName ) {
        try {
            return this.handleFindByUserName( userName );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.UserServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.UserService.findByUserName(java.lang.String userName)' --> "
                            + th, th );
        }
    }

    /**
     * @return the userGroupDao
     */
    public UserGroupDao getUserGroupDao() {
        return userGroupDao;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserService#load(java.lang.Long)
     */
    public ubic.gemma.model.common.auditAndSecurity.User load( final java.lang.Long id ) {
        try {
            return this.handleLoad( id );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.UserServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.UserService.load(java.lang.Long id)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserService#loadAll()
     */
    public java.util.Collection<User> loadAll() {
        try {
            return this.handleLoadAll();
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.UserServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.UserService.loadAll()' --> " + th, th );
        }
    }

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

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserService#update(ubic.gemma.model.common.auditAndSecurity.User)
     */
    public void update( final ubic.gemma.model.common.auditAndSecurity.User user ) {
        try {
            this.handleUpdate( user );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.UserServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.UserService.update(ubic.gemma.model.common.auditAndSecurity.User user)' --> "
                            + th, th );
        }
    }

    /**
     * Gets the reference to <code>user</code>'s DAO.
     */
    protected ubic.gemma.model.common.auditAndSecurity.UserDao getUserDao() {
        return this.userDao;
    }

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.common.auditAndSecurity.User)}
     */
    protected abstract ubic.gemma.model.common.auditAndSecurity.User handleCreate(
            ubic.gemma.model.common.auditAndSecurity.User user ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByEmail(java.lang.String)}
     */
    protected abstract ubic.gemma.model.common.auditAndSecurity.User handleFindByEmail( java.lang.String email )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByUserName(java.lang.String)}
     */
    protected abstract ubic.gemma.model.common.auditAndSecurity.User handleFindByUserName( java.lang.String userName )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #load(java.lang.Long)}
     */
    protected abstract ubic.gemma.model.common.auditAndSecurity.User handleLoad( java.lang.Long id )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadAll()}
     */
    protected abstract java.util.Collection<User> handleLoadAll() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.common.auditAndSecurity.User)}
     */
    protected abstract void handleUpdate( ubic.gemma.model.common.auditAndSecurity.User user )
            throws java.lang.Exception;

}