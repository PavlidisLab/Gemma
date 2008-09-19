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
package ubic.gemma.testing;

import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.AbstractAuthenticationToken;
import org.springframework.security.providers.ProviderManager;
import org.springframework.security.providers.TestingAuthenticationProvider;
import org.springframework.security.providers.TestingAuthenticationToken;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Convenience methods used by multiple test abstractions
 * 
 * @author pavlidis
 * @version $Id$
 */
public class SpringTestUtil {

    /**
     * Grant authority to a test user, with admin privileges, and put the token in the context. This means your tests
     * will be authorized to do anything an administrator would be able to do.
     */
    @SuppressWarnings("unchecked")
    public static void grantAuthority( ConfigurableApplicationContext ctx ) {
        ProviderManager providerManager = ( ProviderManager ) ctx.getBean( "authenticationManager" );
        providerManager.getProviders().add( new TestingAuthenticationProvider() );

        // Grant all roles to test user.
        TestingAuthenticationToken token = new TestingAuthenticationToken( "administrator", "testing",
                new GrantedAuthority[] { new GrantedAuthorityImpl( "user" ), new GrantedAuthorityImpl( "admin" ) } );

        token.setAuthenticated( true );

        putTokenInContext( token );
    }

    /**
     * Grant authority to a test user, with regular user privileges, and put the token in the context. This means your
     * tests will be authorized to do anything a user would be able to do, but lacks administrative privileges.
     */
    @SuppressWarnings("unchecked")
    public static void grantUserAuthority( ConfigurableApplicationContext ctx, String username ) {
        ProviderManager providerManager = ( ProviderManager ) ctx.getBean( "authenticationManager" );
        providerManager.getProviders().add( new TestingAuthenticationProvider() );

        // Grant all roles to test user.
        TestingAuthenticationToken token = new TestingAuthenticationToken( username, "testing",
                new GrantedAuthority[] { new GrantedAuthorityImpl( "user" ) } );

        token.setAuthenticated( true );

        putTokenInContext( token );
    }

    /**
     * @param token
     */
    private static void putTokenInContext( AbstractAuthenticationToken token ) {
        // Create and store the Acegi SecureContext into the ContextHolder.
        SecurityContext secureContext = SecurityContextHolder.getContext();
        secureContext.setAuthentication( token );
        SecurityContextHolder.setContext( secureContext );
    }
}
