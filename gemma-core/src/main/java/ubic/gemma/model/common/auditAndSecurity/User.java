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

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A user of the software system, who is authenticated.
 */
@Entity
@DiscriminatorValue("User")
@Table(indexes = @Index(name = "CONTACT_DELETED_AT_IDX", columnList = "DELETED_AT"))
public class User extends Person implements ubic.gemma.core.security.model.User {

    @Column(name = "USER_NAME", unique = true, updatable = false, columnDefinition = "VARCHAR(255)")
    private String userName;

    @Column(name = "PASSWORD", columnDefinition = "VARCHAR(255)")
    private String password;

    @Column(name = "PASSWORD_HINT", columnDefinition = "VARCHAR(255)")
    private String passwordHint;

    @Column(name = "ENABLED", columnDefinition = "TINYINT")
    private boolean enabled;

    @Column(name = "SIGNUP_TOKEN", columnDefinition = "VARCHAR(255)")
    private String signupToken;

    @Column(name = "SIGNUP_TOKEN_DATESTAMP", columnDefinition = "DATETIME(3)")
    private java.util.Date signupTokenDatestamp;

    @Column(name = "DELETED_AT", columnDefinition = "DATETIME(3)")
    private java.util.Date deletedAt;

    @Column(name = "DELETED_BY", columnDefinition = "VARCHAR(255)")
    private String deletedBy;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<JobInfo> jobs = new java.util.HashSet<>();

    @ManyToMany(mappedBy = "groupMembers", fetch = FetchType.LAZY)
    private Set<UserGroup> groups = new HashSet<>();

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled( boolean enabled ) {
        this.enabled = enabled;
    }

    public Set<JobInfo> getJobs() {
        return this.jobs;
    }

    public void setJobs( Set<JobInfo> jobs ) {
        this.jobs = jobs;
    }

    @SuppressWarnings("JpaAttributeMemberSignatureInspection")
    public Collection<UserGroup> getGroups() {
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

    /**
     * Timestamp of the admin soft-delete; null when the account is active.
     * Hard-deleting users is forbidden — ACL sids, audit-event authors, and
     * other references would dangle. See {@code V16__user_soft_delete.sql}.
     */
    public java.util.Date getDeletedAt() {
        return this.deletedAt;
    }

    public void setDeletedAt( java.util.Date deletedAt ) {
        this.deletedAt = deletedAt;
    }

    /**
     * Username of the admin who soft-deleted this account; null when active.
     * Free-form string (not an FK) so admin churn doesn't cascade.
     */
    public String getDeletedBy() {
        return this.deletedBy;
    }

    public void setDeletedBy( String deletedBy ) {
        this.deletedBy = deletedBy;
    }

    @Override
    public String getUserName() {
        return this.userName;
    }

    /**
     * The username is immutable. See the {@code update="false"} field in the Hibernate mapping.
     */
    public void setUserName( String userName ) {
        this.userName = userName;
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof User ) ) {
            return false;
        }
        User user = ( User ) object;
        return Objects.equals( userName, user.userName );
    }

    @Override
    public int hashCode() {
        return Objects.hash( userName );
    }

    public static final class Factory {

        public static User newInstance( String userName ) {
            User u = new User();
            u.setUserName( userName );
            return u;
        }
    }

}
