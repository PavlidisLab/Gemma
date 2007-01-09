/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package ubic.gemma.model.common.auditAndSecurity;

import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.common.Securable;

/**
 * @see ubic.gemma.model.common.auditAndSecurity.UserGroupService
 * @author keshav
 * @version $Id$
 */
public class UserGroupServiceImpl extends ubic.gemma.model.common.auditAndSecurity.UserGroupServiceBase {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.UserGroupServiceBase#handleCreate(ubic.gemma.model.common.auditAndSecurity.User,
     *      java.lang.String, java.lang.String)
     */
    @Override
    protected Securable handleCreate( User owner, String name, String description ) throws Exception {

        // TODO don't pass in owner. You have access to the principal from the SecurityContextHolder.
        // Object obj = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        // String ownerUsername = obj.toString();
        UserGroup userGroup = UserGroup.Factory.newInstance();
        userGroup.setName( name );
        userGroup.setDescription( description );

        Collection<User> groupMembers = new HashSet<User>();
        groupMembers.add( owner );

        userGroup.setGroupMembers( groupMembers );

        return this.getUserGroupDao().create( userGroup );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.UserGroupServiceBase#handleUpdate(ubic.gemma.model.common.auditAndSecurity.User)
     */
    @Override
    protected UserGroup handleUpdate( String groupName, User groupMember ) throws Exception {
        // TODO add finder methods so you can find persistet group by groupMember name
        return null;
    }
}