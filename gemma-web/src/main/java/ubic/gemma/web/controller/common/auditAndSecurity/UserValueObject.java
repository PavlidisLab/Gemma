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

import gemma.gsec.model.User;

/**
 * @author keshav
 * @version $Id$
 */
public class UserValueObject {
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
        enabled = user.getEnabled();
        password = user.getPassword();
    }

    public String getCurrentGroup() {
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
