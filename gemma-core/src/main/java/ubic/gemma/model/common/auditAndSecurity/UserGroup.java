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

import gemma.gsec.model.GroupAuthority;
import gemma.gsec.model.User;

import java.util.HashSet;
import java.util.Set;

/**
 * An organized group of researchers with an identifiable leader and group members.
 *
 * @author Paul
 */
public class UserGroup extends AbstractAuditable implements gemma.gsec.model.UserGroup {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 5795744069086222179L;
    private Set<User> groupMembers = new HashSet<>();
    private Set<GroupAuthority> authorities = new HashSet<>();

    /**
     * No-arg constructor added to satisfy javabean contract
     */
    public UserGroup() {
    }

    @SuppressWarnings({ "unchecked", "JpaAttributeMemberSignatureInspection" })
    @Override
    public Set<GroupAuthority> getAuthorities() {
        return this.authorities;
    }

    @SuppressWarnings("unchecked")
    public void setAuthorities( Set<GroupAuthority> authorities ) {
        this.authorities = authorities;
    }

    @SuppressWarnings({ "unchecked", "JpaAttributeMemberSignatureInspection" })
    @Override
    public Set<User> getGroupMembers() {
        return this.groupMembers;
    }

    @SuppressWarnings("unchecked")
    public void setGroupMembers( Set<User> groupMembers ) {
        this.groupMembers = groupMembers;
    }

    public static final class Factory {

        public static UserGroup newInstance() {
            return new UserGroup();
        }

    }

}