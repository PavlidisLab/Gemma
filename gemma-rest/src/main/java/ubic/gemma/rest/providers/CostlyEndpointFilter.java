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

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import ubic.gemma.core.security.util.SecurityUtil;
import ubic.gemma.rest.annotations.Costly;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Takes a permit from the {@link CostlyEndpointBudget} for an unauthenticated request to a route
 * marked {@link Costly}, and answers 503 when the pool is full.
 *
 * <h2>Authenticated requests pass straight through</h2>
 * A request carrying a credential takes no permit and is never refused here. The budget is for
 * unauthenticated read traffic, so a logged-in caller is not queued behind it and cannot be turned
 * away by it.
 *
 * <h2>Where the permit is handed back</h2>
 * Not here, and not in a {@code ContainerResponseFilter}. A response filter runs when the response is
 * dispatched, which for a streamed entity is before the body has been written — releasing there would
 * hand the permit back at the <em>start</em> of a matrix download rather than the end, and the
 * {@code vectors} pool would cap nothing. The release therefore happens on Jersey's
 * {@code RequestEvent.Type#FINISHED}, which fires after the entity is written, in
 * {@link CostlyEndpointApplicationEventListener}. The two halves are joined by the
 * {@link #POOL_PROPERTY} request property set below.
 *
 * <h2>Why 503 and not 429</h2>
 * 429 says the client sent too many requests, which is usually untrue here: the traffic this bounds
 * arrives spread across many clients, and the request being refused may be the only one that client
 * has sent. 503 says the server is at capacity, which is the accurate statement, and it keeps the
 * distinction legible in the access log when someone later asks which one happened.
 *
 * @see CostlyEndpointBudget
 */
@Slf4j
@Provider
@Component
@Priority(Priorities.USER)
public class CostlyEndpointFilter implements ContainerRequestFilter {

    /**
     * Name of the pool a permit was taken from, or absent if none was.
     */
    static final String POOL_PROPERTY = "ubic.gemma.rest.costly.pool";

    /**
     * Long enough for a client to be worth retrying, short enough that a transient burst is not
     * turned into a stampede when everyone retries at once.
     */
    private static final String RETRY_AFTER_SECONDS = "5";

    /**
     * Absent in the mocked Spring contexts the {@code gemma-rest} tests build, where the providers are
     * still package-scanned by Jersey. {@link #budgetOrNull()} says what happens then.
     */
    @Nullable
    @Autowired(required = false)
    private CostlyEndpointBudget budget;

    private final AtomicBoolean warnedAboutMissingBudget = new AtomicBoolean();

    @Override
    public void filter( ContainerRequestContext requestContext ) {
        Costly costly = costlyAnnotation( requestContext );
        if ( costly == null ) {
            return;
        }
        if ( !isAnonymous() ) {
            return;
        }
        CostlyEndpointBudget budget = budgetOrNull();
        if ( budget == null ) {
            return;
        }
        if ( !budget.tryAcquire( costly.value(), costly.queueSeconds() ) ) {
            log.debug( "Budget " + costly.value() + " is full; refusing " + requestContext.getUriInfo().getPath() + "." );
            requestContext.abortWith( Response.status( Response.Status.SERVICE_UNAVAILABLE )
                    .header( HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS )
                    .type( MediaType.APPLICATION_JSON_TYPE )
                    .entity( "{\"error\":{\"code\":503,\"message\":\"Gemma is at capacity for this kind of request. Retry shortly.\"}}" )
                    .build() );
            return;
        }
        requestContext.setProperty( POOL_PROPERTY, costly.value() );
    }

    /**
     * The budget, or {@code null} if the context has none, in which case the route runs unbounded.
     * <p>
     * A missing budget is a wiring fault, not a configuration choice, so it is reported once at WARN
     * rather than passed over in silence — an inert budget looks exactly like a budget that is never
     * filled. The WAR component-scans {@code ubic.gemma.rest}, so the bean is present there;
     * {@code CostlyEndpointWiringTest} pins that.
     */
    @Nullable
    private CostlyEndpointBudget budgetOrNull() {
        if ( budget == null && warnedAboutMissingBudget.compareAndSet( false, true ) ) {
            log.warn( "No " + CostlyEndpointBudget.class.getSimpleName() + " bean is available; "
                    + "@Costly routes are running unbounded." );
        }
        return budget;
    }

    /**
     * Whether the caller is unauthenticated, and so subject to the budget at all.
     * <p>
     * {@link SecurityUtil#isUserAnonymous()} throws when the context holds no {@code Authentication}
     * at all. The REST chain configures {@code anonymous()} so one is always present under
     * {@code /rest/v2/**}, but an empty context is treated as anonymous here — the budgeted side of
     * the two — rather than turning a wiring gap into a 500 on an endpoint that would otherwise work.
     * Erring the other way would exempt a request precisely when the security context is broken.
     */
    private static boolean isAnonymous() {
        return SecurityContextHolder.getContext().getAuthentication() == null || SecurityUtil.isUserAnonymous();
    }

    /**
     * The {@link Costly} annotation on the matched resource method, if it carries one.
     * <p>
     * Read off Jersey's own {@link ExtendedUriInfo} rather than an injected
     * {@code @Context ResourceInfo} field, following {@link UnknownQueryParameterFilter}. A provider
     * that Spring instantiates does not reliably receive JAX-RS {@code @Context} field injection, and
     * the failure is silent: the field stays null, every route looks unannotated, and the budgets
     * quietly bound nothing. This filter is not {@code @PreMatching}, so the match is already
     * resolved by the time it runs.
     */
    @Nullable
    private static Costly costlyAnnotation( ContainerRequestContext requestContext ) {
        UriInfo uriInfo = requestContext.getUriInfo();
        if ( !( uriInfo instanceof ExtendedUriInfo ) ) {
            return null;
        }
        ResourceMethod matched = ( ( ExtendedUriInfo ) uriInfo ).getMatchedResourceMethod();
        if ( matched == null ) {
            return null;
        }
        Method m = matched.getInvocable().getDefinitionMethod();
        return m.getAnnotation( Costly.class );
    }
}
