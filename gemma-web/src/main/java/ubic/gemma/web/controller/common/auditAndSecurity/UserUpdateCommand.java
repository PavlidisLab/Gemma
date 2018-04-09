/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * Licensed under the Apache License; Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing; software
 * distributed under the License is distributed on an "AS IS" BASIS;
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND; either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.web.controller.common.auditAndSecurity;

import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.JobInfo;
import ubic.gemma.model.common.auditAndSecurity.User;

import java.util.Collection;

/**
 * Just like a regular user; but has 'new password' and 'confirm password' fields. It can be constructed from a user. To
 * convert to a user object, use the asUser() method.
 *
 * @author pavlidis
 */
@SuppressWarnings("unused") // Used in front end
public class UserUpdateCommand {

    private String oldPassword = null;
    private String newPassword = null;
    private String confirmNewPassword = null;
    private Boolean adminUser = false;

    // stored so this can be used to modify a persistent instance.
    private User user;

    public UserUpdateCommand() {
        this.user = User.Factory.newInstance();
    }

    public User asUser() {
        return user;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        UserUpdateCommand other = ( UserUpdateCommand ) obj;
        if ( user == null ) {
            return other.user == null;
        } else
            return user.equals( other.user );
    }

    public Boolean getAdminUser() {
        return this.adminUser;
    }

    public void setAdminUser( Boolean adminUser ) {
        this.adminUser = adminUser;
    }

    public AuditTrail getAuditTrail() {
        return this.user.getAuditTrail();
    }

    public void setAuditTrail( AuditTrail auditTrail ) {
        this.user.setAuditTrail( auditTrail );
    }

    /**
     * @return the confirmNewPassword
     */
    public String getConfirmNewPassword() {
        return this.confirmNewPassword;
    }

    /**
     * @param confirmNewPassword the confirmNewPassword to set
     */
    public void setConfirmNewPassword( String confirmNewPassword ) {
        this.confirmNewPassword = confirmNewPassword;
    }

    public String getDescription() {
        return this.user.getDescription();
    }

    public void setDescription( String description ) {
        this.user.setDescription( description );
    }

    public String getEmail() {
        return this.user.getEmail();
    }

    public void setEmail( String email ) {
        this.user.setEmail( email );
    }

    public Boolean getEnabled() {
        return this.user.getEnabled();
    }

    public void setEnabled( Boolean enabled ) {
        this.user.setEnabled( enabled );
    }

    public String getFullName() {
        return this.user.getFullName();
    }

    public Long getId() {
        return this.user.getId();
    }

    public void setId( Long id ) {
        this.user.setId( id );
    }

    public Collection<JobInfo> getJobs() {
        return this.user.getJobs();
    }

    public void setJobs( Collection<JobInfo> jobs ) {
        this.user.setJobs( jobs );
    }

    public String getLastName() {
        return this.user.getLastName();
    }

    public void setLastName( String lastName ) {
        this.user.setLastName( lastName );
    }

    public String getName() {
        return this.user.getName();
    }

    public void setName( String name ) {
        this.user.setName( name );
    }

    /**
     * @return the newPassword
     */
    public String getNewPassword() {
        return this.newPassword;
    }

    public void setNewPassword( String newPassword ) {
        this.newPassword = newPassword;
    }

    /**
     * @return the oldPassword
     */
    public String getOldPassword() {
        return this.oldPassword;
    }

    public void setOldPassword( String oldPassword ) {
        this.oldPassword = oldPassword;
    }

    public String getPassword() {
        return this.user.getPassword();
    }

    public void setPassword( String password ) {
        this.user.setPassword( password );
    }

    public String getPasswordHint() {
        return this.user.getPasswordHint();
    }

    public void setPasswordHint( String passwordHint ) {
        this.user.setPasswordHint( passwordHint );
    }

    public String getUserName() {
        return this.user.getUserName();
    }

    public void setUserName( String userName ) {
        this.user.setUserName( userName );
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( user == null ) ? 0 : user.hashCode() );
        return result;
    }

    @Override
    public String toString() {
        return this.user.toString();
    }

}
