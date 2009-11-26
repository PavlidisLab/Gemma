/*
 * The Gemma project
 * 
 * Copyright (c) 2006-2010 University of British Columbia
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

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.TestingAuthenticationProvider;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import ubic.gemma.security.authentication.UserManager;

/**
 * Convenience methods used by multiple test abstractions. It is important that this not be used for anything other than tests!
 * 
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.security.authentication.AuthenticationUtils
 */
@Component
public class AuthenticationTestingUtil {

    @Autowired
    UserManager userManager;

    /**
     * Grant authority to a test user, with admin privileges, and put the token in the context. This means your tests
     * will be authorized to do anything an administrator would be able to do.
     */
    public void grantAdminAuthority( ApplicationContext ctx ) {
        ProviderManager providerManager = ( ProviderManager ) ctx.getBean( "authenticationManager" );
        providerManager.getProviders().add( new TestingAuthenticationProvider() );

        // Grant all roles to test user.
        TestingAuthenticationToken token = new TestingAuthenticationToken( "administrator", "administrator",
                new GrantedAuthority[] { new GrantedAuthorityImpl( "GROUP_ADMIN" ) } );

        token.setAuthenticated( true );

        putTokenInContext( token );
    }

    /**
     * Grant authority to a test user, with regular user privileges, and put the token in the context. This means your
     * tests will be authorized to do anything that user could do
     */
    public void switchToUser( ApplicationContext ctx, String username ) {

        UserDetails user = userManager.loadUserByUsername( username );

        List<GrantedAuthority> authrs = new ArrayList<GrantedAuthority>( user.getAuthorities() );

        ProviderManager providerManager = ( ProviderManager ) ctx.getBean( "authenticationManager" );
        providerManager.getProviders().add( new TestingAuthenticationProvider() );

        TestingAuthenticationToken token = new TestingAuthenticationToken( username, "testing", authrs );
        token.setAuthenticated( true );

        putTokenInContext( token );
    }

    /**
     * @param token
     */
    private static void putTokenInContext( AbstractAuthenticationToken token ) {
        SecurityContextHolder.getContext().setAuthentication( token );
    }
}
