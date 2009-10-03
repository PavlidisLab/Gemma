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

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.common.auditAndSecurity.UserGroupService</code>, provides access
 * to all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.common.auditAndSecurity.UserGroupService
 */
public abstract class UserGroupServiceBase implements ubic.gemma.model.common.auditAndSecurity.UserGroupService {

    private ubic.gemma.model.common.auditAndSecurity.UserGroupDao userGroupDao;

    private ubic.gemma.model.common.auditAndSecurity.UserDao userDao;

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserGroupService#create(java.lang.String, java.lang.String)
     */
    public ubic.gemma.model.common.Securable create( final java.lang.String name, final java.lang.String description ) {
        try {
            return this.handleCreate( name, description );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.UserGroupServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.UserGroupService.create(java.lang.String name, java.lang.String description)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>user</code>'s DAO.
     */
    public void setUserDao( ubic.gemma.model.common.auditAndSecurity.UserDao userDao ) {
        this.userDao = userDao;
    }

    /**
     * Sets the reference to <code>userGroup</code>'s DAO.
     */
    public void setUserGroupDao( ubic.gemma.model.common.auditAndSecurity.UserGroupDao userGroupDao ) {
        this.userGroupDao = userGroupDao;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserGroupService#update(java.lang.String,
     *      ubic.gemma.model.common.auditAndSecurity.User)
     */
    public void update( final java.lang.String groupName,
            final ubic.gemma.model.common.auditAndSecurity.User groupMember ) {
        try {
            this.handleUpdate( groupName, groupMember );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.UserGroupServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.UserGroupService.update(java.lang.String groupName, ubic.gemma.model.common.auditAndSecurity.User groupMember)' --> "
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
     * Gets the reference to <code>userGroup</code>'s DAO.
     */
    protected ubic.gemma.model.common.auditAndSecurity.UserGroupDao getUserGroupDao() {
        return this.userGroupDao;
    }

    /**
     * Performs the core logic for {@link #create(java.lang.String, java.lang.String)}
     */
    protected abstract ubic.gemma.model.common.Securable handleCreate( java.lang.String name,
            java.lang.String description ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #update(java.lang.String, ubic.gemma.model.common.auditAndSecurity.User)}
     */
    protected abstract void handleUpdate( java.lang.String groupName,
            ubic.gemma.model.common.auditAndSecurity.User groupMember ) throws java.lang.Exception;

}