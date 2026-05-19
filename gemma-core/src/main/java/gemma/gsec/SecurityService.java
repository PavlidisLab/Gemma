/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
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
package gemma.gsec;

import gemma.gsec.acl.voter.AclEntryCollectionVoter;
import gemma.gsec.model.Securable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.Sid;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * High-level methods for security-related manipulations.
 *
 * @author paul
 * @version $Id: SecurityService.java,v 1.98 2013/09/14 16:56:03 paul Exp $
 */
public interface SecurityService {

    /**
     * Check if the given securables are public.
     */
    <T extends Securable> Map<T, Boolean> arePublic( Collection<T> securables );

    /**
     * Check if the given securables are private.
     *
     * @throws AuthorizationServiceException if the collection is empty, see comments in
     *                                       {@link AclEntryCollectionVoter AclCollectionEntryVoter}
     */
    <T extends Securable> Map<T, Boolean> arePrivate( Collection<T> securables );

    /**
     * Check if the given securable are shared.
     *
     * @throws AuthorizationServiceException if the collection is empty, see comments in
     *                                       {@link AclEntryCollectionVoter AclCollectionEntryVoter}
     */
    <T extends Securable> Map<T, Boolean> areShared( Collection<T> securables );

    /**
     * Pick public securables.
     *
     * @return the subset that are public, if any
     */
    <T extends Securable> Collection<T> choosePublic( Collection<T> securables );

    /**
     * Pick private securables.
     *
     * @return the subset which are private, if any
     */
    <T extends Securable> Collection<T> choosePrivate( Collection<T> securables );

    /**
     * @return list of userNames of users who can read the given securable.
     */
    Collection<String> readableBy( Securable s );

    /**
     * Retrieve a list of users allowed to edit a given securable.
     *
     * @return list of userNames who can edit the given securable.
     * @throws AuthorizationServiceException if the collection is empty, see comments in
     *                                       {@link AclEntryCollectionVoter AclCollectionEntryVoter}
     */
    Collection<String> editableBy( Securable s );

    /**
     * Obtain the number of authenticated users.
     * <p>
     * We make this available to anonymous
     */
    int getAuthenticatedUserCount();

    /**
     * @return user names
     */
    @Secured("GROUP_ADMIN")
    Collection<String> getAuthenticatedUserNames();

    /**
     * Obtain a collection of all availble security IDs (basically, user names and group authorities).
     * <p>
     * This methods is only available to administrators.
     */
    @Secured("GROUP_ADMIN")
    Collection<Sid> getAvailableSids();

    List<String> getGroupAuthoritiesNameFromGroupName( String groupName );

    /**
     * @throws AuthorizationServiceException if the collection is empty, see comments in
     *                                       {@link AclEntryCollectionVoter AclCollectionEntryVoter}
     */
    @Secured({ "ACL_SECURABLE_COLLECTION_READ" })
    <T extends Securable> Map<T, Collection<String>> getGroupsEditableBy( Collection<T> securables );

    @Secured({ "ACL_SECURABLE_READ" })
    Collection<String> getGroupsEditableBy( Securable s );

    @Secured({ "ACL_SECURABLE_COLLECTION_READ" })
    <T extends Securable> Map<T, Collection<String>> getGroupsReadableBy( Collection<T> securables );

    /**
     * Obtain all the groups whose members can read a given object.
     */
    Collection<String> getGroupsReadableBy( Securable s );

    /**
     * Obtain all the groups that the user can edit.
     */
    Collection<String> getGroupsUserCanEdit( String userName );

    /**
     * Obtain the owner of a given object.
     */
    Sid getOwner( Securable s );

    /**
     * Obtain the owners of a collection of objects.
     * <p>
     * Pretty much have to be either the owner of the securables or administrator to call this.
     *
     * @throws AccessDeniedException if the current user is not allowed to access the information.
     */
    <T extends Securable> Map<T, Sid> getOwners( Collection<T> securables );

    /**
     * Determine if a securable is public (i.e. can be seen by anyone, including anonymous users).
     *
     * @see gemma.gsec.util.SecurityUtil#isPublic(Acl)
     */
    boolean isPublic( Securable s );

