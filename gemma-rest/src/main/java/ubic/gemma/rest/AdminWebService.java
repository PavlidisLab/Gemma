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
 */
package ubic.gemma.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import ubic.gemma.core.job.SubmittedTask;
import ubic.gemma.core.job.TaskRunningService;
import ubic.gemma.rest.util.ResponseDataObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

import static ubic.gemma.rest.util.Responders.respond;

/**
 * Admin-only system monitoring surface for the gemma-curation-ui admin panel.
 * Replaces the legacy gemma-web {@code SystemMonitorController} DWR calls
 * ({@code getCacheStatus}, {@code clearAllCaches}, {@code clearCache},
 * {@code getHibernateStatus}) with structured JSON-returning endpoints.
 *
 * <p>All endpoints require {@code GROUP_ADMIN} authority.</p>
 *
 * <p>Note: the legacy {@code CacheMonitor.enableStatistics} /
 * {@code disableStatistics} entry points are stubs on the current post-EhCache-2
 * build (see {@code CacheMonitorImpl}) and are not exposed here. The legacy
 * {@code resetHibernateStatus} is also not exposed pending a UX decision on
 * whether the admin panel needs a Hibernate-stats reset button at all.</p>
 *
 * @author phase 3 admin-panel wiring
 */
@Service
@Path("/admin")
@Tag(name = "Admin", description = "System monitoring endpoints — admin only")
public class AdminWebService {

    private final CacheManager cacheManager;
    private final SessionFactory sessionFactory;
    private final TaskRunningService taskRunningService;

    @Autowired
    public AdminWebService( CacheManager cacheManager, SessionFactory sessionFactory,
            TaskRunningService taskRunningService ) {
        this.cacheManager = cacheManager;
        this.sessionFactory = sessionFactory;
        this.taskRunningService = taskRunningService;
    }

    /* ===== Caches ===== */

