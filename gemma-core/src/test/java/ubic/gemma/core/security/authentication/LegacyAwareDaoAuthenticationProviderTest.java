/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
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
 */
package ubic.gemma.core.security.authentication;

import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Verifies {@link LegacyAwareDaoAuthenticationProvider} can authenticate legacy
 * SHA-1+username-salt hashes without any ThreadLocal state, and triggers the Spring Security
 * 6 password-upgrade hook ({@link UserDetailsPasswordService}) on success.
 */
public class LegacyAwareDaoAuthenticationProviderTest {

    private LegacyAwareDaoAuthenticationProvider provider;
    private GemmaLegacyAwarePasswordEncoder encoder;
    private AtomicReference<String> upgradedHash;
    private UserDetails storedUser;

    @Before
    public void setUp() {
        encoder = new GemmaLegacyAwarePasswordEncoder();
        upgradedHash = new AtomicReference<>();

        // Legacy hash from init-data.sql for username='administrator', password='administrator'.
        String legacyHash = "b7338dcc17d6b6c199a75540aab6d0506567b980";
        storedUser = User.withUsername( "administrator" )
                .password( legacyHash )
                .authorities( AuthorityUtils.createAuthorityList( "GROUP_ADMIN" ) )
                .build();

        UserDetailsService uds = username -> {
            if ( "administrator".equals( username ) ) {
                return storedUser;
            }
            throw new UsernameNotFoundException( username );
        };

        UserDetailsPasswordService pwSvc = ( user, newPassword ) -> {
            upgradedHash.set( newPassword );
            return User.withUsername( user.getUsername() )
                    .password( newPassword )
                    .authorities( user.getAuthorities() )
                    .build();
        };

        provider = new LegacyAwareDaoAuthenticationProvider();
        provider.setUserDetailsService( uds );
        provider.setPasswordEncoder( encoder );
        provider.setUserDetailsPasswordService( pwSvc );
    }

    @Test
    public void legacyHash_successfulLogin_triggersBcryptUpgrade() {
        Authentication request = new UsernamePasswordAuthenticationToken( "administrator", "administrator" );

        Authentication result = provider.authenticate( request );

        assertNotNull( "auth must succeed against the legacy fixture hash", result );
        assertTrue( result.isAuthenticated() );
        assertEquals( "administrator", result.getName() );

        // The framework must have invoked the password-upgrade hook with a bcrypt-prefixed
        // new hash — no ThreadLocal anywhere in this code path.
        String upgraded = upgradedHash.get();
        assertNotNull( "UserDetailsPasswordService.updatePassword must be called for legacy logins",
                upgraded );
        assertTrue( "upgraded hash must be bcrypt-prefixed",
                upgraded.startsWith( GemmaLegacyAwarePasswordEncoder.BCRYPT_PREFIX ) );
        assertTrue( "upgraded hash must verify the same raw password",
                encoder.matches( "administrator", upgraded ) );
    }

    @Test
    public void legacyHash_wrongPassword_rejected() {
        Authentication request = new UsernamePasswordAuthenticationToken( "administrator", "not-the-password" );
        try {
            provider.authenticate( request );
            fail( "expected BadCredentialsException for wrong legacy password" );
        } catch ( BadCredentialsException expected ) {
            // ok
        }
    }

    @Test
    public void bcryptHash_stillAuthenticatesViaPasswordEncoder() {
        // Simulate a user who has already been upgraded: stored hash is bcrypt.
        String bcryptStored = encoder.encode( "secret" );
        UserDetails upgradedStoredUser = User.withUsername( "alice" )
                .password( bcryptStored )
                .authorities( AuthorityUtils.createAuthorityList( "GROUP_USER" ) )
                .build();
        provider.setUserDetailsService( username -> {
            if ( "alice".equals( username ) ) {
                return upgradedStoredUser;
            }
            throw new UsernameNotFoundException( username );
        } );

        Authentication request = new UsernamePasswordAuthenticationToken( "alice", "secret" );
        Authentication result = provider.authenticate( request );
        assertTrue( result.isAuthenticated() );

        // bcrypt hash does NOT trigger upgrade.
        // (upgradedHash is shared between tests via setUp; here we just confirm no further
        // write happened on this path beyond what bcrypt stored.)
        // upgradeEncoding(bcrypt) returns false, so updatePassword should NOT fire.
        assertEquals( "bcrypt logins must not trigger password upgrade",
                null, upgradedHash.get() );
    }
}