    /**
     * Determine if a securable is shared (i.e. can be seen by any registered users).
     *
     * @see gemma.gsec.util.SecurityUtil#isShared(Acl)
     */
    boolean isShared( Securable s );

    /**
     * Determine if a securable is private (i.e. can only be seen by its owner or an administrator).
     *
     * @return true if anonymous users can view (READ) the object, false otherwise. If the object doesn't have an ACL,
     * return true (be safe!)
     * @see org.springframework.security.acls.jdbc.BasicLookupStrategy
     * @see gemma.gsec.util.SecurityUtil#isPrivate(Acl)
     */
    boolean isPrivate( Securable s );

    /**
     * @return true if the owner is the same as the current authenticated user. Special case: if the owner is an
     * administrator, and the uc
     */
    boolean isOwnedByCurrentUser( Securable s );

    /**
     * Determine if the current user can read an object.
     */
    boolean isReadableByCurrentUser( Securable s );

    /**
     * @return true if the given user can read the securable, false otherwise. (READ or ADMINISTRATION required)
     */
    boolean isReadableByUser( Securable s, String userName );

    /**
     * Determine if members of a group is allowed to read an object.
     */
    boolean isReadableByGroup( Securable s, String groupName );

    /**
     * Determine if the current user can edit the securable.
     */
    boolean isEditableByCurrentUser( Securable s );

    /**
     * Determine if the given user is allowed to edit an object.
     */
    boolean isEditableByUser( Securable s, String userName );

    /**
     * Determine if the given group is allowed to edit an object.
     */
    boolean isEditableByGroup( Securable s, String groupName );

    /**
     * Make a collection of objects public.
     */
    @Secured("ACL_SECURABLE_COLLECTION_EDIT")
    void makePublic( Collection<? extends Securable> objs );

    /**
     * Makes the object public
     */
    @Secured("ACL_SECURABLE_EDIT")
    void makePublic( Securable object );

    /**
     * Make a collection of objects private.
     */
    @Secured("ACL_SECURABLE_COLLECTION_EDIT")
    void makePrivate( Collection<? extends Securable> objs );

    /**
     * Makes the object private.
     */
    @Secured("ACL_SECURABLE_EDIT")
    void makePrivate( Securable object );

    /**
     * Adds read permission for a given group.
     */
    @Secured("ACL_SECURABLE_EDIT")
    void makeReadableByGroup( Securable s, String groupName ) throws AccessDeniedException;

    /**
     * Remove read permissions; also removes write permissions.
     */
    @Secured("ACL_SECURABLE_EDIT")
    void makeUnreadableByGroup( Securable s, String groupName ) throws AccessDeniedException;

    /**
     * Remove write permissions. Leaves read permissions, if present.
     */
    @Secured("ACL_SECURABLE_EDIT")
    void makeUneditableByGroup( Securable s, String groupName ) throws AccessDeniedException;

    /**
     * Adds write (and read) permissions.
     */
    @Secured("ACL_SECURABLE_EDIT")
    void makeEditableByGroup( Securable s, String groupName ) throws AccessDeniedException;

    /**
     * Administrative method to allow a user to get access to an object. This is useful for cases where a data set is
     * loaded by admin but we need to hand it off to a user. If the user is the same as the current owner nothing is
     * done.
     * <p>
     * TODO: consider allowing a groupauthority to be the owner (GROUP_ADMIN) - see bug 2996
     */
    @Secured("GROUP_ADMIN")
    void makeOwnedByUser( Securable s, String userName );

    /**
     * Change the 'owner' of an object to a specific user. Note that this doesn't support making the owner a
     * grantedAuthority.
     */
    @Secured("GROUP_ADMIN")
    void setOwner( Securable s, String userName );

    /**
     * If the group already exists, an exception will be thrown.
     */
    void createGroup( String groupName );

    /**
     * Add a given user to a group by name.
     */
    void addUserToGroup( String userName, String groupName );

    /**
     * Remove a user from a group.
     */
    void removeUserFromGroup( String userName, String groupName );
}