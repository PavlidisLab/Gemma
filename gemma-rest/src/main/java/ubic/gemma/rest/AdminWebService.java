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
import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import ubic.gemma.core.job.SubmittedTask;
import ubic.gemma.core.job.TaskRunningService;
import ubic.gemma.rest.util.ResponseDataObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
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
@CommonsLog
public class AdminWebService {

    private final CacheManager cacheManager;
    private final SessionFactory sessionFactory;
    private final TaskRunningService taskRunningService;

    /**
     * Filesystem root holding the per-entity Hibernate Search 7 Lucene index
     * sub-directories. Wired from {@code gemma.search.dir}; see
     * {@code HibernateConfig#resolveSearchIndexBase}.
     */
    @Nullable
    @Value("${gemma.search.dir:}")
    private String searchIndexBase;

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

    /* ===== Search indices ===== */

    /**
     * Per-{@code @Indexed}-entity Hibernate Search 7 index status. Replaces the
     * legacy gemma-web {@code indexer.js} flow that pinged the indexer directly
     * to discover what was indexable. The new UI uses this read-only view to
     * surface index sizes / on-disk paths; rebuild actions stay in the CLI
     * ({@code IndexGemmaCLI}).
     */
    @GET
    @Path("/search/indices")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Hibernate Search 7 per-index status",
            description = "Enumerates every @Indexed entity known to Hibernate Search 7 and reports, for each: the JPA entity name, the Java simple class name, the Lucene index name, the on-disk index directory (under `gemma.search.dir`), the document count (matchAll fetchTotalHitCount), and the directory mtime. Document-count queries run in a short-lived Hibernate session; on query failure the documentCount field is reported as -1 and the error message is included.",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(implementation = ResponseDataObject.class)))
            })
    public ResponseDataObject<SearchIndicesResponse> getSearchIndices() {
        SearchMapping mapping = Search.mapping( sessionFactory );
        Collection<? extends SearchIndexedEntity<?>> indexed = mapping.allIndexedEntities();
        List<IndexStatusValueObject> indices = new ArrayList<>( indexed.size() );
        long totalDocs = 0;
        boolean totalDocsExact = true;
        try ( Session session = sessionFactory.openSession() ) {
            for ( SearchIndexedEntity<?> entity : indexed ) {
                indices.add( buildStatus( entity, session ) );
            }
        }
        for ( IndexStatusValueObject vo : indices ) {
            if ( vo.documentCount < 0 ) {
                totalDocsExact = false;
            } else {
                totalDocs += vo.documentCount;
            }
        }
        indices.sort( Comparator.comparing( v -> v.entityName, Comparator.nullsLast( Comparator.naturalOrder() ) ) );
        SearchIndicesResponse body = new SearchIndicesResponse();
        body.indexBase = searchIndexBase;
        body.count = indices.size();
        body.totalDocumentCount = totalDocs;
        body.totalDocumentCountExact = totalDocsExact;
        body.indices = indices;
        return respond( body );
    }

    private <E> IndexStatusValueObject buildStatus( SearchIndexedEntity<E> entity, Session session ) {
        IndexStatusValueObject vo = new IndexStatusValueObject();
        vo.entityName = entity.jpaName();
        vo.className = entity.javaClass().getSimpleName();
        vo.indexName = entity.indexManager().descriptor().hibernateSearchName();
        // Doc count via matchAll() fetchTotalHitCount() — backend-agnostic, doesn't
        // require unwrapping to the Lucene-specific extension. A blank / missing
        // gemma.search.dir falls back to a tmpdir path in HibernateConfig, so the
        // query is always answerable; only schema problems would push us to the
        // catch arm.
        try {
            vo.documentCount = Search.session( session )
                    .search( entity.javaClass() )
                    .where( f -> f.matchAll() )
                    .fetchTotalHitCount();
        } catch ( RuntimeException e ) {
            log.warn( "Failed to count documents for " + entity.javaClass().getName() + ": " + e.getMessage() );
            vo.documentCount = -1L;
            vo.error = e.getClass().getSimpleName() + ": " + e.getMessage();
        }
        // Per-index on-disk directory — HS 7 Lucene local-filesystem backend names the
        // sub-directory after IndexDescriptor.hibernateSearchName() under directory.root.
        if ( searchIndexBase != null && !searchIndexBase.trim().isEmpty() ) {
            java.nio.file.Path indexDir = Paths.get( searchIndexBase, vo.indexName );
            vo.indexPath = indexDir.toString();
            if ( Files.isDirectory( indexDir ) ) {
                vo.exists = true;
                try {
                    vo.lastModified = Files.getLastModifiedTime( indexDir ).toInstant();
                } catch ( IOException e ) {
                    log.debug( "Could not read mtime for " + indexDir + ": " + e.getMessage() );
                }
            } else {
                vo.exists = false;
            }
        }
        return vo;
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

    public static class SearchIndicesResponse {
        /** Filesystem root (gemma.search.dir) under which per-entity index dirs live. May be blank if unconfigured. */
        @Nullable
        public String indexBase;
        public int count;
        public long totalDocumentCount;
        /** False if any per-index count failed; the total is then a lower bound. */
        public boolean totalDocumentCountExact;
        public List<IndexStatusValueObject> indices;
    }

    public static class IndexStatusValueObject {
        /** JPA entity name (e.g. {@code ExpressionExperiment}). */
        public String entityName;
        /** Java simple class name. */
        public String className;
        /** Hibernate Search index name (the sub-directory name under {@code indexBase}). */
        public String indexName;
        /** Absolute path to the on-disk index directory, or null if {@code gemma.search.dir} is unset. */
        @Nullable
        public String indexPath;
        /** True if the on-disk index directory exists; null if {@code indexPath} is null. */
        @Nullable
        public Boolean exists;
        /** Document count from a matchAll {@code fetchTotalHitCount}; -1 on query failure. */
        public long documentCount;
        /** mtime of the on-disk index directory; null if missing / unconfigured / unreadable. */
        @Nullable
        public Instant lastModified;
        /** Populated only when {@code documentCount == -1}. */
        @Nullable
        public String error;
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
