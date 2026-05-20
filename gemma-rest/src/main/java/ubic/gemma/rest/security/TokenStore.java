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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * In-memory bearer-token store backing the {@code /rest/v2/login} flow used by the
 * curation-UI SPA (see {@code AUTH_FOR_SPA_RECCE.md} Option C).
 *
 * <p>Tokens are opaque random UUIDs minted on a successful
 * {@code POST /rest/v2/login} and resolved back to the original {@link Authentication}
 * object by {@link BearerTokenAuthenticationFilter}.
 *
 * <h2>NOTE: design choices (deliberately punted for the MVP — flagged for follow-up)</h2>
 * <ul>
 *   <li><b>TTL: 8h sliding</b> ({@link Caffeine#expireAfterAccess}). Hardcoded; easy to
 *       promote to a configurable Gemma property later. Picked to cover one curator
 *       workday without forcing a re-login mid-session.</li>
 *   <li><b>Concurrent sessions: allowed.</b> Multiple live tokens per principal is fine
 *       (curators may have a laptop + desktop). No single-session enforcement here;
 *       gemma-web's {@code <s:concurrency-control>} only applied to the JSESSIONID
 *       flow which the standalone REST WAR doesn't speak.</li>
 *   <li><b>Audit-log auth events: skipped for v1.</b> Gemma's audit machinery is
 *       entity-scoped ({@code AuditTrail} on Securables); plumbing "session created /
 *       revoked" auth events through it is non-trivial and not blocking for the
 *       curator-UI MVP. Add later if/when ops wants login auditing.</li>
 *   <li><b>In-memory only.</b> WAR restart invalidates every live token; curators
 *       re-login. Acceptable for the MVP. Graduation path is a {@code gemd.auth_tokens}
 *       table or Redis; the store abstraction does not preclude either.</li>
 * </ul>
 *
 * <p>Thread-safe by virtue of Caffeine's underlying {@code ConcurrentMap}.
 *
 * @see BearerTokenAuthenticationFilter
 * @see AuthWebService
 */
@Component
public class TokenStore {

    /**
     * Sliding TTL: a token is invalidated 8 hours after its last successful lookup.
     * Picked to cover one curator workday; promoted to a config property later if needed.
     */
    public static final Duration TTL = Duration.ofHours( 8 );

    private final Cache<String, Authentication> cache;

    public TokenStore() {
        this.cache = Caffeine.newBuilder()
                .expireAfterAccess( TTL )
                .build();
    }

    /**
     * Mint a new opaque token for the given authenticated principal.
     *
     * @param authentication a successful {@link Authentication} (must be
     *                       {@code isAuthenticated() == true}); typically the result of
     *                       {@code AuthenticationManager.authenticate(...)}.
     * @return a fresh random UUID token string. Callers should treat it as opaque.
     */
    public String issue( Authentication authentication ) {
        String token = UUID.randomUUID().toString();
        cache.put( token, authentication );
        return token;
    }

    /**
     * Look up the {@link Authentication} previously associated with this token, refreshing
     * the sliding-TTL clock. Returns {@code null} if the token is unknown or expired.
     */
    @Nullable
    public Authentication lookup( String token ) {
        return cache.getIfPresent( token );
    }

    /**
     * Revoke a token. Idempotent: removing an already-removed token is a no-op (does not
     * throw, does not log). Used by {@code POST /rest/v2/logout}.
     */
    public void revoke( String token ) {
        cache.invalidate( token );
    }

    /**
     * For diagnostics/testing only.
     */
    long size() {
        cache.cleanUp();
        return cache.estimatedSize();
    }
}
