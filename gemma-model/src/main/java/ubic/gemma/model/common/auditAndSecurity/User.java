/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2012 University of British Columbia
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
package ubic.gemma.model.common.auditAndSecurity;

import java.util.Collection;

/**
 * <p>
 * A user of the software system, who is authenticated.
 * </p>
 */
public abstract class User extends ubic.gemma.model.common.auditAndSecurity.PersonImpl implements
        gemma.gsec.model.SecuredNotChild {

    /**
     * Constructs new instances of {@link ubic.gemma.model.common.auditAndSecurity.User}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.common.auditAndSecurity.User}.
         */
        public static ubic.gemma.model.common.auditAndSecurity.User newInstance() {
            return new ubic.gemma.model.common.auditAndSecurity.UserImpl();
        }

    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -8345612050802728158L;
    private String userName;

    private String password;

    private String passwordHint;

    private Boolean enabled;

    private String signupToken;

    private java.util.Date signupTokenDatestamp;

    private Collection<UserRole> roles = new java.util.HashSet<ubic.gemma.model.common.auditAndSecurity.UserRole>();

    private Collection<JobInfo> jobs = new java.util.HashSet<JobInfo>();

    private Collection<UserQuery> savedQueries = new java.util.HashSet<UserQuery>();

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public User() {
    }

    /**
     * 
     */
    public Boolean getEnabled() {
        return this.enabled;
    }

    /**
     * 
     */
    public Collection<ubic.gemma.model.common.auditAndSecurity.JobInfo> getJobs() {
        return this.jobs;
    }

    /**
     * 
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * 
     */
    public String getPasswordHint() {
        return this.passwordHint;
    }

    /**
     * 
     */
    public Collection<ubic.gemma.model.common.auditAndSecurity.UserRole> getRoles() {
        return this.roles;
    }

    /**
     * 
     */
    public Collection<ubic.gemma.model.common.auditAndSecurity.UserQuery> getSavedQueries() {
        return this.savedQueries;
    }

    /**
     * 
     */
    public String getSignupToken() {
        return this.signupToken;
    }

    /**
     * 
     */
    public java.util.Date getSignupTokenDatestamp() {
        return this.signupTokenDatestamp;
    }

    /**
     * 
     */
    public String getUserName() {
        return this.userName;
    }

    public void setEnabled( Boolean enabled ) {
        this.enabled = enabled;
    }

    public void setJobs( Collection<ubic.gemma.model.common.auditAndSecurity.JobInfo> jobs ) {
        this.jobs = jobs;
    }

    public void setPassword( String password ) {
        this.password = password;
    }

    public void setPasswordHint( String passwordHint ) {
        this.passwordHint = passwordHint;
    }

    public void setRoles( Collection<ubic.gemma.model.common.auditAndSecurity.UserRole> roles ) {
        this.roles = roles;
    }

    public void setSavedQueries( Collection<ubic.gemma.model.common.auditAndSecurity.UserQuery> savedQueries ) {
        this.savedQueries = savedQueries;
    }

    public void setSignupToken( String signupToken ) {
        this.signupToken = signupToken;
    }

    public void setSignupTokenDatestamp( java.util.Date signupTokenDatestamp ) {
        this.signupTokenDatestamp = signupTokenDatestamp;
    }

    public void setUserName( String userName ) {
        this.userName = userName;
    }

}