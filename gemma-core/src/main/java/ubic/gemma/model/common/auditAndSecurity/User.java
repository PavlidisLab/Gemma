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
 * A user of the software system, who is authenticated.
 */
public class User extends Person implements gemma.gsec.model.User {

    private static final long serialVersionUID = 5910497757956461559L;
    private String userName;
    private String password;
    private String passwordHint;
    private Boolean enabled;
    private String signupToken;
    private java.util.Date signupTokenDatestamp;
    private Collection<JobInfo> jobs = new java.util.HashSet<>();

    @Override
    public Boolean getEnabled() {
        return this.enabled;
    }

    @Override
    public void setEnabled( Boolean enabled ) {
        this.enabled = enabled;
    }

    public Collection<JobInfo> getJobs() {
        return this.jobs;
    }

    public void setJobs( Collection<JobInfo> jobs ) {
        this.jobs = jobs;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public void setPassword( String password ) {
        this.password = password;
    }

    @Override
    public String getPasswordHint() {
        return this.passwordHint;
    }

    @Override
    public void setPasswordHint( String passwordHint ) {
        this.passwordHint = passwordHint;
    }

    @Override
    public String getSignupToken() {
        return this.signupToken;
    }

    @Override
    public void setSignupToken( String signupToken ) {
        this.signupToken = signupToken;
    }

    @Override
    public java.util.Date getSignupTokenDatestamp() {
        return this.signupTokenDatestamp;
    }

    @Override
    public void setSignupTokenDatestamp( java.util.Date signupTokenDatestamp ) {
        this.signupTokenDatestamp = signupTokenDatestamp;
    }

    @Override
    public String getUserName() {
        return this.userName;
    }

    @Override
    public void setUserName( String userName ) {
        this.userName = userName;
    }

    public static final class Factory {

        public static User newInstance() {
            return new User();
        }

    }

}