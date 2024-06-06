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
package ubic.gemma.web.controller.common.auditAndSecurity;

import gemma.gsec.model.Securable;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.remote.EntityDelegator;

import java.util.Collection;

/**
 * Note: do not use parameterized collections as parameters for ajax methods in this class! Type information is lost
 * during proxy creation so DWR can't figure out what type of collection the method should take. See bug 2756. Use
 * arrays instead.
 *
 * @author paul
 */
@SuppressWarnings("unused") // Used in front end
public interface SecurityController {

    boolean addUserToGroup( String userName, String groupName );

    String createGroup( String groupName );

    /**
     * AJAX
     *
     * @param groupName group name
     * @throws org.hibernate.exception.ConstraintViolationException cannot remove a group if it is being used to set any
     * permission, remove permission settings first (is thrown if the acl_entry table has rows with this sid)
     */
    void deleteGroup( String groupName );

    Integer getAuthenticatedUserCount();

    Collection<String> getAuthenticatedUserNames();

    /**
     * AJAX
     *
     * @return List of group names the user can add members to and/or give permissions on objects to.
     */
    Collection<UserGroupValueObject> getAvailableGroups();

    /**
     * Return a list of principals that is users
     *
     * @return SidValueObjects
     */
    Collection<SidValueObject> getAvailablePrincipalSids();

    /**
     * AJAX, but administrator-only!
     *
     * @return SID VOs
     */
    Collection<SidValueObject> getAvailableSids();

    Collection<UserValueObject> getGroupMembers( String groupName );

    SecurityInfoValueObject getSecurityInfo( EntityDelegator<? extends Securable> ed );

    /**
     * AJAX
     *
     * @param currentGroup A specific group that we're focusing on. Can be null. Used to populate client-side checkboxes
     *                     to show permissions.
     * @param privateOnly  Only show data that are private (non-publicly readable); otherwise show all the data for the
     *                     user. This option is probably of most use to administrators.
     * @return security info VO
     */
    Collection<SecurityInfoValueObject> getUsersData( String currentGroup, boolean privateOnly );

    boolean makeGroupReadable( EntityDelegator<? extends Securable> ed, String groupName );

    boolean makeGroupWriteable( EntityDelegator<? extends Securable> ed, String groupName );

    boolean makePrivate( EntityDelegator<? extends Securable> ed );

    boolean makePublic( EntityDelegator<? extends Securable> ed );

    boolean removeGroupReadable( EntityDelegator<? extends Securable> ed, String groupName );

    boolean removeGroupWriteable( EntityDelegator<? extends Securable> ed, String groupName );

    boolean removeUsersFromGroup( String[] userNames, String groupName );

    void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService );

    SecurityInfoValueObject updatePermission( SecurityInfoValueObject settings );

    void updatePermissions( SecurityInfoValueObject[] settings );

}