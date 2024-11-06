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

import java.util.HashSet;
import java.util.Set;

/**
 * A user of the software system, who is authenticated.
 */
public class User extends Person implements gemma.gsec.model.User {

    private String userName;
    private String password;
    private String passwordHint;
    private Boolean enabled;
    private String signupToken;
    private java.util.Date signupTokenDatestamp;
    private Set<JobInfo> jobs = new java.util.HashSet<>();
    private Set<UserGroup> groups = new HashSet<>();

    @Override
    public Boolean getEnabled() {
        return this.enabled;
    }

    public void setEnabled( Boolean enabled ) {
        this.enabled = enabled;
    }

    public Set<JobInfo> getJobs() {
        return this.jobs;
    }

    public void setJobs( Set<JobInfo> jobs ) {
        this.jobs = jobs;
    }

    public Set<UserGroup> getGroups() {
        return groups;
    }

    public void setGroups( Set<UserGroup> groups ) {
        this.groups = groups;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    public void setPassword( String password ) {
        this.password = password;
    }

    @Override
    public String getPasswordHint() {
        return this.passwordHint;
    }

    public void setPasswordHint( String passwordHint ) {
        this.passwordHint = passwordHint;
    }

    @Override
    public String getSignupToken() {
        return this.signupToken;
    }

    public void setSignupToken( String signupToken ) {
        this.signupToken = signupToken;
    }

    @Override
    public java.util.Date getSignupTokenDatestamp() {
        return this.signupTokenDatestamp;
    }

    public void setSignupTokenDatestamp( java.util.Date signupTokenDatestamp ) {
        this.signupTokenDatestamp = signupTokenDatestamp;
    }

    @Override
    public String getUserName() {
        return this.userName;
    }

    public void setUserName( String userName ) {
        this.userName = userName;
    }

    public static final class Factory {

        public static User newInstance() {
            return new User();
        }

    }

}