    /**
     * Lists the registered Spring caches by name. Replaces the HTML-returning
     * legacy {@code SystemMonitorController.getCacheStatus()}.
     */
    @GET
    @Path("/caches")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "List the registered caches",
            description = "Returns the names of all caches known to the Spring CacheManager, sorted alphabetically. Per-cache hit/miss statistics are not available on the current build (post-EhCache-2 era).",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(implementation = ResponseDataObject.class)))
            })
    public ResponseDataObject<CacheListResponse> getCaches() {
        Collection<String> names = cacheManager.getCacheNames();
        List<String> sorted = new ArrayList<>( new TreeSet<>( names ) );
        CacheListResponse body = new CacheListResponse();
        body.count = sorted.size();
        body.names = sorted;
        return respond( body );
    }

    /**
     * Clears every registered cache. Replaces the legacy
     * {@code SystemMonitorController.clearAllCaches()} DWR call.
     */
    @DELETE
    @Path("/caches")
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Clear all registered caches",
            description = "Iterates over the Spring CacheManager and clears every cache. Returns 204 on success.",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = {
                    @ApiResponse(responseCode = "204")
            })
    public Response clearAllCaches() {
        for ( String name : cacheManager.getCacheNames() ) {
            Cache cache = cacheManager.getCache( name );
            if ( cache != null ) {
                cache.clear();
            }
        }
        return Response.noContent().build();
    }

    /**
     * Clears a single named cache. Replaces the legacy
     * {@code SystemMonitorController.clearCache(name)} DWR call.
     */
    @DELETE
    @Path("/caches/{cacheName}")
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Clear a single named cache",
            description = "Clears the cache identified by `{cacheName}`. Returns 204 on success, 404 if no cache by that name is registered.",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = {
                    @ApiResponse(responseCode = "204"),
                    @ApiResponse(responseCode = "404", description = "No cache registered under that name")
            })
    public Response clearCache( @PathParam("cacheName") String cacheName ) {
        Cache cache = cacheManager.getCache( cacheName );
        if ( cache == null ) {
            throw new NotFoundException( "No cache found with name=" + cacheName );
        }
        cache.clear();
        return Response.noContent().build();
    }

    /* ===== Hibernate stats ===== */

    /**
     * Returns a structured snapshot of Hibernate statistics. Replaces the
     * HTML-returning legacy {@code SystemMonitorController.getHibernateStatus()}.
     */
    @GET
    @Path("/hibernate/stats")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Hibernate statistics snapshot",
            description = "Returns a flat snapshot of the Hibernate session-factory statistics: counts of sessions opened/closed, transactions, flushes, prepared statements, queries executed, plus query-cache and second-level-cache hit/miss/put counters. When Hibernate statistics collection is disabled the counters report zero.",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(implementation = ResponseDataObject.class)))
            })
    public ResponseDataObject<HibernateStatsResponse> getHibernateStats() {
        Statistics s = sessionFactory.getStatistics();
        HibernateStatsResponse body = new HibernateStatsResponse();
        body.statisticsEnabled = s.isStatisticsEnabled();
        body.startTime = new Date( s.getStartTime() );
        body.sessionOpenCount = s.getSessionOpenCount();
        body.sessionCloseCount = s.getSessionCloseCount();
        body.transactionCount = s.getTransactionCount();
        body.flushCount = s.getFlushCount();
        body.prepareStatementCount = s.getPrepareStatementCount();
        body.queryExecutionCount = s.getQueryExecutionCount();
        body.queryExecutionMaxTimeMillis = s.getQueryExecutionMaxTime();
        body.queryExecutionMaxTimeQuery = s.getQueryExecutionMaxTimeQueryString();
        body.queryCacheHitCount = s.getQueryCacheHitCount();
        body.queryCacheMissCount = s.getQueryCacheMissCount();
        body.queryCachePutCount = s.getQueryCachePutCount();
        body.secondLevelCacheHitCount = s.getSecondLevelCacheHitCount();
        body.secondLevelCacheMissCount = s.getSecondLevelCacheMissCount();
        body.secondLevelCachePutCount = s.getSecondLevelCachePutCount();
        return respond( body );
    }

    /**
     * Resets the Hibernate statistics counters to zero. Replaces the legacy
     * {@code SystemMonitorController.resetHibernateStatus()} DWR call.
     */
    @POST
    @Path("/hibernate/reset")
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Reset Hibernate statistics",
            description = "Clears the Hibernate session-factory statistics counters. Returns 204 on success. Has no effect on whether statistics collection itself is enabled.",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = {
                    @ApiResponse(responseCode = "204")
            })
    public Response resetHibernateStats() {
        sessionFactory.getStatistics().clear();
        return Response.noContent().build();
    }

    /* ===== Background jobs ===== */

    /**
     * Aggregated admin view of the in-memory background task queue. Returns the per-task
     * {@link TaskStatusValueObject} snapshots plus counts of tasks in each status. The underlying
     * task store is in-memory only and tasks are evicted ~10 minutes after completion.
     */
    @GET
    @Path("/jobs")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "List currently tracked background tasks",
            description = "Returns a snapshot of every task currently tracked by the in-memory task store, sorted by submission time (newest first), with counts of tasks by status. The store evicts tasks roughly 10 minutes after completion.",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(implementation = ResponseDataObject.class)))
            })
    public ResponseDataObject<JobsListResponse> getJobs() {
        Collection<SubmittedTask> tasks = taskRunningService.getSubmittedTasks();
        List<TaskStatusValueObject> snapshots = new ArrayList<>();
        int queued = 0, running = 0, completed = 0, failed = 0, cancelling = 0, unknown = 0;
        for ( SubmittedTask task : tasks ) {
            snapshots.add( new TaskStatusValueObject( task ) );
            SubmittedTask.Status st = task.getStatus();
            if ( st == null ) {
                unknown++;
                continue;
            }
            switch ( st ) {
                case QUEUED: queued++; break;
                case RUNNING: running++; break;
                case COMPLETED: completed++; break;
                case FAILED: failed++; break;
                case CANCELLING: cancelling++; break;
                default: unknown++; break;
            }
        }
        // newest first; nulls sort last
        snapshots.sort( Comparator.comparing( TaskStatusValueObject::getSubmittedAt,
                Comparator.nullsLast( Comparator.reverseOrder() ) ) );
        JobsListResponse body = new JobsListResponse();
        body.total = snapshots.size();
        body.queued = queued;
        body.running = running;
        body.completed = completed;
        body.failed = failed;
        body.cancelling = cancelling;
        body.unknown = unknown;
        body.tasks = snapshots;
        return respond( body );
    }

    /* ===== DTOs ===== */

    public static class CacheListResponse {
        public int count;
        public List<String> names;
    }

    public static class JobsListResponse {
        public int total;
        public int queued;
        public int running;
        public int completed;
        public int failed;
        public int cancelling;
        public int unknown;
        public List<TaskStatusValueObject> tasks;
    }

    public static class HibernateStatsResponse {
        public boolean statisticsEnabled;
        public Date startTime;
        public long sessionOpenCount;
        public long sessionCloseCount;
        public long transactionCount;
        public long flushCount;
        public long prepareStatementCount;
        public long queryExecutionCount;
        public long queryExecutionMaxTimeMillis;
        @Nullable
        public String queryExecutionMaxTimeQuery;
        public long queryCacheHitCount;
        public long queryCacheMissCount;
        public long queryCachePutCount;
        public long secondLevelCacheHitCount;
        public long secondLevelCacheMissCount;
        public long secondLevelCachePutCount;
    }
}
