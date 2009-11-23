/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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

import ubic.gemma.persistence.BaseDao;

/**
 * @see ubic.gemma.model.common.auditAndSecurity.User
 * @version $Id$
 * @author Gemma
 */
public interface UserDao extends BaseDao<User> {

    /**
     * 
     */
    public void addAuthority( ubic.gemma.model.common.auditAndSecurity.User user, java.lang.String roleName );

    /**
     * @param user
     * @param password - encrypted
     */
    public void changePassword( ubic.gemma.model.common.auditAndSecurity.User user, String password );

    /**
     * @param contact
     * @return
     */
    public User find( User contact );

    /**
     * 
     */
    public ubic.gemma.model.common.auditAndSecurity.User findByEmail( java.lang.String email );

    /**
     * 
     */
    public ubic.gemma.model.common.auditAndSecurity.User findByUserName( java.lang.String userName );

    /**
     * @param u
     * @return
     */
    public Collection<GroupAuthority> loadGroupAuthorities( User u );

    /**
     * @param user
     * @return
     */
    public Collection<UserGroup> loadGroups( User user );

}
