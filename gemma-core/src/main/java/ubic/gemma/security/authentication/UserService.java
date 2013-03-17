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
package ubic.gemma.security.authentication;

import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import ubic.gemma.model.common.auditAndSecurity.GroupAuthority;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserExistsException;
import ubic.gemma.model.common.auditAndSecurity.UserGroup;

import java.util.Collection;

/**
 * @version $Id$
 * @author paul
 */
public interface UserService {

    /**
     * @param group
     * @param authority
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void addGroupAuthority( UserGroup group, String authority );

    /**
     * @param user
     * @param group
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" /* this applies to the first arg only! - should use an expression */})
    public void addUserToGroup( UserGroup group, User user );

    /**
     * @param user
     * @return
     * @throws UserExistsException
     */
    @Secured( { "GROUP_ADMIN" })
    public User create( User user ) throws UserExistsException;

    /**
     * @param group
     * @return
     */
    @Secured( { "GROUP_USER" })
    public UserGroup create( UserGroup group );

    /**
     * Remove a user from the persistent store.
     * 
     * @param user
     */
    @Secured( { "GROUP_ADMIN" })
    public void delete( User user );

    /**
     * Remove a group from the persistent store
     * 
     * @param group
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void delete( UserGroup group );

    /**
     * 
     */
    @Secured( { "GROUP_USER", "AFTER_ACL_READ" })
    public User findByEmail( java.lang.String email );

    /**
     * @param userName
     * @return user or null if they don't exist.
     */
    public User findByUserName( String userName ); // don't secure,

    // to allow login

    /**
     * @param oldName
     * @return
     */
    @Secured( { "GROUP_USER", "AFTER_ACL_READ" })
    public UserGroup findGroupByName( String name );

    @Secured("GROUP_USER")
    public boolean groupExists( String name );

    /**
     * @param usernName
     * @return
     */
    @Secured( { "GROUP_USER", "AFTER_ACL_COLLECTION_READ" })
    public Collection<UserGroup> findGroupsForUser( User user );

    /**
     * A list of groups available (will be security-filtered)...might need to allow anonymous.
     */
    @Secured( { "GROUP_USER", "AFTER_ACL_COLLECTION_READ" })
    public Collection<UserGroup> listAvailableGroups();

    /**
     * @param id
     * @return
     */
    @Secured( { "GROUP_USER", "AFTER_ACL_READ" })
    public User load( Long id );

    /**
     * Retrieves a list of users
     */
    @Secured( { "GROUP_ADMIN" })
    public Collection<User> loadAll();

    /**
     * @param u
     * @return
     */
    public Collection<GroupAuthority> loadGroupAuthorities( User u ); // must not be secured to allow login...

    /**
     * Remove an authority from a group. Would rarely be used.
     * 
     * @param group
     * @param authority
     */
    @Secured( { "GROUP_ADMIN" })
    public void removeGroupAuthority( UserGroup group, String authority );

    /**
     * @param user
     * @param group
     */
    @PreAuthorize("hasPermission(#group, 'write') or hasPermission(#group, 'administration')")
    public void removeUserFromGroup( User user, UserGroup group );

    /**
     * @param user
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void update( User user );

    /**
     * @param group
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void update( UserGroup group );
}
