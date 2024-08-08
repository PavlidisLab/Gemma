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
package ubic.gemma.core.security;

import gemma.gsec.AuthorityConstants;
import gemma.gsec.SecurityService;
import gemma.gsec.authentication.UserDetailsImpl;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.core.security.authentication.UserService;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.common.auditAndSecurity.UserGroup;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests the Group facilities of the UserManager..
 *
 * @author keshav, paul
 */
public class UserGroupServiceTest extends BaseSpringContextTest {

    private final String userName1 = "jonesey";
    private final String userName2 = "mark";
    @Autowired
    private UserManager userManager = null;
    @Autowired
    private UserService userService = null;
    @Autowired
    private SecurityService securityService = null;
    private String groupName = null;

    @Before
    public void setUp() throws Exception {
        this.groupName = RandomStringUtils.insecure().nextAlphabetic( 6 );

        /*
         * Create a user with default privileges.
         */
        if ( !userManager.userExists( userName1 ) )
            this.userManager.createUser(
                    new UserDetailsImpl( "foo", this.userName1, true, null, "foo@gmail.com", "key", new Date() ) );

        if ( !userManager.userExists( userName2 ) )
            this.userManager.createUser(
                    new UserDetailsImpl( "foo2", this.userName2, true, null, "foo2@gmail.com", "key2", new Date() ) );

    }

    /**
     * Tests creating a UserGroup
     */
    @Test
    public void testCreateUserGroup() {

        List<GrantedAuthority> authos = new ArrayList<>();
        authos.add( new SimpleGrantedAuthority( "GROUP_TESTING" ) );
        this.userManager.createGroup( this.groupName, authos );

        List<GrantedAuthority> findGroupAuthorities = this.userManager.findGroupAuthorities( this.groupName );

        for ( GrantedAuthority grantedAuthority : findGroupAuthorities ) {
            assertEquals( "GROUP_TESTING", grantedAuthority.getAuthority() );
        }

    }

    /**
     * Test for deleting a user group
     */
    @Test
    public void testDeleteUserGroup() {

        this.runAsAdmin();
        List<GrantedAuthority> authos = new ArrayList<>();
        authos.add( new SimpleGrantedAuthority( "GROUP_TESTING" ) );
        this.userManager.createGroup( this.groupName, authos );

        // add another user to group
        this.userManager.addUserToGroup( this.userName1, this.groupName );
        this.userManager.addUserToGroup( this.userName2, this.groupName );

        // grant read permission to group
        ExpressionExperiment ee = this.getTestPersistentExpressionExperiment();
        UserGroup group = this.userService.findGroupByName( this.groupName );

        this.securityService.makeOwnedByUser( ee, userName1 );
        this.securityService.makeOwnedByUser( group, userName1 );

        this.runAsUser( userName1 );
        this.securityService.makePrivate( ee );
        this.securityService.makeReadableByGroup( ee, this.groupName );

        // remove the group
        this.userManager.deleteGroup( this.groupName );

    }

    /**
     * Tests updating the UserGroup
     */
    @Test
    public void testUpdateUserGroup() {
        List<GrantedAuthority> authos = new ArrayList<>();
        authos.add( new SimpleGrantedAuthority( "GROUP_TESTING" ) );
        this.userManager.createGroup( this.groupName, authos );

        List<GrantedAuthority> findGroupAuthorities = this.userManager.findGroupAuthorities( this.groupName );

        for ( GrantedAuthority grantedAuthority : findGroupAuthorities ) {
            assertEquals( "GROUP_TESTING", grantedAuthority.getAuthority() );
        }

        /*
         * Add a user to the group
         */

        this.userManager.addUserToGroup( this.userName1, this.groupName );

        List<String> users = this.userManager.findUsersInGroup( this.groupName );
        assertTrue( users.contains( this.userName1 ) );

        /*
         * Make sure user can see group (from bug 2822)
         */
        UserGroup group = this.userService.findGroupByName( this.groupName );
        this.securityService.isViewableByUser( group, this.userName1 );

        /*
         * Remove a user from the group.
         */
        this.userManager.removeUserFromGroup( this.userName1, this.groupName );
        users = this.userManager.findUsersInGroup( this.groupName );
        assertTrue( !users.contains( this.userName1 ) );

        super.runAsUser( this.userName1 );

        /*
         * Can the user remove themselves from the group?
         */
        try {
            this.userManager.removeUserFromGroup( this.userName1, this.groupName );
            fail( "Should have gotten access denied when user tried to remove themselves from a group" );
        } catch ( AccessDeniedException ok ) {
            // expected behaviour
        }

        /*
         * Can they elevate the group authority?
         */
        try {
            this.userManager.addGroupAuthority( this.groupName,
                    new SimpleGrantedAuthority( AuthorityConstants.ADMIN_GROUP_AUTHORITY ) );
            fail( "Should have gotten access denied when user tried to make group ADMIN" );
        } catch ( AccessDeniedException ok ) {
            // expected behaviour
        }

    }

    @Test
    public void testUserAddSelvesToAdmin() {
        super.runAsUser( this.userName1 );

        try {
            this.userManager.addUserToGroup( this.userName1, "Administrators" );
            fail( "Should have gotten access denied when user tried to make themselves admin" );
        } catch ( AccessDeniedException ok ) {
            // expected behaviour
        }
    }
}
