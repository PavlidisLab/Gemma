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

/**
 * @version $Id$
 * @author paul
 */
public interface UserService {

    /**
     * 
     */
    public void addRole( ubic.gemma.model.common.auditAndSecurity.User user, java.lang.String roleName );

    /**
     * <p>
     * Saves a user's information
     * </p>
     */
    public ubic.gemma.model.common.auditAndSecurity.User create( ubic.gemma.model.common.auditAndSecurity.User user )
            throws ubic.gemma.model.common.auditAndSecurity.UserExistsException;

    /**
     * <p>
     * Removes a user from the database by their username
     * </p>
     */
    public void delete( java.lang.String userName );

    /**
     * 
     */
    public ubic.gemma.model.common.auditAndSecurity.User findByEmail( java.lang.String email );

    /**
     * 
     */
    public ubic.gemma.model.common.auditAndSecurity.User findByUserName( java.lang.String userName );

    /**
     * 
     */
    public ubic.gemma.model.common.auditAndSecurity.User load( java.lang.Long id );

    /**
     * <p>
     * Retrieves a list of users, filtering with parameters on a user object
     * </p>
     */
    public java.util.Collection<User> loadAll();

    /**
     * <p>
     * Returns a list of possible roles, used to populate admin tool where roles can be altered.
     * </p>
     */
    public java.util.Collection<UserRole> loadAllRoles();

    /**
     * 
     */
    public void update( ubic.gemma.model.common.auditAndSecurity.User user );

}
