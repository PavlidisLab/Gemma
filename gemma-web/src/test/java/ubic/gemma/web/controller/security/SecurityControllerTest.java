/*
 * The Gemma project
 * 
 * Copyright (c) 2009 University of British Columbia
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
package ubic.gemma.web.controller.security;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.security.SecurityService;
import ubic.gemma.security.authentication.UserDetailsImpl;
import ubic.gemma.security.authentication.UserManager;
import ubic.gemma.testing.BaseSpringWebTest;
import ubic.gemma.web.controller.common.auditAndSecurity.SecurityController;
import ubic.gemma.web.controller.common.auditAndSecurity.SecurityInfoValueObject;
import ubic.gemma.web.remote.EntityDelegator;

/**
 * @author paul
 * @version $Id$
 */
public class SecurityControllerTest extends BaseSpringWebTest {

    private EntityDelegator ed;

    private ExpressionExperiment ee;

    private Long eeId;

    @Autowired
    private SecurityController securityController;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private UserManager userManager;

    private String userName;

    @Before
    public void setup() {
        this.ee = super.getTestPersistentBasicExpressionExperiment();
        this.eeId = ee.getId();
        this.userName = randomName();
        makeUser( userName );
        ed = new EntityDelegator();
        ed.setClassDelegatingFor( ee.getClass().getName() );
        ed.setId( this.eeId );
        securityService.makeOwnedByUser( ee, userName );
    }

    @Test
    public void testGetInfo() {
        SecurityInfoValueObject securityInfo = securityController.getSecurityInfo( ed );
        assertNotNull( securityInfo );
        assertTrue( securityInfo.isPubliclyReadable() );
    }

    @Test
    public void testRemoveUsersFromGroupDisallowed() {
        String[] userNames = { "administrator" };
        try {
            securityController.removeUsersFromGroup( userNames, "Administrators" );

            /*
             * Fix it.
             */
            securityController.addUserToGroup( "administrator", "Administrators" );

            fail( "Should have gotten an exception" );
        } catch ( Exception e ) {
            // / ok
        }
    }

    @Test
    public void testUpdatePermissions() {
        String groupName = randomName();
        runAsUser( this.userName );
        securityService.createGroup( groupName );
        SecurityInfoValueObject securityInfo = securityController.getSecurityInfo( ed );
        securityInfo.getGroupsThatCanRead().add( groupName );

        SecurityInfoValueObject[] os = { securityInfo };

        securityController.updatePermissions( os );

        assertTrue( securityService.isReadableByGroup( ee, groupName ) );
    }

    private void makeUser( String username ) {
        try {
            userManager.loadUserByUsername( username );
        } catch ( UsernameNotFoundException e ) {
            userManager.createUser( new UserDetailsImpl( "foo", username, true, null, RandomStringUtils
                    .randomAlphabetic( 10 ) + "@gmail.com", "key", new Date() ) );
        }
    }
}
