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
package ubic.gemma.core.security.authentication;

import ubic.gemma.core.security.model.GroupAuthority;
import ubic.gemma.core.security.model.User;
import ubic.gemma.core.security.model.UserGroup;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * @author paul
 * @version $Id: UserService.java,v 1.6 2014/06/17 19:20:47 paul Exp $
 */
@SuppressWarnings("unused")
public interface BaseUserService {

    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void addGroupAuthority( UserGroup group, String authority );

    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" /* this applies to the first arg only! - should use an expression */ })
    void addUserToGroup( UserGroup group, User user );

    @Secured({ "GROUP_ADMIN" })
    User create( User user ) throws UserExistsException;

    @Secured({ "GROUP_USER" })
    UserGroup create( UserGroup group );

    /**
     * Remove a user from the persistent store.
     */
    @Secured({ "GROUP_ADMIN" })
    void delete( User user );

    /**
     * Remove a group from the persistent store
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void delete( UserGroup group );

    @Nullable
    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    User findByEmail( String email );

    /**
     * @return user or null if they don't exist.
     */
    @Nullable
    User findByUserName( String userName ); // don't secure,

    // to allow login

    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    UserGroup findGroupByName( String name );

    @Secured({ "GROUP_USER", "AFTER_ACL_COLLECTION_READ" })
    Collection<UserGroup> findGroupsForUser( User user );

    @Secured("GROUP_USER")
    boolean groupExists( String name );

    /**
     * A list of groups available to the current user (will be security-filtered)...might need to allow anonymous.
     */
    @Secured({ "GROUP_USER", "AFTER_ACL_COLLECTION_READ" })
    Collection<UserGroup> listAvailableGroups();

    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    User load( Long id );

    /**
     * Retrieves a list of users
     */
    @Secured({ "GROUP_ADMIN" })
    Collection<User> loadAll();

    Collection<GroupAuthority> loadGroupAuthorities( User u ); // must not be secured to allow login...

    /**
     * Remove an authority from a group. Would rarely be used.
     */
    @Secured({ "GROUP_ADMIN" })
    void removeGroupAuthority( UserGroup group, String authority );

    // Renovations Phase 3: root cause turned out to be parameter-name discovery, not
    // method-security wiring (Phase 2 hypothesis) or ACL evaluation (Phase 3 mid-session
    // hypothesis). Spring's SpEL @PreAuthorize uses DefaultParameterNameDiscoverer to bind
    // `#group` to the second method argument. The discoverer falls through to JDK reflection
    // which needs parameter names in the bytecode -- emitted only when javac is invoked with
    // `-parameters`. gsec wasn't passing it, so `#group` silently resolved to null and the
    // SpEL evaluated `hasPermission(null, 'administration')` -> false for every caller
    // including admin. The fix is `<maven.compiler.parameters>true</maven.compiler.parameters>`
    // in gsec's pom.xml (added alongside this comment). With names available, `#group` binds
    // correctly and the original two-clause SpEL works as written -- no `hasAuthority` disjunct
    @PreAuthorize("hasPermission(#group, 'write') or hasPermission(#group, 'administration')")
    void removeUserFromGroup( User user, UserGroup group );

    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void update( User user );

    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void update( UserGroup group );
}
