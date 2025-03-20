/*
 * The Gemma project
 *
 * Copyright (c) 2009 University of British Columbia
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
package ubic.gemma.web.controller.common.auditAndSecurity;

import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserGroup;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * @author keshav
 *
 */
public class UserValueObject {

    private Set<String> groups;
    private String currentGroup;
    private String email;
    private boolean enabled;
    private boolean inGroup;

    /*
     * Should the client be allowed to edit the user (e.g., group membership or enabledness - used to protect 'special'
     * users like the admin.
     */
    private boolean allowModification;

    private String password;

    private String userName;

    public UserValueObject() {
        super();
    }

    public UserValueObject( User user ) {
        userName = user.getUserName();
        email = user.getEmail();
        enabled = user.isEnabled();
        password = user.getPassword();
        groups = user.getGroups().stream().map( UserGroup::getName ).collect( Collectors.toCollection( TreeSet::new ) );
        // FIXME: select the current group more intelligently
        currentGroup = groups.stream().findAny().orElse( null );
    }

    public Set<String> getGroups() {
        return groups;
    }

    public void setGroups( Set<String> groups ) {
        this.groups = groups;
    }

    /**
     * @deprecated use {@link #getGroups()} instead
     */
    @Deprecated
    public String getCurrentGroup() {
        return currentGroup;
    }

    /**
     * @deprecated use {@link #getGroups()} instead
     */
    @Deprecated
    public String getRole() {
        return currentGroup;
    }

    public String getEmail() {
        return email;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    public String getUserName() {
        return userName;
    }

    public boolean isAllowModification() {
        return allowModification;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isInGroup() {
        return inGroup;
    }

    public void setAllowModification( boolean allowModification ) {
        this.allowModification = allowModification;
    }

    public void setCurrentGroup( String currentGroup ) {
        this.currentGroup = currentGroup;
    }

    public void setEmail( String email ) {
        this.email = email;
    }

    public void setEnabled( boolean enabled ) {
        this.enabled = enabled;
    }

    public void setInGroup( boolean inGroup ) {
        this.inGroup = inGroup;
    }

    public void setUserName( String userName ) {
        this.userName = userName;
    }

}
