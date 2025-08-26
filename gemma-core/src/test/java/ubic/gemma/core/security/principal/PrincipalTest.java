/*
 * The Gemma project
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
package ubic.gemma.core.security.principal;

import gemma.gsec.authentication.UserDetailsImpl;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.core.Authentication;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.core.util.test.BaseSpringContextTest;

import java.util.Date;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test that we can log users in, etc.
 *
 * @author pavlidis
 */
public class PrincipalTest extends BaseSpringContextTest {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String pwd;

    @Autowired
    private UserManager userManager;

    private String username;

    private String email = "foo@foo.foo";

    @Before
    public void before() {

        pwd = this.randomName();
        username = this.randomName();
        email = username + "@foo.foo";
        if ( !userManager.userExists( username ) ) {

            String encodedPassword = passwordEncoder.encodePassword( pwd, username );
            UserDetailsImpl u = new UserDetailsImpl( encodedPassword, username, true, null, email, null, new Date() );
            userManager.createUser( u );
        }
    }

    @Test
    public final void testChangePassword() {
        String newpwd = this.randomName();

        String token = userManager.changePasswordForUser( email, username, newpwd );
        System.out.println( "passwd changed" );

        assertTrue( !userManager.loadUserByUsername( username ).isEnabled() );

        /*
         * User has to unlock the account, we mimic that:
         */
        assertTrue( userManager.validateSignupToken( username, token ) );
        System.out.println( "token validated" );

        Authentication auth = new UsernamePasswordAuthenticationToken( username, newpwd );
        Authentication authentication = ( authenticationManager ).authenticate( auth );
        assertTrue( authentication.isAuthenticated() );
    }

    @Test
    public final void testLogin() {
        Authentication auth = new UsernamePasswordAuthenticationToken( username, pwd );

        Authentication authentication = ( authenticationManager ).authenticate( auth );
        assertTrue( authentication.isAuthenticated() );
    }

    @Test
    public final void testLoginNonexistentUser() {
        Authentication auth = new UsernamePasswordAuthenticationToken( "bad user", "wrong password" );

        try {
            ( authenticationManager ).authenticate( auth );
            fail( "Should have gotten a bad credentials exception" );
        } catch ( BadCredentialsException e ) {
            //
        }
    }

    @Test
    public final void testLoginWrongPassword() {
        Authentication auth = new UsernamePasswordAuthenticationToken( username, "wrong password" );

        try {
            ( authenticationManager ).authenticate( auth );
            fail( "Should have gotten a bad credentials exception" );
        } catch ( BadCredentialsException e ) {
            //
        }
    }

}
