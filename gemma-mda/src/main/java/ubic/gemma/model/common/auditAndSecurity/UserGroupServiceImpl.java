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

import org.springframework.security.context.SecurityContextHolder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.common.Securable;

/**
 * @see ubic.gemma.model.common.auditAndSecurity.UserGroupService
 * @author keshav
 * @version $Id$
 */
public class UserGroupServiceImpl extends ubic.gemma.model.common.auditAndSecurity.UserGroupServiceBase {
    private Log log = LogFactory.getLog( this.getClass() );

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.UserGroupServiceBase#handleCreate(java.lang.String,
     *      java.lang.String)
     */
    @Override
    protected Securable handleCreate( String name, String description ) throws Exception {

        // if user is logged in, then you can get the principal from the authentication object
        Object obj = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String ownerUserName = obj.toString();
        log.debug( ownerUserName );

        User user = this.getUserDao().findByUserName( ownerUserName );

        UserGroup userGroup = UserGroup.Factory.newInstance();
        userGroup.setName( name );
        userGroup.setDescription( description );

        Collection<User> groupMembers = new HashSet<User>();
        groupMembers.add( user );

        userGroup.setGroupMembers( groupMembers );

        return this.getUserGroupDao().create( userGroup );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.UserGroupServiceBase#handleUpdate(java.lang.String,
     *      ubic.gemma.model.common.auditAndSecurity.User)
     */
    @Override
    protected void handleUpdate( String groupName, User groupMember ) throws Exception {

        UserGroup userGroup = this.getUserGroupDao().findByUserGroupName( groupName );
        if ( userGroup == null ) {
            throw new RuntimeException( "Cannot update group " + groupName + ".  Group does not exist." );
        }

        Collection<User> groupMembers = userGroup.getGroupMembers();
        groupMembers.add( groupMember );

        this.getUserGroupDao().update( userGroup );
    }
}