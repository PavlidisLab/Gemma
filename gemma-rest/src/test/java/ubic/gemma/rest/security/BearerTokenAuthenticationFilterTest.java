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

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pure-Mockito unit tests for {@link BearerTokenAuthenticationFilter}.
 * <p>
 * Verifies header parsing branches and SecurityContext-population semantics without booting a
 * Spring context. Every {@code @Test} stays sub-second.
 */
@ExtendWith(MockitoExtension.class)
class BearerTokenAuthenticationFilterTest {

    @Mock
    private TokenStore tokenStore;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    private BearerTokenAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new BearerTokenAuthenticationFilter( tokenStore );
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noAuthorizationHeader_passesThroughAndLeavesContextUntouched() throws Exception {
        when( request.getHeader( "Authorization" ) ).thenReturn( null );

        filter.doFilter( request, response, chain );

        assertThat( SecurityContextHolder.getContext().getAuthentication() ).isNull();
        verify( chain, times( 1 ) ).doFilter( request, response );
        verifyNoInteractions( tokenStore );
    }

    @Test
    void wrongScheme_basicAuth_passesThrough() throws Exception {
        when( request.getHeader( "Authorization" ) ).thenReturn( "Basic dXNlcjpwYXNz" );

        filter.doFilter( request, response, chain );

        assertThat( SecurityContextHolder.getContext().getAuthentication() ).isNull();
        verify( chain, times( 1 ) ).doFilter( request, response );
        verifyNoInteractions( tokenStore );
    }

    @Test
    void bearerWithKnownToken_populatesSecurityContext() throws Exception {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "alice", "n/a", Collections.singletonList( new SimpleGrantedAuthority( "ROLE_USER" ) ) );
        when( request.getHeader( "Authorization" ) ).thenReturn( "Bearer my-opaque-token" );
        when( tokenStore.lookup( "my-opaque-token" ) ).thenReturn( authentication );

        filter.doFilter( request, response, chain );

        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        assertThat( current ).isSameAs( authentication );
        assertThat( current.getName() ).isEqualTo( "alice" );
        verify( chain, times( 1 ) ).doFilter( request, response );
    }

    @Test
    void bearerWithUnknownToken_passesThroughWithoutPopulatingContext() throws Exception {
        when( request.getHeader( "Authorization" ) ).thenReturn( "Bearer stale-token" );
        when( tokenStore.lookup( "stale-token" ) ).thenReturn( null );

        filter.doFilter( request, response, chain );

        assertThat( SecurityContextHolder.getContext().getAuthentication() ).isNull();
        verify( chain, times( 1 ) ).doFilter( request, response );
    }

    @Test
    void emptyBearerValue_passesThroughWithoutLookup() throws Exception {
        when( request.getHeader( "Authorization" ) ).thenReturn( "Bearer " );

        filter.doFilter( request, response, chain );

        assertThat( SecurityContextHolder.getContext().getAuthentication() ).isNull();
        verify( chain, times( 1 ) ).doFilter( request, response );
        verify( tokenStore, never() ).lookup( org.mockito.ArgumentMatchers.anyString() );
    }

    @Test
    void bearerSchemeCaseInsensitive_resolvesToken() throws Exception {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "bob", "n/a", Collections.singletonList( new SimpleGrantedAuthority( "ROLE_USER" ) ) );
        when( request.getHeader( "Authorization" ) ).thenReturn( "bearer my-token" );
        when( tokenStore.lookup( "my-token" ) ).thenReturn( authentication );

        filter.doFilter( request, response, chain );

        assertThat( SecurityContextHolder.getContext().getAuthentication() ).isSameAs( authentication );
        verify( chain, times( 1 ) ).doFilter( request, response );
    }

    @Test
    void extractBearerToken_handlesNullAndMalformedInputs() {
        assertThat( BearerTokenAuthenticationFilter.extractBearerToken( null ) ).isNull();
        assertThat( BearerTokenAuthenticationFilter.extractBearerToken( "" ) ).isNull();
        assertThat( BearerTokenAuthenticationFilter.extractBearerToken( "Basic xyz" ) ).isNull();
        assertThat( BearerTokenAuthenticationFilter.extractBearerToken( "Bearer " ) ).isNull();
        assertThat( BearerTokenAuthenticationFilter.extractBearerToken( "Bearer   " ) ).isNull();
        assertThat( BearerTokenAuthenticationFilter.extractBearerToken( "Bearer abc" ) ).isEqualTo( "abc" );
        assertThat( BearerTokenAuthenticationFilter.extractBearerToken( "Bearer  spaced  " ) ).isEqualTo( "spaced" );
    }
}
