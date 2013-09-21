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
 * An organized group of researchers with an identifiable leader and group members.
 */
public abstract class UserGroup extends ubic.gemma.model.common.Auditable implements gemma.gsec.model.SecuredNotChild {

    /**
     * Constructs new instances of {@link ubic.gemma.model.common.auditAndSecurity.UserGroup}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.common.auditAndSecurity.UserGroup}.
         */
        public static ubic.gemma.model.common.auditAndSecurity.UserGroup newInstance() {
            return new ubic.gemma.model.common.auditAndSecurity.UserGroupImpl();
        }

    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 5795744069086222179L;
    private Collection<User> groupMembers = new java.util.HashSet<>();

    private Collection<GroupAuthority> authorities = new java.util.HashSet<GroupAuthority>();

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public UserGroup() {
    }

    /**
     * 
     */
    public Collection<ubic.gemma.model.common.auditAndSecurity.GroupAuthority> getAuthorities() {
        return this.authorities;
    }

    /**
     * 
     */
    public Collection<ubic.gemma.model.common.auditAndSecurity.User> getGroupMembers() {
        return this.groupMembers;
    }

    public void setAuthorities( Collection<ubic.gemma.model.common.auditAndSecurity.GroupAuthority> authorities ) {
        this.authorities = authorities;
    }

    public void setGroupMembers( Collection<ubic.gemma.model.common.auditAndSecurity.User> groupMembers ) {
        this.groupMembers = groupMembers;
    }

}