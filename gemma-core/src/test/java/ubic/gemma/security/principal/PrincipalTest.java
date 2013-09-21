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
package ubic.gemma.security.principal;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.core.Authentication;

import ubic.gemma.security.authentication.UserDetailsImpl;
import ubic.gemma.security.authentication.UserManager;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * Test that we can log users in
 * 
 * @author pavlidis
 * @version $Id$
 */
public class PrincipalTest extends BaseSpringContextTest {

    String pwd;

    String username;

    @Autowired
    UserManager userManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Before
    public void before() {

        pwd = randomName();
        username = randomName();

        if ( !userManager.userExists( username ) ) {

            String encodedPassword = passwordEncoder.encodePassword( pwd, username );
            UserDetailsImpl u = new UserDetailsImpl( encodedPassword, username, true, null, null, null, new Date() );
            userManager.createUser( u );
        }
    }

    /**
     * @throws Exception
     */
    @Test
    public final void testLogin() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken( username, pwd );

        Authentication authentication = ( ( ProviderManager ) authenticationManager ).authenticate( auth );
        assertTrue( authentication.isAuthenticated() );
    }

    @Test
    public final void testLoginNonexistentUser() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken( "bad user", "wrong password" );

        try {
            ( ( ProviderManager ) authenticationManager ).authenticate( auth );
            fail( "Should have gotten a bad credentials exception" );
        } catch ( BadCredentialsException e ) {
            //
        }
    }

    @Test
    public final void testLoginWrongPassword() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken( username, "wrong password" );

        try {
            ( ( ProviderManager ) authenticationManager ).authenticate( auth );
            fail( "Should have gotten a bad credentials exception" );
        } catch ( BadCredentialsException e ) {
            //
        }
    }

}
