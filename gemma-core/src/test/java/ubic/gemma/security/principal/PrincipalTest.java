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

import org.springframework.security.Authentication;
import org.springframework.security.BadCredentialsException;
import org.springframework.security.providers.ProviderManager;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;

import ubic.gemma.testing.BaseSpringContextTest;
import ubic.gemma.util.ConfigUtils;

/**
 * Test that we can log users in
 * 
 * @author pavlidis
 * @version $Id$
 */
public class PrincipalTest extends BaseSpringContextTest {

    /**
     * @throws Exception
     */
    public final void testLogin() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken( ConfigUtils.getString( "gemma.admin.user" ),
                ConfigUtils.getString( "gemma.admin.password" ) );

        ProviderManager providerManager = ( ProviderManager ) this.getBean( "authenticationManager" );
        Authentication authentication = providerManager.doAuthentication( auth );
        assertTrue( authentication.isAuthenticated() );
    }

    public final void testLoginWrongPassword() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken( ConfigUtils.getString( "gemma.admin.user" ),
                "wrong password" );

        ProviderManager providerManager = ( ProviderManager ) this.getBean( "authenticationManager" );
        try {
            providerManager.doAuthentication( auth );
            fail( "Should have gotten a bad credentials exception" );
        } catch ( BadCredentialsException e ) {
            //
        }
    }

    public final void testLoginNonexistentUser() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken( "bad user", "wrong password" );

        ProviderManager providerManager = ( ProviderManager ) this.getBean( "authenticationManager" );

        try {
            providerManager.doAuthentication( auth );
            fail( "Should have gotten a bad credentials exception" );
        } catch ( BadCredentialsException e ) {
            //
        }
    }

}
