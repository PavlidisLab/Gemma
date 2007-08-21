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

import java.util.Collection;

import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.JobInfo;
import ubic.gemma.model.common.auditAndSecurity.Organization;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserRole;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.NotedReferenceList;

/**
 * Just like a regular user; but has 'new password' and 'confirm password' fields. It can be constructed from a user. To
 * convert to a user object, use the asUser() method.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class UserUpdateCommand {

    private String oldPassword = null;
    private String newPassword = null;
    private String confirmNewPassword = null;
    private Boolean adminUser = false;
    private Collection roles = null;

    // stored so this can be used to modify a persistent instance.
    private User user;

    public UserUpdateCommand() {
        this.user = User.Factory.newInstance();
    }

    public UserUpdateCommand( User user ) {
        fromUser( user );
    }

    /**
     * @param address
     * @see ubic.gemma.model.common.auditAndSecurity.Contact#setAddress(java.lang.String)
     */
    public void setAddress( String address ) {
        this.user.setAddress( address );
    }

    /**
     * @param affiliations
     * @see ubic.gemma.model.common.auditAndSecurity.Person#setAffiliations(java.util.Collection)
     */
    public void setAffiliations( Collection<Organization> affiliations ) {
        this.user.setAffiliations( affiliations );
    }

    /**
     * @param auditTrail
     * @see ubic.gemma.model.common.Auditable#setAuditTrail(ubic.gemma.model.common.auditAndSecurity.AuditTrail)
     */
    public void setAuditTrail( AuditTrail auditTrail ) {
        this.user.setAuditTrail( auditTrail );
    }

    /**
     * @param description
     * @see ubic.gemma.model.common.Describable#setDescription(java.lang.String)
     */
    public void setDescription( String description ) {
        this.user.setDescription( description );
    }

    /**
     * @param email
     * @see ubic.gemma.model.common.auditAndSecurity.Contact#setEmail(java.lang.String)
     */
    public void setEmail( String email ) {
        this.user.setEmail( email );
    }

    /**
     * @param enabled
     * @see ubic.gemma.model.common.auditAndSecurity.User#setEnabled(java.lang.Boolean)
     */
    public void setEnabled( Boolean enabled ) {
        this.user.setEnabled( enabled );
    }

    /**
     * @param fax
     * @see ubic.gemma.model.common.auditAndSecurity.Contact#setFax(java.lang.String)
     */
    public void setFax( String fax ) {
        this.user.setFax( fax );
    }

    /**
     * @param id
     * @see ubic.gemma.model.common.Securable#setId(java.lang.Long)
     */
    public void setId( Long id ) {
        this.user.setId( id );
    }

    /**
     * @param job
     * @see ubic.gemma.model.common.auditAndSecurity.User#setJob(java.util.Collection)
     */
    public void setJobs( Collection<JobInfo> jobs ) {
        this.user.setJobs( jobs );
    }

    /**
     * @param lastName
     * @see ubic.gemma.model.common.auditAndSecurity.Person#setLastName(java.lang.String)
     */
    public void setLastName( String lastName ) {
        this.user.setLastName( lastName );
    }

    /**
     * @param name
     * @see ubic.gemma.model.common.Describable#setName(java.lang.String)
     */
    public void setName( String name ) {
        this.user.setName( name );
    }

    /**
     * @param password
     * @see ubic.gemma.model.common.auditAndSecurity.User#setPassword(java.lang.String)
     */
    public void setPassword( String password ) {
        this.user.setPassword( password );
    }

    /**
     * @param passwordHint
     * @see ubic.gemma.model.common.auditAndSecurity.User#setPasswordHint(java.lang.String)
     */
    public void setPasswordHint( String passwordHint ) {
        this.user.setPasswordHint( passwordHint );
    }

    /**
     * @param personPublications
     * @see ubic.gemma.model.common.auditAndSecurity.Person#setPersonPublications(java.util.Collection)
     */
    public void setPersonPublications( Collection<BibliographicReference> personPublications ) {
        this.user.setPersonPublications( personPublications );
    }

    /**
     * @param phone
     * @see ubic.gemma.model.common.auditAndSecurity.Contact#setPhone(java.lang.String)
     */
    public void setPhone( String phone ) {
        this.user.setPhone( phone );
    }

    /**
     * @param referenceLists
     * @see ubic.gemma.model.common.auditAndSecurity.User#setReferenceLists(java.util.Collection)
     */
    public void setReferenceLists( Collection<NotedReferenceList> referenceLists ) {
        this.user.setReferenceLists( referenceLists );
    }

    /**
     * @param roles
     * @see ubic.gemma.model.common.auditAndSecurity.User#setRoles(java.util.Collection)
     */
    public void setRoles( Collection<UserRole> roles ) {
        this.roles = roles;
        this.user.setRoles( this.roles );
    }

    /**
     * @param tollFreePhone
     * @see ubic.gemma.model.common.auditAndSecurity.Contact#setTollFreePhone(java.lang.String)
     */
    public void setTollFreePhone( String tollFreePhone ) {
        this.user.setTollFreePhone( tollFreePhone );
    }

    /**
     * @param URL
     * @see ubic.gemma.model.common.auditAndSecurity.Contact#setURI(java.lang.String)
     */
    public void setURL( String URL ) {
        this.user.setURL( URL );
    }

    /**
     * @param userName
     * @see ubic.gemma.model.common.auditAndSecurity.User#setUserName(java.lang.String)
     */
    public void setUserName( String userName ) {
        this.user.setUserName( userName );
    }

    /**
     * @param user
     */
    private void fromUser( User newUser ) {
        this.user = newUser;

        this.setUserName( user.getUserName() );
        this.setPassword( user.getPassword() );
        this.setPasswordHint( user.getPasswordHint() );
        this.setEnabled( user.getEnabled() );
        this.setLastName( user.getLastName() );
        this.setName( user.getName() );
        this.setURL( user.getURL() );
        this.setAddress( user.getAddress() );
        this.setPhone( user.getPhone() );
        this.setTollFreePhone( user.getTollFreePhone() );
        this.setEmail( user.getEmail() );
        this.setFax( user.getFax() );
        this.setName( user.getName() );
        this.setDescription( user.getDescription() );
        this.setReferenceLists( user.getReferenceLists() );
        this.setRoles( user.getRoles() );
        this.setJobs( user.getJobs() );
        this.setAffiliations( user.getAffiliations() );
        this.setPersonPublications( user.getPersonPublications() );
        this.setAuditTrail( user.getAuditTrail() );

        this.setId( user.getId() );
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

    /**
     * @return the newPassword
     */
    public String getNewPassword() {
        return this.newPassword;
    }

    /**
     * @param newPassword the newPassword to set
     */
    public void setNewPassword( String newPassword ) {
        this.newPassword = newPassword;
    }

    /**
     * @return the oldPassword
     */
    public String getOldPassword() {
        return this.oldPassword;
    }

    /**
     * @param oldPassword the oldPassword to set
     */
    public void setOldPassword( String oldPassword ) {
        this.oldPassword = oldPassword;
    }

    /**
     * @return
     */
    public User asUser() {
        return user;
    }

    /**
     * @param object
     * @return
     * @see ubic.gemma.model.common.auditAndSecurity.User#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object object ) {
        return this.user.equals( object );
    }

    /**
     * @return
     * @see ubic.gemma.model.common.auditAndSecurity.Contact#getAddress()
     */
    public String getAddress() {
        return this.user.getAddress();
    }

    /**
     * @return
     * @see ubic.gemma.model.common.auditAndSecurity.Person#getAffiliations()
     */
    public Collection<Organization> getAffiliations() {
        return this.user.getAffiliations();
    }

    /**
     * @return
     * @see ubic.gemma.model.common.Auditable#getAuditTrail()
     */
    public AuditTrail getAuditTrail() {
        return this.user.getAuditTrail();
    }

    /**
     * @return
     * @see ubic.gemma.model.common.Describable#getDescription()
     */
    public String getDescription() {
        return this.user.getDescription();
    }

    /**
     * @return
     * @see ubic.gemma.model.common.auditAndSecurity.Contact#getEmail()
     */
    public String getEmail() {
        return this.user.getEmail();
    }

    /**
     * @return
     * @see ubic.gemma.model.common.auditAndSecurity.User#getEnabled()
     */
    public Boolean getEnabled() {
        return this.user.getEnabled();
    }

    /**
     * @return
     * @see ubic.gemma.model.common.auditAndSecurity.Contact#getFax()
     */
    public String getFax() {
        return this.user.getFax();
    }

    /**
     * @return
     * @see ubic.gemma.model.common.auditAndSecurity.PersonImpl#getFullName()
     */
    public String getFullName() {
        return this.user.getFullName();
    }

    /**
     * @return
     * @see ubic.gemma.model.common.Securable#getId()
     */
    public Long getId() {
        return this.user.getId();
    }

    /**
     * @return
     * @see ubic.gemma.model.common.auditAndSecurity.User#getJob()
     */
    public Collection<JobInfo> getJobs() {
        return this.user.getJobs();
    }

    /**
     * @return
     * @see ubic.gemma.model.common.auditAndSecurity.Person#getLastName()
     */
    public String getLastName() {
        return this.user.getLastName();
    }

    /**
     * @return
     * @see ubic.gemma.model.common.Describable#getName()
     */
    public String getName() {
        return this.user.getName();
    }

    /**
     * @return
     * @see ubic.gemma.model.common.auditAndSecurity.User#getPassword()
     */
    public String getPassword() {
        return this.user.getPassword();
    }

    /**
     * @return
     * @see ubic.gemma.model.common.auditAndSecurity.User#getPasswordHint()
     */
    public String getPasswordHint() {
        return this.user.getPasswordHint();
    }

    /**
     * @return
     * @see ubic.gemma.model.common.auditAndSecurity.Person#getPersonPublications()
     */
    public Collection<BibliographicReference> getPersonPublications() {
        return this.user.getPersonPublications();
    }

    /**
     * @return
     * @see ubic.gemma.model.common.auditAndSecurity.Contact#getPhone()
     */
    public String getPhone() {
        return this.user.getPhone();
    }

    /**
     * @return
     * @see ubic.gemma.model.common.auditAndSecurity.User#getReferenceLists()
     */
    public Collection<NotedReferenceList> getReferenceLists() {
        return this.user.getReferenceLists();
    }

    /**
     * @return
     * @see ubic.gemma.model.common.auditAndSecurity.User#getRoles()
     */
    public Collection<UserRole> getRoles() {
        roles = this.user.getRoles();
        return roles;
    }

    /**
     * @return
     * @see ubic.gemma.model.common.auditAndSecurity.Contact#getTollFreePhone()
     */
    public String getTollFreePhone() {
        return this.user.getTollFreePhone();
    }

    /**
     * @return
     * @see ubic.gemma.model.common.auditAndSecurity.Contact#getURI()
     */
    public String getURL() {
        return this.user.getURL();
    }

    /**
     * @return
     * @see ubic.gemma.model.common.auditAndSecurity.User#getUserName()
     */
    public String getUserName() {
        return this.user.getUserName();
    }

    /**
     * @return
     * @see ubic.gemma.model.common.auditAndSecurity.User#hashCode()
     */
    @Override
    public int hashCode() {
        return this.user.hashCode();
    }

    /**
     * @return
     * @see ubic.gemma.model.common.DescribableImpl#toString()
     */
    @Override
    public String toString() {
        return this.user.toString();
    }

    public Boolean getAdminUser() {
        return this.adminUser;
    }

    public void setAdminUser( Boolean adminUser ) {
        this.adminUser = adminUser;
    }
}
