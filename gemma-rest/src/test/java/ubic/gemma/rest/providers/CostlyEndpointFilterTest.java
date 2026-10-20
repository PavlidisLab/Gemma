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
package ubic.gemma.rest.providers;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.server.model.Invocable;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import ubic.gemma.rest.annotations.Costly;

import java.lang.reflect.Method;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure-Mockito tests for {@link CostlyEndpointFilter}: what it does to a request when the pool has
 * room, and when it does not.
 */
class CostlyEndpointFilterTest {

    /**
     * Stands in for a real resource class; only the annotation on the method matters.
     */
    @SuppressWarnings("unused")
    private static class FakeResource {

        @Costly(value = "search", queueSeconds = 0)
        public void costly() {
        }

        public void free() {
        }
    }

    private CostlyEndpointBudget budget;
    private CostlyEndpointFilter filter;
    private ContainerRequestContext ctx;

    @BeforeEach
    void setUp() {
        budget = mock( CostlyEndpointBudget.class );
        ctx = mock( ContainerRequestContext.class );
        filter = new CostlyEndpointFilter();
        ReflectionTestUtils.setField( filter, "budget", budget );
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken( "key", "anonymousUser",
                        Collections.singletonList( new SimpleGrantedAuthority( "IS_AUTHENTICATED_ANONYMOUSLY" ) ) ) );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Point the request at one of {@link FakeResource}'s methods, the way Jersey does: the annotation
     * is reached through the matched {@code ResourceMethod} on an {@link ExtendedUriInfo}, not through
     * an injected field.
     */
    private void matchMethod( String name ) throws NoSuchMethodException {
        Method m = FakeResource.class.getDeclaredMethod( name );
        Invocable invocable = mock( Invocable.class );
        when( invocable.getDefinitionMethod() ).thenReturn( m );
        ResourceMethod resourceMethod = mock( ResourceMethod.class );
        when( resourceMethod.getInvocable() ).thenReturn( invocable );
        ExtendedUriInfo uriInfo = mock( ExtendedUriInfo.class );
        when( uriInfo.getMatchedResourceMethod() ).thenReturn( resourceMethod );
        when( uriInfo.getPath() ).thenReturn( "/genes/search" );
        when( ctx.getUriInfo() ).thenReturn( uriInfo );
    }

    @Test
    void anUnannotatedRouteIsNotCharged() throws Exception {
        matchMethod( "free" );

        filter.filter( ctx );

        verify( budget, never() ).tryAcquire( any(), org.mockito.ArgumentMatchers.anyInt() );
        verify( ctx, never() ).abortWith( any() );
        verify( ctx, never() ).setProperty( eq( CostlyEndpointFilter.POOL_PROPERTY ), any() );
    }

    @Test
    void anAcquiredPermitIsRecordedOnTheRequestForTheReleaseToFind() throws Exception {
        matchMethod( "costly" );
        when( budget.tryAcquire( "search", 0 ) ).thenReturn( true );

        filter.filter( ctx );

        verify( ctx ).setProperty( CostlyEndpointFilter.POOL_PROPERTY, "search" );
        verify( ctx, never() ).abortWith( any() );
    }

    @Test
    void aFullPoolAnswers503WithRetryAfterAndLeavesNoPermitRecorded() throws Exception {
        matchMethod( "costly" );
        when( budget.tryAcquire( "search", 0 ) ).thenReturn( false );

        filter.filter( ctx );

        ArgumentCaptor<Response> response = ArgumentCaptor.forClass( Response.class );
        verify( ctx ).abortWith( response.capture() );
        assertThat( response.getValue().getStatus() ).isEqualTo( 503 );
        assertThat( response.getValue().getHeaderString( HttpHeaders.RETRY_AFTER ) ).isEqualTo( "5" );
        // Nothing to release later; recording a pool here would leak a permit on every refusal.
        verify( ctx, never() ).setProperty( eq( CostlyEndpointFilter.POOL_PROPERTY ), any() );
    }

    @Test
    void anAuthenticatedCallerTakesNoPermitAtAll() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken( "curator", "x",
                        Collections.singletonList( new SimpleGrantedAuthority( "GROUP_CURATOR" ) ) ) );
        matchMethod( "costly" );

        filter.filter( ctx );

        // Not "acquired and allowed" -- never asked. A logged-in caller cannot be refused by a full pool.
        verify( budget, never() ).tryAcquire( any(), org.mockito.ArgumentMatchers.anyInt() );
        verify( ctx, never() ).abortWith( any() );
        verify( ctx, never() ).setProperty( eq( CostlyEndpointFilter.POOL_PROPERTY ), any() );
    }

    @Test
    void anEmptySecurityContextCountsAsAnonymousRatherThanFailing() throws Exception {
        SecurityContextHolder.clearContext();
        matchMethod( "costly" );
        when( budget.tryAcquire( "search", 0 ) ).thenReturn( true );

        filter.filter( ctx );

        verify( budget ).tryAcquire( "search", 0 );
    }
}
