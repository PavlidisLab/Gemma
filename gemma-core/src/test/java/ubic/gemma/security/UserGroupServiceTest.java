/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.security;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.common.Securable;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserGroupService;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * Tests the UserGroupService.
 * 
 * @author keshav
 * @version $Id$
 */
public class UserGroupServiceTest extends BaseSpringContextTest {

    private Log log = LogFactory.getLog( this.getClass() );

    private UserGroupService userGroupService = null;

    private String groupName = null;
    private String description = null;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.testing.BaseSpringContextTest#onSetUpInTransaction()
     */
    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransactionGrantingUserAuthority( "test" );

        groupName = RandomStringUtils.randomAlphabetic( 6 );

        description = "A test group created from " + this.getClass().getName();

        userGroupService = ( UserGroupService ) this.getBean( "userGroupService" );

    }

    /**
     * Tests creating a UserGroup
     */
    public void testCreateUserGroup() {
        Securable persistentUserGroup = userGroupService.create( groupName, description );
        assertNotNull( persistentUserGroup );
    }

    /**
     * Tests updating the UserGroup
     */
    public void testUpdateUserGroup() {
        log.debug( "updating user group" );

        userGroupService.create( groupName, description );

        User groupMember = User.Factory.newInstance();
        String username = RandomStringUtils.randomAlphabetic( 6 );
        groupMember.setName( username );
        groupMember.setDescription( "A new user " + username + " to add to group " + groupName );

        userGroupService.update( groupName, groupMember );

        /* to see changes in the database, uncommment setComplete(); */
        // setComplete();
    }
}
