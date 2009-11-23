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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils; 
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import ubic.gemma.security.authentication.UserDetailsImpl;
import ubic.gemma.security.authentication.UserManager;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * Tests the Group facilities of the UserManager..
 * 
 * @author keshav, paul
 * @version $Id$
 */
public class UserGroupServiceTest extends BaseSpringContextTest {

    @Autowired
    private UserManager userManager = null;

    private String groupName = null;

    private String aDifferentUsername = "jonesey";

    @Before
    public void setup() throws Exception {
        groupName = RandomStringUtils.randomAlphabetic( 6 );

        /*
         * Create a user with default privileges.
         */
        try {
            userManager.loadUserByUsername( aDifferentUsername );
        } catch ( UsernameNotFoundException e ) {
            userManager.createUser( new UserDetailsImpl( "foo", aDifferentUsername, true, null, "foo@gmail.com", "key",
                    new Date() ) );
        }
    }

    /**
     * Tests creating a UserGroup
     */
    @Test
    public void testCreateUserGroup() {

        List<GrantedAuthority> authos = new ArrayList<GrantedAuthority>();
        authos.add( new GrantedAuthorityImpl( "GROUP_TESTING" ) );
        userManager.createGroup( groupName, authos );

        List<GrantedAuthority> findGroupAuthorities = userManager.findGroupAuthorities( groupName );

        for ( GrantedAuthority grantedAuthority : findGroupAuthorities ) {
            assertEquals( "GROUP_TESTING", grantedAuthority.getAuthority() );
        }

    }

    @Test
    public void testUserAddSelvesToAdmin() {
        super.runAsUser( aDifferentUsername );

        try {
            userManager.addUserToGroup( aDifferentUsername, "Administrators" );
            fail( "Should have gotten access denied when user tried to make themselves admin" );
        } catch ( AccessDeniedException ok ) {

        }
    }

    /**
     * Tests updating the UserGroup
     */
    @Test
    public void testUpdateUserGroup() {
        List<GrantedAuthority> authos = new ArrayList<GrantedAuthority>();
        authos.add( new GrantedAuthorityImpl( "GROUP_TESTING" ) );
        userManager.createGroup( groupName, authos );

        List<GrantedAuthority> findGroupAuthorities = userManager.findGroupAuthorities( groupName );

        for ( GrantedAuthority grantedAuthority : findGroupAuthorities ) {
            assertEquals( "GROUP_TESTING", grantedAuthority.getAuthority() );
        }

        /*
         * Add a user to the group
         */

        userManager.addUserToGroup( aDifferentUsername, groupName );

        List<String> users = userManager.findUsersInGroup( groupName );
        assertTrue( users.contains( aDifferentUsername ) );

        /*
         * Remove a user from the group.
         */
        userManager.removeUserFromGroup( aDifferentUsername, groupName );
        users = userManager.findUsersInGroup( groupName );
        assertTrue( !users.contains( aDifferentUsername ) );

        super.runAsUser( aDifferentUsername );

        /*
         * Can the user remove themselves from the group?
         */
        try {
            userManager.removeUserFromGroup( aDifferentUsername, groupName );
            fail( "Should have gotten access denied when user tried to remove themselves from a group" );
        } catch ( AccessDeniedException ok ) {

        }

        /*
         * Can they elevate the group authority?
         */
        try {
            userManager.addGroupAuthority( groupName, new GrantedAuthorityImpl( "GROUP_ADMIN" ) );
            fail( "Should have gotten access denied when user tried to make group ADMIN" );
        } catch ( AccessDeniedException ok ) {

        }

    }
}
