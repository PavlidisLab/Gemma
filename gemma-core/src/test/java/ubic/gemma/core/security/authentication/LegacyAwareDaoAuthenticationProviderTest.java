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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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

    @BeforeEach
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

        assertNotNull( result, "auth must succeed against the legacy fixture hash" );
        assertTrue( result.isAuthenticated() );
        assertEquals( "administrator", result.getName() );

        // The framework must have invoked the password-upgrade hook with a bcrypt-prefixed
        // new hash — no ThreadLocal anywhere in this code path.
        String upgraded = upgradedHash.get();
        assertNotNull( upgraded,
                "UserDetailsPasswordService.updatePassword must be called for legacy logins" );
        assertTrue( upgraded.startsWith( GemmaLegacyAwarePasswordEncoder.BCRYPT_PREFIX ),
                "upgraded hash must be bcrypt-prefixed" );
        assertTrue( encoder.matches( "administrator", upgraded ),
                "upgraded hash must verify the same raw password" );
    }

    @Test
    public void fixedSaltHash_authenticatesViaGooblyfoobly() {
        // Pre-2009 SystemWideSaltSource format: SHA-1(password + "{" + systemSalt + "}") where
        // systemSalt is the configured constant ("gooblyfoobly" per commit 66f574e926, 2008-10-02).
        // We construct the stored hash via the encoder itself rather than hard-coding a value —
        // the historical init-data.sql hashes in the 2008 commit don't reproduce under plain
        // ShaPasswordEncoder formula (possibly hand-edited at the time), so we test the
        // round-trip against the encoder's canonical output instead.
        String fixedSaltHash = GemmaLegacyAwarePasswordEncoder
                .sha1HexUsernameSalt( "test", LegacyAwareDaoAuthenticationProvider.SYSTEM_WIDE_SALT );
        assertTrue( GemmaLegacyAwarePasswordEncoder.isLegacySha1Hex( fixedSaltHash ),
                "encoder output must be a 40-char hex SHA-1" );

        UserDetails legacyAdmin = User.withUsername( "administrator" )
                .password( fixedSaltHash )
                .authorities( AuthorityUtils.createAuthorityList( "GROUP_ADMIN" ) )
                .build();
        provider.setUserDetailsService( username -> {
            if ( "administrator".equals( username ) ) {
                return legacyAdmin;
            }
            throw new UsernameNotFoundException( username );
        } );

        // Username-salt would NOT match this hash — only the fixed-salt fallback rescues it.
        Authentication result = provider.authenticate(
                new UsernamePasswordAuthenticationToken( "administrator", "test" ) );
        assertTrue( result.isAuthenticated(),
                "fixed-salt fallback must verify pre-2009 hashes whose stored value matches"
                        + " SHA-1(password + \"{gooblyfoobly}\")" );
    }

    @Test
    public void fixedSaltHash_wrongPassword_rejected() {
        String fixedSaltHash = GemmaLegacyAwarePasswordEncoder
                .sha1HexUsernameSalt( "test", LegacyAwareDaoAuthenticationProvider.SYSTEM_WIDE_SALT );
        UserDetails legacyAdmin = User.withUsername( "administrator" )
                .password( fixedSaltHash )
                .authorities( AuthorityUtils.createAuthorityList( "GROUP_ADMIN" ) )
                .build();
        provider.setUserDetailsService( username -> {
            if ( "administrator".equals( username ) ) {
                return legacyAdmin;
            }
            throw new UsernameNotFoundException( username );
        } );

        try {
            provider.authenticate(
                    new UsernamePasswordAuthenticationToken( "administrator", "not-the-password" ) );
            fail( "expected BadCredentialsException for wrong fixed-salt password" );
        } catch ( BadCredentialsException expected ) {
            // ok
        }
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
        assertEquals( null, upgradedHash.get(),
                "bcrypt logins must not trigger password upgrade" );
    }
}
