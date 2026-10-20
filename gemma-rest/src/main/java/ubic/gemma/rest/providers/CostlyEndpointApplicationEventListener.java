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

import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import ubic.gemma.rest.annotations.Costly;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Hands back the permits {@link CostlyEndpointFilter} takes, and checks at startup that every
 * {@link Costly} annotation names a pool that exists.
 *
 * <h2>Why the release lives on a request event</h2>
 * {@code RequestEvent.Type#FINISHED} is the only hook that fires after a streamed entity has been
 * written. A {@code ContainerResponseFilter} runs when the response is dispatched, which for
 * {@code /datasets/{id}/data/processed} is before the matrix has gone out — releasing there would
 * make the {@code vectors} pool a cap on starting downloads rather than on running ones.
 * {@code AnalyticsRequestEventListener} tracks per-request state the same way.
 *
 * <p>FINISHED fires for aborted and failed requests too, so a permit taken by the filter is handed
 * back even when the resource method throws or the client disconnects.
 *
 * @see CostlyEndpointFilter
 * @see CostlyEndpointBudget
 */
@Slf4j
@Provider
@Component
public class CostlyEndpointApplicationEventListener implements ApplicationEventListener {

    /**
     * Absent in the mocked contexts the {@code gemma-rest} tests build; nothing was acquired there
     * either, so there is nothing to hand back. The startup check below runs regardless.
     */
    @Nullable
    @Autowired(required = false)
    private CostlyEndpointBudget budget;

    @Override
    public void onEvent( ApplicationEvent event ) {
        if ( event.getType() == ApplicationEvent.Type.INITIALIZATION_FINISHED ) {
            checkPoolsExist( event );
        }
    }

    @Override
    public RequestEventListener onRequest( RequestEvent requestEvent ) {
        return new ReleaseListener();
    }

    /**
     * Fail the application rather than let a route silently go unbounded because its pool name was
     * misspelled.
     */
    private void checkPoolsExist( ApplicationEvent event ) {
        Set<String> unknown = new LinkedHashSet<>();
        int annotated = 0;
        for ( Resource resource : event.getResourceModel().getResources() ) {
            for ( Method m : costlyMethods( resource ) ) {
                annotated++;
                String pool = m.getAnnotation( Costly.class ).value();
                if ( !CostlyEndpointBudget.POOLS.containsKey( pool ) ) {
                    unknown.add( pool + " (on " + m.getDeclaringClass().getSimpleName() + "#" + m.getName() + ")" );
                }
            }
        }
        if ( !unknown.isEmpty() ) {
            throw new IllegalStateException( "@Costly names no such budget: " + String.join( ", ", unknown )
                    + ". Known pools: " + String.join( ", ", CostlyEndpointBudget.POOLS.keySet() ) + "." );
        }
        log.info( "Concurrency budgets cover " + annotated + " resource methods across "
                + CostlyEndpointBudget.POOLS.size() + " pools." );
    }

    private static List<Method> costlyMethods( Resource resource ) {
        List<Method> found = new ArrayList<>();
        for ( ResourceMethod rm : resource.getAllMethods() ) {
            Method m = rm.getInvocable().getDefinitionMethod();
            if ( m.isAnnotationPresent( Costly.class ) ) {
                found.add( m );
            }
        }
        for ( Resource child : resource.getChildResources() ) {
            found.addAll( costlyMethods( child ) );
        }
        return found;
    }

    /**
     * Releases the permit recorded on the request, once, when the request is finished with.
     */
    private class ReleaseListener implements RequestEventListener {

        @Override
        public void onEvent( RequestEvent event ) {
            if ( event.getType() != RequestEvent.Type.FINISHED ) {
                return;
            }
            ContainerRequest request = event.getContainerRequest();
            Object pool = request.getProperty( CostlyEndpointFilter.POOL_PROPERTY );
            if ( !( pool instanceof String ) ) {
                return;
            }
            // Cleared first so a second FINISHED could not double-release into the semaphore, which
            // would permanently widen the pool.
            request.removeProperty( CostlyEndpointFilter.POOL_PROPERTY );
            if ( budget != null ) {
                budget.release( ( String ) pool );
            }
        }
    }
}
