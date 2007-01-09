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

import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserGroupService;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author keshav
 * @version $Id$
 */
public class UserGroupServiceTest extends BaseSpringContextTest {

    private Log log = LogFactory.getLog( this.getClass() );

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.testing.BaseSpringContextTest#onSetUpInTransaction()
     */
    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

    }

    /*
     * 
     * 
     */
    public void testCreateUserGroup() {

        /* owner of the group */
        User owner = this.getTestPersistentUser();
        log.info( "user is " + owner.getUserName() );

        UserGroupService userGroupService = ( UserGroupService ) this.getBean( "userGroupService" );

        userGroupService.create( owner, RandomStringUtils.randomAlphabetic( 6 ), "A test group created from "
                + this.getClass().getName() );
    }

    /**
     * 
     *
     */
    public void testUpdateUserGroup() {

        User owner = this.getTestPersistentUser();
        log.info( "User is " + owner.getUserName() );

        UserGroupService userGroupService = ( UserGroupService ) this.getBean( "userGroupService" );
    }

}
