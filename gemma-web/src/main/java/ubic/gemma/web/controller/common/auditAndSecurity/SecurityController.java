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

import java.util.Collection;

import org.springframework.dao.DataIntegrityViolationException;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.web.remote.EntityDelegator;

/**
 * Note: do not use parameterized collections as parameters for ajax methods in this class! Type information is lost
 * during proxy creation so DWR can't figure out what type of collection the method should take. See bug 2756. Use
 * arrays instead.
 * 
 * @author paul
 * @version $Id$
 */
public interface SecurityController {

    /**
     * AJAX
     * 
     * @param userName
     * @param groupName
     * @return
     */
    public abstract boolean addUserToGroup( String userName, String groupName );

    /**
     * AJAX
     * 
     * @param groupName
     * @return
     */
    public abstract String createGroup( String groupName );

    /**
     * AJAX
     * 
     * @param groupName
     * @throws DataIntegrityViolationException cannot delete a group if it is being used to set any permission, delete
     *         permission settings first (is thrown if the acl_entry table has rows with this sid)
     */
    public abstract void deleteGroup( String groupName );

    /**
     * @return
     * @see ubic.gemma.security.SecurityServiceImpl#getAuthenticatedUserCount()
     */
    public abstract Integer getAuthenticatedUserCount();

    /**
     * @return
     * @see ubic.gemma.security.SecurityServiceImpl#getAuthenticatedUserNames()
     */
    public abstract Collection<String> getAuthenticatedUserNames();

    /**
     * AJAX
     * 
     * @return List of group names the user can add members to and/or give permissions on objects to.
     */
    public abstract Collection<UserGroupValueObject> getAvailableGroups();

    /**
     * Return a list of principals that is users
     * 
     * @return SidValueObjects
     */
    public abstract Collection<SidValueObject> getAvailablePrincipalSids();

    /**
     * AJAX, but administrator-only!
     * 
     * @return
     */
    public abstract Collection<SidValueObject> getAvailableSids();

    /**
     * @param groupName
     * @return
     */
    public abstract Collection<UserValueObject> getGroupMembers( String groupName );

    /**
     * AJAX
     * 
     * @param ed
     * @return
     */
    public abstract SecurityInfoValueObject getSecurityInfo( EntityDelegator ed );

    /**
     * AJAX
     * 
     * @param currentGroup A specific group that we're focusing on. Can be null. Used to populate client-side checkboxes
     *        to show permissions.
     * @param privateOnly Only show data that are private (non-publicly readable); otherwise show all the data for the
     *        user. This option is probably of most use to administrators.
     * @return
     */
    public abstract Collection<SecurityInfoValueObject> getUsersData( String currentGroup, boolean privateOnly );

    /**
     * AJAX
     * 
     * @param ed
     * @param groupName
     * @return
     */
    public abstract boolean makeGroupReadable( EntityDelegator ed, String groupName );

    /**
     * AJAX
     * 
     * @param ed
     * @param groupName
     * @return
     */
    public abstract boolean makeGroupWriteable( EntityDelegator ed, String groupName );

    /**
     * AJAX
     * 
     * @param ed
     * @return
     */
    public abstract boolean makePrivate( EntityDelegator ed );

    /**
     * AJAX
     * 
     * @param ed
     * @return
     */
    public abstract boolean makePublic( EntityDelegator ed );

    /**
     * AJAX
     * 
     * @param ed
     * @param groupName
     * @return
     */
    public abstract boolean removeGroupReadable( EntityDelegator ed, String groupName );

    /**
     * AJAX
     * 
     * @param ed
     * @param groupName
     * @return
     */
    public abstract boolean removeGroupWriteable( EntityDelegator ed, String groupName );

    /**
     * AJAX
     * 
     * @param userNames
     * @param groupName
     * @return
     */
    public abstract boolean removeUsersFromGroup( String[] userNames, String groupName );

    /**
     * @param expressionExperimentService
     */
    public abstract void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService );

    /**
     * AJAX
     * 
     * @param settings
     * @return updated value object
     */
    public abstract SecurityInfoValueObject updatePermission( SecurityInfoValueObject settings );

    /**
     * AJAX
     * 
     * @param settings
     */
    public abstract void updatePermissions( SecurityInfoValueObject[] settings );

}