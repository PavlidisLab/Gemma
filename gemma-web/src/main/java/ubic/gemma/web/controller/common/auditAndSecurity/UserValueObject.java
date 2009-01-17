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

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserRole;

/**
 * @author keshav
 * @version $Id$
 */
public class UserValueObject {
    private Log log = LogFactory.getLog( this.getClass() );

    private String userName;
    private String email;
    private String role;
    private boolean enabled;

    public UserValueObject() {
        super();
    }

    public UserValueObject( User user ) {
        userName = user.getUserName();
        email = user.getEmail();
        enabled = user.getEnabled();
        Collection<UserRole> roles = user.getRoles();
        if ( roles.size() > 1 ) {
            log.error( "Too many roles for user " + user.getName() + ".  Not setting role on "
                    + UserValueObject.class.getName() );
        } else {
            role = roles.iterator().next().getName();
        }

    }

    public String getEmail() {
        return email;
    }

    public void setEmail( String email ) {
        this.email = email;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled( boolean enabled ) {
        this.enabled = enabled;
    }

    public String getRole() {
        return role;
    }

    public void setRole( String role ) {
        this.role = role;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName( String userName ) {
        this.userName = userName;
    }

}
