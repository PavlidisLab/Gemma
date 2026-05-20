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
package ubic.gemma.rest;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.rest.RootWebService.UserValueObject;
import ubic.gemma.rest.security.BearerTokenAuthenticationFilter;
import ubic.gemma.rest.security.TokenStore;
import ubic.gemma.rest.util.ResponseDataObject;

import static ubic.gemma.rest.util.Responders.respond;

/**
 * Bearer-token auth endpoints for the curation-UI SPA. Implements Option C of
 * {@code AUTH_FOR_SPA_RECCE.md}: opaque token minted on credential check,
 * sent on every subsequent call as {@code Authorization: Bearer <token>}.
 *
 * <p>Wire contract (matches what the SPA's
 * {@code gemma-curation-ui/apps/curation/src/api/session.ts} expects):
 * <pre>
 *   POST /rest/v2/login   {"username", "password"}        &rarr; 200 {"token", "user"}
 *   POST /rest/v2/logout                                  &rarr; 200 (idempotent)
 *   GET  /rest/v2/me                                      &rarr; 200 user|null
 * </pre>
 *
 * <p>Note: the legacy HTTP Basic chain is preserved for CLI / RClient / cron clients;
 * see {@link ubic.gemma.rest.security.RestSecurityConfig} for the filter ordering.
 *
 * @see TokenStore
 * @see BearerTokenAuthenticationFilter
 */
@Service
@Path("/")
@Slf4j
public class AuthWebService {

    @Autowired
    @Qualifier("authenticationManager")
    private AuthenticationManager authenticationManager;

    @Autowired
    private TokenStore tokenStore;

    @Autowired
    private UserManager userManager;

    /**
     * Verify credentials, mint a new opaque bearer token, and return both the token and
     * the canonical user shape (mirrors {@code GET /rest/v2/users/me}).
     *
     * <p>Anonymous callers and bad credentials produce 401. Empty payload produces 400.
     */
    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Authenticate username/password and mint an opaque bearer token",
            description = "POST a JSON body {username, password}. On success the response carries "
                    + "{token, user} where token is an opaque random string; "
                    + "send it as Authorization: Bearer <token> on subsequent calls.")
    public Response login( LoginRequest req ) {
        if ( req == null || req.username == null || req.password == null
                || req.username.isEmpty() ) {
            throw new BadRequestException( "username and password are required" );
        }
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken( req.username, req.password ) );
        } catch ( AuthenticationException e ) {
            // Don't leak which side (user vs password) was wrong; same 401 either way.
            log.debug( "Login rejected for username='{}': {}", req.username, e.getMessage() );
            throw new NotAuthorizedException( "Invalid credentials", "xBasic realm=\"Gemma RESTful API\"" );
        }
        if ( authentication == null || !authentication.isAuthenticated() ) {
            throw new NotAuthorizedException( "Invalid credentials", "xBasic realm=\"Gemma RESTful API\"" );
        }
        String token = tokenStore.issue( authentication );
        User user = userManager.findByUserName( req.username );
        UserValueObject userVo = user != null ? toUserVo( user ) : null;
        return Response.ok( new ResponseDataObject<>( new LoginResponse( token, userVo ) ) ).build();
    }

    /**
     * Revoke the bearer token presented in the {@code Authorization} header. Idempotent —
     * revoking an unknown / already-revoked token is a 200 with empty body, matching the
     * SPA's expectation that logout always "succeeds" so it can clear local state
     * unconditionally.
     */
    @POST
    @Path("/logout")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Revoke the presented bearer token (idempotent)")
    public Response logout( @Context HttpServletRequest request ) {
        String token = BearerTokenAuthenticationFilter.extractBearerToken( request.getHeader( "Authorization" ) );
        if ( token != null ) {
            tokenStore.revoke( token );
        }
        // 200 with empty body. The SPA's useLogout() ignores the body.
        return Response.ok().build();
    }

    /**
     * Convenience alias for {@code GET /rest/v2/users/me}; the curation-UI SPA's
     * {@code useMe()} hook points at {@code /rest/v2/me} verbatim, so we mirror the shape
     * here rather than asking the SPA to track the longer path.
     *
     * <p>Returns the canonical {@link UserValueObject} (same as {@link RootWebService#getMyself()}).
     */
    @GET
    @Path("/me")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Retrieve the authenticated user (alias of /rest/v2/users/me)")
    public ResponseDataObject<UserValueObject> me() {
        User user = userManager.getCurrentUser();
        if ( user == null ) {
            return respond( null );
        }
        return respond( toUserVo( user ) );
    }

    private UserValueObject toUserVo( User user ) {
        String group = userManager.findGroupsForUser( user.getUserName() ).stream().findFirst().orElse( null );
        return new UserValueObject( user, group );
    }

    /**
     * JSON body for {@code POST /login}. JAX-RS / Jackson populates this via the
     * {@code @Consumes(APPLICATION_JSON)} provider. Lombok {@code @Data} would be nicer
     * but the rest of this module favors plain POJOs for SwaggerCore-discovered request
     * bodies; sticking with that.
     */
    public static class LoginRequest {
        @Nullable
        public String username;
        @Nullable
        public String password;

        public LoginRequest() {
        }

        @Nullable
        public String getUsername() {
            return username;
        }

        public void setUsername( @Nullable String username ) {
            this.username = username;
        }

        @Nullable
        public String getPassword() {
            return password;
        }

        public void setPassword( @Nullable String password ) {
            this.password = password;
        }
    }

    /**
     * Wire payload for {@code POST /login} — {@code {token, user}}. Matches the SPA's
     * {@code LoginResponse} type literally.
     */
    @lombok.Value
    public static class LoginResponse {
        String token;
        @Nullable
        UserValueObject user;
    }
}
