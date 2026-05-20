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
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Spring Security filter that resolves an {@code Authorization: Bearer <opaque>} header
 * to a {@link Authentication} previously issued by {@link AuthWebService#login} and stored
 * in {@link TokenStore}.
 *
 * <p>If the header is present and the token is valid, the corresponding
 * {@link Authentication} is placed on the {@link SecurityContextHolder} for the remainder
 * of the request. If the header is absent or the token is unknown / expired, the filter
 * is a no-op: the chain continues and the standard
 * {@link org.springframework.security.web.authentication.www.BasicAuthenticationFilter}
 * (registered immediately after this one in {@link RestSecurityConfig}) gets a chance to
 * authenticate via HTTP Basic. This preserves the legacy CLI / script / RClient code paths
 * that still rely on Basic.
 *
 * <p>Order: registered via {@code .addFilterBefore(..., BasicAuthenticationFilter.class)}
 * in {@link RestSecurityConfig}.
 *
 * @see TokenStore
 * @see AuthWebService
 */
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger( BearerTokenAuthenticationFilter.class );

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenStore tokenStore;

    public BearerTokenAuthenticationFilter( TokenStore tokenStore ) {
        this.tokenStore = tokenStore;
    }

    /**
     * Extract the bare opaque token from an {@code Authorization} header value, or
     * {@code null} if the header is missing / not a Bearer scheme / has an empty token.
     *
     * <p>Public so {@link ubic.gemma.rest.AuthWebService#logout} can use the same parsing
     * rule the filter uses (so a logout call with a malformed header still 200s rather
     * than NPE'ing on substring math).
     */
    public static String extractBearerToken( String headerValue ) {
        if ( headerValue == null || !headerValue.regionMatches( true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length() ) ) {
            return null;
        }
        String token = headerValue.substring( BEARER_PREFIX.length() ).trim();
        return token.isEmpty() ? null : token;
    }

    @Override
    protected void doFilterInternal( HttpServletRequest request, HttpServletResponse response, FilterChain filterChain ) throws ServletException, IOException {
        // If somebody already authenticated us (chain ordering accident, or test scaffolding),
        // don't clobber that context.
        if ( SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()
                && !isAnonymous( SecurityContextHolder.getContext().getAuthentication() ) ) {
            filterChain.doFilter( request, response );
            return;
        }

        String token = extractBearerToken( request.getHeader( AUTHORIZATION_HEADER ) );
        if ( token != null ) {
            Authentication authentication = tokenStore.lookup( token );
            if ( authentication != null ) {
                SecurityContext ctx = SecurityContextHolder.createEmptyContext();
                ctx.setAuthentication( authentication );
                SecurityContextHolder.setContext( ctx );
                if ( log.isDebugEnabled() ) {
                    log.debug( "Bearer token resolved for principal: {}", authentication.getName() );
                }
            } else {
                // Unknown / expired token: leave the context untouched and let downstream
                // filters fall through. The chain's anonymous provider will eventually run;
                // any /rest/v2/users/** request will then 401 via RestAuthEntryPoint, which is
                // the correct UX (curator sees "session expired, please log in").
                if ( log.isDebugEnabled() ) {
                    log.debug( "Bearer token presented but unknown or expired; falling through to Basic." );
                }
            }
        }

        filterChain.doFilter( request, response );
    }

    private static boolean isAnonymous( Authentication authentication ) {
        return authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken;
    }
}
