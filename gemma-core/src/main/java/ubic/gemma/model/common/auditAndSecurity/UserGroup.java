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

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import ubic.gemma.model.common.DescribableUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * An organized group of researchers with an identifiable leader and group members.
 *
 * @author Paul
 */
@Entity
@Table(name = "USER_GROUP")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class UserGroup extends AbstractAuditable implements ubic.gemma.core.security.model.UserGroup {

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "GROUP_MEMBERS",
            joinColumns = @JoinColumn(name = "USER_GROUPS_FK", columnDefinition = "BIGINT"),
            inverseJoinColumns = @JoinColumn(name = "GROUP_MEMBERS_FK", columnDefinition = "BIGINT"),
            foreignKey = @ForeignKey(name = "USER_USER_GROUPS_FKC"))
    private Set<User> groupMembers = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "GROUP_FK", columnDefinition = "BIGINT", foreignKey = @ForeignKey(name = "GROUP_AUTHORITY_GROUP_FKC"))
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<GroupAuthority> authorities = new HashSet<>();

    /**
     * No-arg constructor added to satisfy javabean contract
     */
    public UserGroup() {
    }

    @Override
    public Set<GroupAuthority> getAuthorities() {
        return this.authorities;
    }

    public void setAuthorities( Set<GroupAuthority> authorities ) {
        this.authorities = authorities;
    }

    @Override
    public Set<User> getGroupMembers() {
        return this.groupMembers;
    }

    public void setGroupMembers( Set<User> groupMembers ) {
        this.groupMembers = groupMembers;
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object )
            return true;
        if ( !( object instanceof UserGroup ) )
            return false;
        UserGroup that = ( UserGroup ) object;
        if ( this.getId() != null && that.getId() != null ) {
            return getId().equals( that.getId() );
        }
        return DescribableUtils.equalsByName( this, that );
    }

    public static final class Factory {

        public static UserGroup newInstance() {
            return new UserGroup();
        }

    }

}
