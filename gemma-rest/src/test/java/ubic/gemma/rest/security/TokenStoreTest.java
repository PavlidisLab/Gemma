/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.rest.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure-JVM unit tests for {@link TokenStore}. No Caffeine internals are stubbed —
 * the store is exercised end-to-end against its real Caffeine cache. Sliding TTL is
 * NOT time-warped here (would require a fake ticker); we only assert the
 * issue/lookup/revoke contract.
 */
class TokenStoreTest {

    private TokenStore store;
    private Authentication alice;

    @BeforeEach
    void setUp() {
        store = new TokenStore();
        alice = new UsernamePasswordAuthenticationToken( "alice", "n/a",
                Collections.singletonList( new SimpleGrantedAuthority( "ROLE_USER" ) ) );
    }

    @Test
    void issue_returnsOpaqueTokenResolvableByLookup() {
        String token = store.issue( alice );

        assertThat( token ).isNotBlank();
        assertThat( store.lookup( token ) ).isSameAs( alice );
    }

    @Test
    void issue_mintsDistinctTokensPerCall() {
        String first = store.issue( alice );
        String second = store.issue( alice );

        assertThat( first ).isNotEqualTo( second );
        assertThat( store.lookup( first ) ).isSameAs( alice );
        assertThat( store.lookup( second ) ).isSameAs( alice );
    }

    @Test
    void lookup_unknownToken_returnsNull() {
        assertThat( store.lookup( "no-such-token" ) ).isNull();
    }

    @Test
    void revoke_invalidatesIssuedToken() {
        String token = store.issue( alice );
        assertThat( store.lookup( token ) ).isSameAs( alice );

        store.revoke( token );

        assertThat( store.lookup( token ) ).isNull();
    }

    @Test
    void revoke_unknownToken_isNoOp() {
        // Idempotent: revoking an unknown / already-revoked token must not throw.
        store.revoke( "never-issued" );
        store.revoke( "never-issued" );
    }

    @Test
    void size_reflectsLiveIssuedTokens() {
        assertThat( store.size() ).isZero();

        String token = store.issue( alice );
        assertThat( store.size() ).isEqualTo( 1L );

        store.revoke( token );
        assertThat( store.size() ).isZero();
    }
}
