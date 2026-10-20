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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import ubic.gemma.rest.annotations.Costly;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Concurrency budgets for the unauthenticated reads marked {@link Costly}.
 *
 * <h2>Pools</h2>
 * Each pool caps how many <em>anonymous</em> requests may be in flight against a group of routes that
 * contend for the same resource. The defaults below are declared in source, in the manner of
 * {@code EhcacheConfig#APP_CACHES}, and each is overridable at deploy time with
 * {@code gemma.rest.budget.<pool>.permits} in {@code Gemma.properties} (or the matching
 * {@code GEMMA_REST_BUDGET_*} environment variable).
 *
 * <h2>Authenticated callers are exempt</h2>
 * A request carrying a credential takes no permit and is never refused by this mechanism, however
 * full the pool is. The budget exists for unauthenticated read traffic; a logged-in curator is not
 * what it is bounding, and making them queue behind anonymous load would be the wrong trade. The
 * consequence to be aware of is that authenticated traffic is unbounded here — if it ever needs a
 * cap, that is a separate decision and a separate pool, not a widening of this one.
 *
 * <h2>Sizing</h2>
 * The numbers below are starting points, not measurements. Set them from the p95 figures
 * {@code scripts/perf_search.py} reports: the useful cap is the concurrency at which p95 latency
 * times the permit count still fits inside the connection pool and the heap. The rejection counter
 * is how you tell whether a cap is doing anything — one that never moves is too loose to matter, one
 * that climbs outside a traffic spike is too tight.
 *
 * @see Costly
 * @see CostlyEndpointFilter
 */
@Slf4j
@Component
public class CostlyEndpointBudget implements InitializingBean {

    /**
     * Pool name to the number of concurrent anonymous requests it allows. Adding a pool here is what
     * makes {@code @Costly("name")} legal; an annotation naming something absent from this map fails
     * at startup.
     */
    static final Map<String, Integer> POOLS;

    static {
        Map<String, Integer> pools = new LinkedHashMap<>();
        // Whole expression matrices: bandwidth and heap, and the response is streamed, so a permit is
        // held for the duration of the download rather than the query.
        pools.put( "vectors", 4 );
        // Differential-expression result fetches; ProcessedExpressionDataVectorServiceImpl already logs
        // these past 1s.
        pools.put( "diffex", 6 );
        // Matrix computations behind the dataset visualizations.
        pools.put( "viz", 6 );
        // Lucene plus the ontology fan-out; CompositeSearchSource logs the per-source split.
        pools.put( "search", 12 );
        // GO subtree walks, which grow with the breadth of the term.
        pools.put( "goterms", 6 );
        POOLS = Collections.unmodifiableMap( pools );
    }

    private final Map<String, Semaphore> permits = new ConcurrentHashMap<>();
    private final Map<String, Counter> rejections = new ConcurrentHashMap<>();

    /**
     * Absent in a test context that does not import Gemma's settings; the declared defaults are then
     * used unchanged, which is what "no overrides" means anyway.
     */
    @Nullable
    @Autowired(required = false)
    @Qualifier("settingsPropertySources")
    private PropertySources propertySources;

    /**
     * Absent unless the metrics profile is active, matching {@code MetricsWebService}.
     */
    @Nullable
    @Autowired(required = false)
    private PrometheusMeterRegistry meterRegistry;

    @Override
    public void afterPropertiesSet() {
        PropertySourcesPropertyResolver resolver = new PropertySourcesPropertyResolver(
                propertySources != null ? propertySources : new MutablePropertySources() );
        for ( Map.Entry<String, Integer> e : POOLS.entrySet() ) {
            String pool = e.getKey();
            int size = resolver.getProperty( "gemma.rest.budget." + pool + ".permits", Integer.class, e.getValue() );
            if ( size < 1 ) {
                throw new IllegalStateException( "gemma.rest.budget." + pool + ".permits must be at least 1, got " + size + "." );
            }
            // Fair queueing: without it a steady stream of arrivals can barge ahead of a thread that has
            // already been waiting, and the timeout below then expires for the waiter rather than the
            // newcomer.
            permits.put( pool, new Semaphore( size, true ) );
            registerMetrics( pool, size );
            log.info( "Costly-endpoint budget " + pool + ": " + size + " concurrent anonymous requests." );
        }
    }

    private void registerMetrics( String pool, int size ) {
        if ( meterRegistry == null ) {
            return;
        }
        Gauge.builder( "gemma_rest_budget_permits_in_use", permits.get( pool ), s -> size - s.availablePermits() )
                .tag( "pool", pool )
                .description( "Anonymous requests currently holding a permit in this concurrency budget." )
                .register( meterRegistry );
        Gauge.builder( "gemma_rest_budget_permits_total", permits.get( pool ), s -> size )
                .tag( "pool", pool )
                .description( "Size of this concurrency budget." )
                .register( meterRegistry );
        rejections.put( pool, Counter.builder( "gemma_rest_budget_rejections_total" )
                .tag( "pool", pool )
                .description( "Anonymous requests answered 503 because this budget was full." )
                .register( meterRegistry ) );
    }

    /**
     * Take a permit from a pool, waiting up to {@code queueSeconds} for one.
     * <p>
     * Only called for unauthenticated requests; see the class comment.
     *
     * @return true if a permit was taken and {@link #release(String)} must be called for it
     * @throws IllegalArgumentException if no such pool is configured, which
     *                                  {@link CostlyEndpointApplicationEventListener} rules out at startup
     */
    public boolean tryAcquire( String pool, int queueSeconds ) {
        Semaphore semaphore = permits.get( pool );
        if ( semaphore == null ) {
            throw new IllegalArgumentException( "No budget is configured for pool " + pool + "." );
        }
        try {
            if ( semaphore.tryAcquire( queueSeconds, TimeUnit.SECONDS ) ) {
                return true;
            }
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
        }
        countRejection( pool );
        return false;
    }

    /**
     * Hand back a permit taken by {@link #tryAcquire(String, int)}.
     */
    public void release( String pool ) {
        Semaphore semaphore = permits.get( pool );
        if ( semaphore != null ) {
            semaphore.release();
        }
    }

    private void countRejection( String pool ) {
        Counter c = rejections.get( pool );
        if ( c != null ) {
            c.increment();
        }
    }
}
