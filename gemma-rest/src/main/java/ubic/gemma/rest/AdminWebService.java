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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
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
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import ubic.gemma.core.job.SubmittedTask;
import ubic.gemma.core.job.TaskRunningService;
import ubic.gemma.rest.util.args.PlatformArgService;
import ubic.gemma.rest.util.args.PlatformArg;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.core.analysis.report.ArrayDesignReportService;
import ubic.gemma.core.tasks.maintenance.ArrayDesignReportTaskCommand;
import ubic.gemma.core.search.indexer.IndexerService;
import ubic.gemma.core.loader.expression.geo.model.GeoRecord;
import ubic.gemma.core.loader.expression.geo.service.GeoBrowser;
import ubic.gemma.core.loader.expression.geo.service.GeoBrowserImpl;
import ubic.gemma.core.loader.expression.geo.service.GeoRecordType;
import ubic.gemma.core.loader.expression.geo.service.GeoRetrieveConfig;
import ubic.gemma.core.ontology.ObsoleteTermUsage;
import ubic.gemma.core.tasks.maintenance.ObsoleteTermCorrectionTaskCommand;
import ubic.gemma.core.ontology.providers.OntologyService;
import ubic.gemma.core.ontology.providers.OntologyServiceResolver;
import ubic.gemma.core.tasks.analysis.expression.ExpressionExperimentLoadTaskCommand;
import ubic.gemma.core.tasks.maintenance.GeoScrapeTaskCommand;
import ubic.gemma.core.tasks.maintenance.MultifunctionalityTaskCommand;
import ubic.gemma.core.geoscrape.GeoScrapeDryRunCandidate;
import ubic.gemma.core.geoscrape.GeoScrapeService;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.expression.experiment.GeoScrapeWatermark;
import ubic.gemma.core.security.AuthorityConstants;
import ubic.gemma.core.security.authentication.UserDetailsImpl;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.model.blacklist.BlacklistedEntity;
import ubic.gemma.model.blacklist.BlacklistedExperiment;
import ubic.gemma.model.blacklist.BlacklistedPlatform;
import ubic.gemma.model.blacklist.BlacklistedValueObject;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserGroup;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketType;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.persistence.service.blacklist.BlacklistedEntityService;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.TicketService;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseReadService;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.AnnotationSetService;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetRole;
import ubic.gemma.model.genome.Taxon;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.ResponseErrorObject;
import ubic.gemma.rest.util.args.TaxonArg;
import ubic.gemma.rest.util.args.TaxonArgService;

import javax.sql.DataSource;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

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
    private final PlatformArgService platformArgService;
    private final ArrayDesignReportService arrayDesignReportService;
    private final SessionRegistry sessionRegistry;
    private final List<OntologyService> ontologies;
    private final DataSource dataSource;
    private final UserManager userManager;
    private final AnnotationSetService annotationSetService;
    private final TicketService ticketService;
    private final TaxonArgService taxonArgService;
    private final BlacklistedEntityService blacklistedEntityService;
    private final ExternalDatabaseReadService externalDatabaseReadService;
    private final GeoScrapeService geoScrapeService;

    @Value("${gemma.curationAgent.healthUrl:}")
    private String curationAgentHealthUrl;

    @Value("${gemma.curationAgent.healthTimeoutMs:3000}")
    private int curationAgentHealthTimeoutMs;

    /**
     * NCBI E-utilities API key, threaded into the {@link GeoBrowser} so GEO scrapes
     * don't get rate-throttled. Blank when unconfigured (NCBI then enforces the
     * default 3-req/s limit).
     */
    @Value("${entrez.efetch.apikey:}")
    private String ncbiApiKey;

    /**
     * Lazily-initialised GeoBrowser used by {@link #grabGeoRecords(GeoGrabRequest)}.
     * Package-private setter exists for tests; production code constructs a
     * {@link GeoBrowserImpl} on first use.
     */
    @Nullable
    private GeoBrowser geoBrowser;

    /**
     * Filesystem root holding the per-entity Hibernate Search 7 Lucene index
     * sub-directories. Wired from {@code gemma.search.dir}; see
     * {@code HibernateConfig#resolveSearchIndexBase}.
     */
    @Nullable
    @Value("${gemma.search.dir:}")
    private String searchIndexBase;

    private final IndexerService indexerService;

    /**
     * Map from user-facing entity name (used as path/query param) to the
     * {@code @Indexed} class the mass-indexer drives. Kept in sync with
     * {@code IndexGemmaCLI.INDEXABLE_ENTITIES}; the CLI is still the canonical
     * source-of-truth for which roots Gemma indexes.
     */
    private static final Map<String, Class<? extends Identifiable>> INDEXABLE_ENTITIES;
    static {
        Map<String, Class<? extends Identifiable>> m = new LinkedHashMap<>();
        m.put( "genes", Gene.class );
        m.put( "datasets", ExpressionExperiment.class );
        m.put( "platforms", ArrayDesign.class );
        m.put( "bibliographicReferences", BibliographicReference.class );
        m.put( "probes", CompositeSequence.class );
        m.put( "sequences", BioSequence.class );
        m.put( "datasetGroups", ExpressionExperimentSet.class );
        m.put( "geneSets", GeneSet.class );
        INDEXABLE_ENTITIES = Collections.unmodifiableMap( m );
    }

    /**
     * Single-flight gate for the destructive mass-reindex path. Concurrent POSTs
     * receive {@code 409 Conflict} rather than racing each other through HS 7's
     * mass-indexer (which would purge each other's just-written segments).
     */
    private final AtomicBoolean reindexInProgress = new AtomicBoolean( false );

    /**
     * Per-entity status of the most recent reindex job (running / completed / failed).
     * Surfaced via {@code GET /admin/search/indices} alongside the doc-count read.
     */
    private final ConcurrentMap<String, String> reindexStatus = new ConcurrentHashMap<>();

    /**
     * Facade used to evict the in-process search / parents / children caches after a per-ontology
     * refresh. The provider-level {@code OntologyService} bean (per-ontology) reloads the Jena
     * model + Lucene index but does not touch {@code OntologyCache}, so stale results were being
     * served until a bounce — see {@link #refreshOntology(String, boolean)}.
     */
    private final ubic.gemma.core.ontology.OntologyService ontologyFacade;

    @Autowired
    public AdminWebService( CacheManager cacheManager, SessionFactory sessionFactory,
            TaskRunningService taskRunningService, SessionRegistry sessionRegistry,
            List<OntologyService> ontologies,
            ubic.gemma.core.ontology.OntologyService ontologyFacade,
            DataSource dataSource, UserManager userManager,
            AnnotationSetService annotationSetService, TicketService ticketService,
            TaxonArgService taxonArgService,
            BlacklistedEntityService blacklistedEntityService,
            ExternalDatabaseReadService externalDatabaseReadService,
            GeoScrapeService geoScrapeService,
            IndexerService indexerService,
            PlatformArgService platformArgService,
            ArrayDesignReportService arrayDesignReportService ) {
        this.cacheManager = cacheManager;
        this.platformArgService = platformArgService;
        this.arrayDesignReportService = arrayDesignReportService;
        this.sessionFactory = sessionFactory;
        this.taskRunningService = taskRunningService;
        this.sessionRegistry = sessionRegistry;
        this.ontologies = ontologies;
        this.ontologyFacade = ontologyFacade;
        this.dataSource = dataSource;
        this.userManager = userManager;
        this.annotationSetService = annotationSetService;
        this.ticketService = ticketService;
        this.taxonArgService = taxonArgService;
        this.blacklistedEntityService = blacklistedEntityService;
        this.externalDatabaseReadService = externalDatabaseReadService;
        this.geoScrapeService = geoScrapeService;
        this.indexerService = indexerService;
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
        body.caches = new ArrayList<>( sorted.size() );
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        for ( String name : sorted ) {
            body.caches.add( readCacheStats( mbs, name ) );
        }
        return respond( body );
    }

    /**
     * Read JCache standard stats for a single cache via the
     * {@code javax.cache:type=CacheStatistics,CacheManager=*,Cache=<name>} MBean.
     * Returns a row with null stat fields if the MBean is missing (cache configured
     * without statisticsEnabled, or non-JCache cache provider). The row always has its
     * name set so the UI can render it as a "stats unavailable" entry rather than skip.
     */
    private CacheStatRow readCacheStats( MBeanServer mbs, String name ) {
        CacheStatRow row = new CacheStatRow();
        row.name = name;
        try {
            // The Cache attribute can be quoted or unquoted depending on the JCache
            // provider's bean registration. Probe quoted first (Ehcache 3 default).
            ObjectName quoted = new ObjectName( "javax.cache:type=CacheStatistics,CacheManager=*,Cache=" + ObjectName.quote( name ) );
            Set<ObjectName> found = mbs.queryNames( quoted, null );
            if ( found.isEmpty() ) {
                ObjectName unquoted = new ObjectName( "javax.cache:type=CacheStatistics,CacheManager=*,Cache=" + name );
                found = mbs.queryNames( unquoted, null );
            }
            if ( !found.isEmpty() ) {
                ObjectName on = found.iterator().next();
                row.hits = longAttr( mbs, on, "CacheHits" );
                row.misses = longAttr( mbs, on, "CacheMisses" );
                row.gets = longAttr( mbs, on, "CacheGets" );
                row.puts = longAttr( mbs, on, "CachePuts" );
                row.removals = longAttr( mbs, on, "CacheRemovals" );
                row.evictions = longAttr( mbs, on, "CacheEvictions" );
                Object hitPct = mbs.getAttribute( on, "CacheHitPercentage" );
                if ( hitPct instanceof Number ) {
                    row.hitPercentage = ( ( Number ) hitPct ).floatValue();
                }
            }
        } catch ( Exception e ) {
            log.debug( "MBean stats unavailable for cache '" + name + "': " + e.getMessage() );
        }
        return row;
    }

    @Nullable
    private static Long longAttr( MBeanServer mbs, ObjectName on, String attr ) {
        try {
            Object v = mbs.getAttribute( on, attr );
            return v instanceof Number ? ( ( Number ) v ).longValue() : null;
        } catch ( Exception e ) {
            return null;
        }
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
                    @ApiResponse(responseCode = "204", description = "All caches cleared.")
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
                    @ApiResponse(responseCode = "204", description = "Cache cleared."),
                    @ApiResponse(responseCode = "404", description = "No cache registered under that name",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class)))
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
                    @ApiResponse(responseCode = "204", description = "Hibernate statistics reset.")
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

    /* ===== Batch GEO import ===== */

    /**
     * Maximum number of accessions accepted per batch — keeps a stray paste from spawning
     * thousands of jobs and saturating the in-memory task store.
     */
    static final int MAX_IMPORT_GEO_BATCH = 100;

    /**
     * Batch GEO accession import. Ports the bulk path of
     * {@code ubic.gemma.apps.LoadExpressionDataCli}: iterate the accession list and
     * submit one {@link ExpressionExperimentLoadTaskCommand} per accession, returning
     * the resulting task-id list so the caller can poll each one through
     * {@code /tasks/{taskId}}.
     *
     * <p>The optional flags on the request body (loadPlatformOnly, suppressMatching, etc.)
     * are applied to every accession in the batch. For one-off imports use
     * {@code POST /datasets/import}.</p>
     */
    @POST
    @Path("/tasks/import-geo")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Submit a batch of GEO accessions for async import",
            description = "Iterates `accessions` and submits one async load task per accession (port of "
                    + "`LoadExpressionDataCli`'s bulk path). Optional flags are applied uniformly across the batch. "
                    + "Returns 202 with the list of submitted task IDs — poll each one via `/tasks/{taskId}`. "
                    + "Rejects an empty list or more than " + MAX_IMPORT_GEO_BATCH + " accessions with 400. "
                    + "Blank entries inside the list are silently skipped; if every entry is blank the request is rejected.",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = {
                    @ApiResponse(responseCode = "202",
                            content = @Content(schema = @Schema(implementation = ResponseDataObject.class))),
                    @ApiResponse(responseCode = "400", description = "Body missing, accession list empty, or batch over cap",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public Response importGeoBatch( @Nullable ImportGeoBatchRequest body ) {
        if ( body == null || body.accessions == null || body.accessions.isEmpty() ) {
            throw new BadRequestException( "Request body must include a non-empty `accessions` array." );
        }
        if ( body.accessions.size() > MAX_IMPORT_GEO_BATCH ) {
            throw new BadRequestException( "Batch size " + body.accessions.size()
                    + " exceeds the per-call cap of " + MAX_IMPORT_GEO_BATCH + "." );
        }
        // Trim+dedupe blanks; mirrors LoadExpressionDataCli's StringUtils.strip / isBlank skip.
        List<String> cleaned = new ArrayList<>( body.accessions.size() );
        for ( String a : body.accessions ) {
            if ( a == null ) continue;
            String trimmed = a.trim();
            if ( trimmed.isEmpty() ) continue;
            cleaned.add( trimmed );
        }
        if ( cleaned.isEmpty() ) {
            throw new BadRequestException( "`accessions` contained no non-blank entries." );
        }
        List<String> submittedJobIds = new ArrayList<>( cleaned.size() );
        for ( String accession : cleaned ) {
            ExpressionExperimentLoadTaskCommand cmd = new ExpressionExperimentLoadTaskCommand();
            cmd.setAccession( accession );
            if ( body.arrayDesignName != null ) cmd.setArrayDesignName( body.arrayDesignName );
            if ( body.loadPlatformOnly != null ) cmd.setLoadPlatformOnly( body.loadPlatformOnly );
            if ( body.suppressMatching != null ) cmd.setSuppressMatching( body.suppressMatching );
            if ( body.splitByPlatform != null ) cmd.setSplitByPlatform( body.splitByPlatform );
            if ( body.aggressiveQtRemoval != null ) cmd.setAggressiveQtRemoval( body.aggressiveQtRemoval );
            if ( body.allowSuperSeriesLoad != null ) cmd.setAllowSuperSeriesLoad( body.allowSuperSeriesLoad );
            if ( body.allowArrayExpressDesign != null ) cmd.setAllowArrayExpressDesign( body.allowArrayExpressDesign );
            if ( body.isArrayExpress != null ) cmd.setArrayExpress( body.isArrayExpress );
            submittedJobIds.add( taskRunningService.submitTaskCommand( cmd ) );
        }
        ImportGeoBatchResponse responseBody = new ImportGeoBatchResponse();
        responseBody.submittedJobIds = submittedJobIds;
        responseBody.count = submittedJobIds.size();
        return Response.status( Response.Status.ACCEPTED ).entity( respond( responseBody ) ).build();
    }

    /* ===== Multifunctionality recompute ===== */

    /**
     * Async port of {@code MultifunctionalityCli}: recompute per-gene multifunctionality
     * scores for a single taxon. Submits a {@link MultifunctionalityTaskCommand}; the caller
     * polls {@code /tasks/{taskId}} for completion.
     *
     * <p>Taxon identifier may be the common name (e.g. {@code human}), scientific name,
     * NCBI ID, or Gemma taxon ID — same shape as elsewhere in the REST API
     * ({@link TaxonArg#valueOf(String)}).</p>
     */
    @POST
    @Path("/tasks/multifunctionality")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Submit an async recompute of per-gene multifunctionality for one taxon",
            description = "Port of `MultifunctionalityCli`. Resolves `taxon` (common name, scientific name, NCBI ID, or Gemma taxon ID) and submits a single async task that calls `GeneMultifunctionalityPopulationService.updateMultifunctionality(taxon)`. Returns 202 with the submitted task ID; poll `/tasks/{taskId}` for progress.",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = {
                    @ApiResponse(responseCode = "202",
                            content = @Content(schema = @Schema(implementation = ResponseDataObject.class))),
                    @ApiResponse(responseCode = "400", description = "Missing or malformed taxon identifier",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "404", description = "No taxon matches the supplied identifier",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public Response submitMultifunctionalityRecompute( @QueryParam("taxon") TaxonArg<?> taxonArg ) {
        if ( taxonArg == null ) {
            throw new BadRequestException( "`taxon` query parameter is required." );
        }
        Taxon taxon = taxonArgService.getEntity( taxonArg );
        MultifunctionalityTaskCommand cmd = new MultifunctionalityTaskCommand( taxon );
        String jobId = taskRunningService.submitTaskCommand( cmd );
        MultifunctionalityRecomputeResponse body = new MultifunctionalityRecomputeResponse();
        body.submittedJobId = jobId;
        body.taxonId = taxon.getId();
        body.taxonName = taxon.getCommonName();
        return Response.status( Response.Status.ACCEPTED )
                .location( URI.create( "/tasks/" + jobId ) )
                .entity( respond( body ) )
                .build();
    }

    /* ===== Platform reports ===== */

    /**
     * Regenerate the cached report for ONE platform, synchronously.
     * <p>
     * The report holds the per-platform element / sequence / alignment / gene counts that
     * {@code GET /platforms} serves as {@code numberOfGenes} and {@code numberOfMappedElements}.
     * They are never computed per request — counting distinct genes for one large platform measures
     * ~1.7s against production — so they are read from a file that something has to write. On a
     * production node nothing does: the Quartz trigger that refreshes them monthly
     * ({@code SchedulerConfig.arrayDesignReportTrigger}) is gated on the {@code scheduler} profile,
     * which production does not run.
     * <p>
     * Synchronous because a single platform is a couple of seconds and the caller wants the new
     * numbers back. Use {@code POST /admin/tasks/platform-reports} for the whole corpus.
     */
    @POST
    @Path("/platforms/{platform}/report")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Regenerate the cached report for one platform",
            description = "Recomputes and rewrites the on-disk report backing `numberOfGenes` / `numberOfMappedElements` for a single platform, and returns the refreshed value object. Synchronous; takes a couple of seconds on a large platform.",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(implementation = ResponseDataObject.class))),
                    @ApiResponse(responseCode = "404", description = "No platform matches the supplied identifier",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public ResponseDataObject<ArrayDesignValueObject> regeneratePlatformReport(
            @PathParam("platform") PlatformArg<?> platformArg ) {
        ArrayDesign platform = platformArgService.getEntity( platformArg );
        return respond( arrayDesignReportService.generateArrayDesignReport( platform.getId() ) );
    }

    /**
     * Submit an async regeneration of the cached reports for EVERY platform.
     * <p>
     * The bulk counterpart of {@link #regeneratePlatformReport}; the corpus-wide run is far too long
     * to hold a request open. Mirrors the other admin task endpoints: returns 202 with the job id,
     * poll {@code /tasks/{taskId}}.
     */
    @POST
    @Path("/tasks/platform-reports")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Submit an async regeneration of every platform's cached report",
            description = "Port of the `updatePlatformReports` CLI. Submits a single async task that rewrites the on-disk report for every platform plus the all-platforms summary. Returns 202 with the submitted task ID; poll `/tasks/{taskId}` for progress.",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = {
                    @ApiResponse(responseCode = "202",
                            content = @Content(schema = @Schema(implementation = ResponseDataObject.class)))
            })
    public Response submitPlatformReportsRegeneration() {
        String jobId = taskRunningService.submitTaskCommand( new ArrayDesignReportTaskCommand( true ) );
        return Response.status( Response.Status.ACCEPTED )
                .location( URI.create( "/tasks/" + jobId ) )
                .entity( respond( Collections.singletonMap( "submittedJobId", jobId ) ) )
                .build();
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

    /**
     * Trigger a Hibernate Search 7 mass-reindex for one entity (or all of them).
     * <p>
     * Destructive: HS 7's mass-indexer purges the existing on-disk Lucene index
     * for the entity before rebuilding ({@code purgeAllOnStart(true)}). Runs
     * asynchronously on a background thread; this endpoint returns
     * {@code 202 Accepted} as soon as the work is queued. Use
     * {@link #getSearchIndices()} to monitor doc-count progress and the
     * {@code reindexStatus} field per entity.
     * <p>
     * Concurrent reindex requests are rejected with {@code 409 Conflict} — the
     * mass-indexer is single-flight per JVM so two parallel calls would purge
     * each other's just-written segments.
     *
     * @param entity user-facing entity name (one of {@code genes}, {@code datasets},
     *               {@code platforms}, {@code bibliographicReferences}, {@code probes},
     *               {@code sequences}, {@code datasetGroups}, {@code geneSets}); or
     *               omit to reindex all indexable roots sequentially.
     */
    @POST
    @Path("/search/indices")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Trigger a Hibernate Search 7 mass-reindex",
            description = "Kicks off a destructive rebuild of the on-disk Lucene index "
                    + "for one (or all) indexable entities. Returns 202 with a list of "
                    + "queued entities. Run via the legacy CLI (IndexGemmaCLI) too — this "
                    + "endpoint is the curation-UI / ops-script equivalent so an admin can "
                    + "rebuild without shelling onto the container.",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = {
                    @ApiResponse(responseCode = "202",
                            content = @Content(schema = @Schema(implementation = ResponseDataObject.class))),
                    @ApiResponse(responseCode = "400", description = "Unknown entity name.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "409", description = "Another reindex is already running.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public Response reindexSearchIndices(
            @QueryParam("entity") @Nullable String entity ) {
        List<Class<? extends Identifiable>> targets = new ArrayList<>();
        if ( entity == null || entity.trim().isEmpty() || "all".equalsIgnoreCase( entity.trim() ) ) {
            targets.addAll( INDEXABLE_ENTITIES.values() );
        } else {
            Class<? extends Identifiable> cls = INDEXABLE_ENTITIES.get( entity.trim() );
            if ( cls == null ) {
                throw new BadRequestException( "Unknown entity '" + entity + "'. Supported: " + INDEXABLE_ENTITIES.keySet() );
            }
            targets.add( cls );
        }
        if ( !reindexInProgress.compareAndSet( false, true ) ) {
            throw new ClientErrorException( "Another reindex is already running. Wait for it to finish, "
                    + "then re-issue this request. Status is exposed via GET /admin/search/indices.",
                    Response.Status.CONFLICT );
        }
        // Capture for the worker thread; copy out so the closure doesn't retain the request scope.
        final List<Class<? extends Identifiable>> work = new ArrayList<>( targets );
        for ( Class<? extends Identifiable> c : work ) {
            reindexStatus.put( c.getSimpleName(), "queued" );
        }
        Thread.startVirtualThread( () -> {
            try {
                for ( Class<? extends Identifiable> c : work ) {
                    reindexStatus.put( c.getSimpleName(), "running" );
                    log.info( "/admin/search/indices: reindexing " + c.getSimpleName() + "…" );
                    try {
                        indexerService.index( c );
                        reindexStatus.put( c.getSimpleName(), "completed" );
                        log.info( "/admin/search/indices: " + c.getSimpleName() + " reindex complete." );
                    } catch ( RuntimeException e ) {
                        reindexStatus.put( c.getSimpleName(), "failed: " + e.getClass().getSimpleName() + " — " + e.getMessage() );
                        log.error( "/admin/search/indices: " + c.getSimpleName() + " reindex failed.", e );
                    }
                }
            } finally {
                reindexInProgress.set( false );
            }
        } );
        List<String> queued = new ArrayList<>( work.size() );
        for ( Class<? extends Identifiable> c : work ) queued.add( c.getSimpleName() );
        return Response.accepted( respond( new ReindexAcceptedResponse( queued ) ) ).build();
    }

    /** Wire shape for {@link #reindexSearchIndices}: list of entity classes whose reindex was queued. */
    @lombok.Value
    public static class ReindexAcceptedResponse {
        List<String> queued;
    }

    /* ===== JVM / OS snapshot ===== */

    /**
     * Process-level memory / GC / thread / load snapshot. Complements the anonymous
     * {@code /info} endpoint (build + JVM identity + OS identity) with the live, admin-only
     * resource numbers the legacy {@code systemStats.jsp} hand-rolled. Single read; no
     * historical series — that's what {@code /metrics} is for.
     */
    @GET
    @Path("/system")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "JVM and OS resource snapshot",
            description = "Returns a single-read snapshot of process resources: heap and non-heap memory (used/committed/max bytes), thread counts (live, daemon, peak), uptime, OS name/version/arch, processor count, and 1-minute system load average. For historical series use `/metrics` (Prometheus).",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(implementation = ResponseDataObject.class)))
            })
    public ResponseDataObject<SystemSnapshotResponse> getSystem() {
        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = mem.getHeapMemoryUsage();
        MemoryUsage nonHeap = mem.getNonHeapMemoryUsage();
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        RuntimeMXBean rt = ManagementFactory.getRuntimeMXBean();
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();

        SystemSnapshotResponse body = new SystemSnapshotResponse();
        body.heap = new MemoryBlock( heap.getUsed(), heap.getCommitted(), heap.getMax() );
        body.nonHeap = new MemoryBlock( nonHeap.getUsed(), nonHeap.getCommitted(), nonHeap.getMax() );
        body.threads = new ThreadBlock( threads.getThreadCount(), threads.getDaemonThreadCount(), threads.getPeakThreadCount() );
        body.startTimeMillis = rt.getStartTime();
        body.uptimeMillis = rt.getUptime();
        body.osName = os.getName();
        body.osVersion = os.getVersion();
        body.osArch = os.getArch();
        body.availableProcessors = os.getAvailableProcessors();
        // -1 on platforms where the JVM can't read load average (e.g. some Windows builds)
        body.systemLoadAverage = os.getSystemLoadAverage();
        return respond( body );
    }

    /* ===== Authenticated sessions ===== */

    /**
     * Authenticated session listing. The legacy {@code activeUsers.jsp} surfaced a count
     * via {@code SecurityController.getAuthenticatedUserCount} and a JSP comment promising
     * a table of users that was never built. This endpoint delivers that table: distinct
     * authenticated principals (across both browser and basic-auth callers), each with
     * the count of currently-tracked sessions, the most recent request time, and any
     * granted GROUP_* authorities.
     */
    @GET
    @Path("/sessions")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "List authenticated users with active sessions",
            description = "Enumerates every principal registered with the Spring Security `SessionRegistry`. For each principal returns the username, the count of currently-tracked sessions (excluding expired), the timestamp of the most recent request across those sessions, and the list of granted authorities when the principal is a `UserDetails`. Anonymous sessions are not included — only authenticated ones are tracked.",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(implementation = ResponseDataObject.class)))
            })
    public ResponseDataObject<SessionsResponse> getSessions() {
        List<Object> principals = sessionRegistry.getAllPrincipals();
        List<SessionPrincipalValueObject> rows = new ArrayList<>( principals.size() );
        int totalActiveSessions = 0;
        for ( Object principal : principals ) {
            // includeExpiredSessions=false: we want a live picture, not a forensic one
            List<SessionInformation> sessions = sessionRegistry.getAllSessions( principal, false );
            if ( sessions.isEmpty() ) {
                continue;
            }
            SessionPrincipalValueObject vo = new SessionPrincipalValueObject();
            vo.username = principalUsername( principal );
            vo.sessionCount = sessions.size();
            totalActiveSessions += sessions.size();
            Date mostRecent = null;
            for ( SessionInformation s : sessions ) {
                Date r = s.getLastRequest();
                if ( r != null && ( mostRecent == null || r.after( mostRecent ) ) ) {
                    mostRecent = r;
                }
            }
            vo.lastRequest = mostRecent;
            if ( principal instanceof UserDetails ) {
                List<String> auths = new ArrayList<>();
                ( (UserDetails) principal ).getAuthorities()
                        .forEach( a -> auths.add( a.getAuthority() ) );
                auths.sort( Comparator.naturalOrder() );
                vo.authorities = auths;
            }
            rows.add( vo );
        }
        // newest activity first; principals with no last-request time sort last
        rows.sort( Comparator.comparing( ( SessionPrincipalValueObject v ) -> v.lastRequest,
                Comparator.nullsLast( Comparator.reverseOrder() ) ) );
        SessionsResponse body = new SessionsResponse();
        body.authenticatedUserCount = rows.size();
        body.activeSessionCount = totalActiveSessions;
        body.principals = rows;
        return respond( body );
    }

    private static String principalUsername( Object principal ) {
        if ( principal instanceof UserDetails ) {
            return ( (UserDetails) principal ).getUsername();
        }
        return principal.toString();
    }

    /* ===== Loaded ontologies ===== */

    /**
     * Per-ontology load status. Enumerates every {@code OntologyService} bean (Mondo, PATO,
     * CHEBI, Uberon, CellType, the unified TDB, etc.) and reports each one's enable / load
     * / initialization-thread state plus its inference and search settings. Term counts
     * are skipped by default because {@code getAllURIs()} can be expensive on large
     * ontologies; opt in with {@code ?includeTermCount=true}.
     */
    @GET
    @Path("/ontologies")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Loaded-ontology status snapshot",
            description = "Enumerates every OntologyService bean and reports its stable `identifier` (the handle to pass to the refresh / rebuild-slim endpoints), the full set of `acceptedNames` those endpoints match on, and enabled / loaded / initialization-thread state, inference mode, language level, search-enabled flag, and process-imports flag. Term counts are not included by default (`getAllURIs()` traverses the in-memory model); pass `?includeTermCount=true` to include them. If a bean throws while being inspected the error message is captured in the row's `error` field and inspection of the remaining ontologies continues.",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(implementation = ResponseDataObject.class)))
            })
    public ResponseDataObject<OntologiesResponse> getOntologies(
            @QueryParam("includeTermCount") @DefaultValue("false") boolean includeTermCount ) {
        List<OntologyStatusValueObject> rows = new ArrayList<>( ontologies.size() );
        int enabled = 0, loaded = 0, initializing = 0;
        for ( OntologyService o : ontologies ) {
            OntologyStatusValueObject vo = inspect( o, includeTermCount );
            rows.add( vo );
            if ( Boolean.TRUE.equals( vo.enabled ) ) enabled++;
            if ( Boolean.TRUE.equals( vo.loaded ) ) loaded++;
            if ( Boolean.TRUE.equals( vo.initializing ) ) initializing++;
        }
        // sort on identifier, not dc:title — the title is null for the ontologies that don't declare one
        rows.sort( Comparator.comparing( (OntologyStatusValueObject v) -> v.identifier,
                Comparator.nullsLast( String.CASE_INSENSITIVE_ORDER ) ) );
        OntologiesResponse body = new OntologiesResponse();
        body.count = rows.size();
        body.enabledCount = enabled;
        body.loadedCount = loaded;
        body.initializingCount = initializing;
        body.ontologies = rows;
        return respond( body );
    }

    /**
     * Refresh a single ontology in-process: re-run {@code initialize(forceLoad=true)} on a
     * background thread so the source is re-fetched, the model is rebuilt, and the in-memory
     * state is atomically swapped without a container restart. Returns 202 immediately; the
     * caller polls {@link #getOntologies(boolean)} to watch the {@code initializing} flag
     * flip back to false.
     *
     * <p>Matches the ontology through {@link OntologyServiceResolver}, which accepts the well-known
     * abbreviation (CLO, HPO, TGEMO, …), the {@link OntologyService#getIdentifier() identifier}, the
     * implementing class name, or the {@code dc:title}, ignoring case and punctuation. Every ontology
     * is therefore refreshable, including the ones whose {@code dc:title} is absent or contains spaces.
     * 404 if no bean matches, 409 if a refresh is already in flight on that bean.
     *
     * <p>For the slim-CHEBI path the refresh re-runs the {@code loadModel} override, which
     * checks the seed-hash sidecar and re-extracts the slim if the corpus has drifted.
     */
    @POST
    @Path("/ontologies/{name}/refresh")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Refresh a single ontology in-process",
            description = "Kicks off an asynchronous re-initialization of the named ontology. The name may be the ontology's well-known abbreviation (`CLO`, `HPO`, `TGEMO`, ...), its identifier (`cellLineOntology`), its class name, or its `dc:title`; matching ignores case and punctuation, and every accepted spelling is listed as `acceptedNames` by `GET /admin/ontologies`. The currently-loaded model keeps serving reads until the new model is built and atomically swapped in. Use the per-ontology load-status endpoint to watch progress. 404 if no bean matches the given name; 409 if a refresh on that ontology is already running.",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = {
                    @ApiResponse(responseCode = "202", description = "Refresh accepted; the initialization thread is now running.",
                            content = @Content(schema = @Schema(implementation = ResponseDataObject.class))),
                    @ApiResponse(responseCode = "404", description = "No ontology with that name.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "409", description = "A refresh is already in progress for that ontology.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public Response refreshOntology(
            @PathParam("name") String name,
            @QueryParam("forceIndexing") @DefaultValue("false") boolean forceIndexing ) {
        OntologyService match = OntologyServiceResolver.resolve( ontologies, name )
                .orElseThrow( () -> new NotFoundException( "No ontology found with name=" + name
                        + ". Accepted names are listed as `acceptedNames` by GET /admin/ontologies." ) );
        if ( match.isInitializationThreadAlive() ) {
            throw new ClientErrorException(
                    "Refresh already in progress for ontology=" + name, Response.Status.CONFLICT );
        }
        log.info( "Hot-refresh requested for ontology=" + OntologyServiceResolver.getPreferredName( match )
                + " (requested as '" + name + "', forceIndexing=" + forceIndexing + ")" );
        // forceLoad=true is the whole point — we want the cached source re-validated and the
        // model rebuilt. forceIndexing defaults to false so we don't blow away a still-valid
        // Lucene index unless the caller explicitly asks.
        match.startInitializationThread( true, forceIndexing );
        // Reloading the Jena model + Lucene index alone is not enough — three layers cache
        // ontology-derived data and all of them must be invalidated for newly-added terms (e.g.
        // a fresh TGEMO_00210) to surface to clients:
        //
        //   1. OntologyCache: findTerm / getParents / getChildren entries keyed by ontology
        //      service. Mirrors OntologyServiceImpl.reinitializeAndReindexAllOntologies.
        //   2. AnnotationsSearchResponseCache: the /annotations/search response payload, keyed
        //      by (query, strategy, limit, prefixes, ...). Each cached hit carries baked-in
        //      matchedVia / matchedText / definition / parents fields that were computed
        //      against the prior model — a hit that resolved with matchedVia=null because the
        //      term wasn't yet loaded stays null until this cache rotates (5min TTL) or is
        //      flushed. No per-ontology key exists, so the safest move is to drop the whole
        //      region. Refreshes are infrequent enough that the recomputation cost is fine.
        //
        // Wait for the init thread on a daemon (endpoint still returns 202 immediately) and
        // evict in order: ontology-keyed caches first, then the response payload region.
        final OntologyService refreshed = match;
        Thread invalidator = new Thread( () -> {
            try {
                refreshed.waitForInitializationThread();
            } catch ( InterruptedException e ) {
                Thread.currentThread().interrupt();
                return;
            }
            try {
                ontologyFacade.clearCachesForOntology( refreshed );
                int evictedRegions = clearOntologyDerivedResponseCaches();
                log.info( "Hot-refresh of ontology=" + name + " completed; OntologyCache evicted; "
                        + evictedRegions + " response-cache region(s) flushed." );
            } catch ( RuntimeException e ) {
                log.warn( "Failed to evict caches for ontology=" + name + " after refresh; "
                        + "stale lookups may persist until next bounce.", e );
            }
        }, name + "_cache_evict" );
        invalidator.setDaemon( true );
        invalidator.start();
        return Response.accepted( respond( new OntologyRefreshResponse( name, "refreshing" ) ) ).build();
    }

    /**
     * Names of REST-level response caches whose entries are derived from ontology state and
     * must therefore be flushed when any ontology reloads. Today only the annotation-search
     * response payload qualifies — add new region names here if future endpoints introduce
     * similarly-shaped per-query caches.
     */
    private static final String[] ONTOLOGY_DERIVED_RESPONSE_CACHE_REGIONS = {
            "AnnotationsSearchResponseCache",
    };

    /**
     * Flush the response-level caches that bake ontology-derived fields into per-query payloads.
     * Returns the count of regions actually present and flushed (regions absent from the
     * CacheManager are silently skipped, which is fine — the cache simply isn't registered yet
     * on this build). Called from the refresh daemon after the ontology's lower-level
     * OntologyCache has been evicted.
     */
    private int clearOntologyDerivedResponseCaches() {
        int evicted = 0;
        for ( String region : ONTOLOGY_DERIVED_RESPONSE_CACHE_REGIONS ) {
            org.springframework.cache.Cache c = cacheManager.getCache( region );
            if ( c != null ) {
                c.clear();
                evicted++;
            }
        }
        return evicted;
    }

    /** Body shape for {@link #refreshOntology(String, boolean)} returns. */
    @Schema(name = "OntologyRefreshResponse")
    public static class OntologyRefreshResponse {
        public String name;
        public String status;

        public OntologyRefreshResponse() {
        }

        public OntologyRefreshResponse( String name, String status ) {
            this.name = name;
            this.status = status;
        }
    }

    /**
     * Rebuild the slim-cache OWL for an ontology that supports it (currently CHEBI only).
     * The service must already be loaded so the extractor can read the on-disk source.
     * Returns 202 immediately and the extraction runs on a daemon thread; poll
     * {@link #getOntologies(boolean)} to watch the result land
     * (a fresh slim file at {@code ${ontology.cache.dir}/ontology/chebiOntology-slim.owl}).
     *
     * <p>Memory note: STAR module extraction via OWL-API holds the full CHEBI in heap
     * during the run (~3 GB peak after this commit's source-release fix). Invoke on a
     * host with that headroom, and not during another resource-intensive operation.
     */
    @POST
    @Path("/ontologies/{name}/rebuild-slim")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Rebuild the slim-cache for an ontology",
            description = "Currently only CHEBI and MONDO are supported (other ontologies return 404). Accepts the same name spellings as the refresh endpoint. Kicks off the slim extraction asynchronously and returns 202. The service must already be loaded. 409 if a rebuild is already in flight.",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = {
                    @ApiResponse(responseCode = "202", description = "Rebuild started.",
                            content = @Content(schema = @Schema(implementation = ResponseDataObject.class))),
                    @ApiResponse(responseCode = "404", description = "Ontology not found or doesn't support slim rebuild.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "409", description = "Rebuild already in progress.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "503", description = "Ontology is not loaded yet.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public Response rebuildOntologySlim( @PathParam("name") String name ) {
        // Resolve the path argument the same way the refresh endpoint does (abbreviation / identifier /
        // class name / dc:title), then require the bean to actually support slimming. Restricting the
        // candidate list up front keeps a non-slimmable ontology whose name happens to match from
        // stealing the resolution.
        List<OntologyService> slimmables = ontologies.stream()
                .filter( o -> o instanceof ubic.gemma.core.ontology.providers.SlimmableOntologyService )
                .collect( Collectors.toList() );
        ubic.gemma.core.ontology.providers.SlimmableOntologyService slimmable =
                ( ubic.gemma.core.ontology.providers.SlimmableOntologyService ) OntologyServiceResolver
                        .resolve( slimmables, name ).orElse( null );
        if ( slimmable == null ) {
            throw new NotFoundException( "No slimmable ontology found matching name=" + name
                    + " (try CHEBI or MONDO)." );
        }
        try {
            if ( !slimmable.triggerSlimRebuildAsync() ) {
                throw new ClientErrorException(
                        "Slim rebuild already in progress for ontology=" + name,
                        Response.Status.CONFLICT );
            }
        } catch ( IllegalStateException e ) {
            throw new jakarta.ws.rs.ServiceUnavailableException( e.getMessage() );
        }
        log.info( "Slim rebuild kicked for ontology=" + name + " (" + slimmable.getClass().getSimpleName() + ")" );
        return Response.accepted( respond( new OntologyRefreshResponse( name, "rebuilding-slim" ) ) ).build();
    }


    private OntologyStatusValueObject inspect( OntologyService o, boolean includeTermCount ) {
        OntologyStatusValueObject vo = new OntologyStatusValueObject();
        vo.className = o.getClass().getSimpleName();
        // identifier + acceptedNames never depend on the model being loaded, so they are populated
        // outside the try: a bean that fails inspection is still refreshable by name.
        vo.identifier = OntologyServiceResolver.getPreferredName( o );
        vo.acceptedNames = new ArrayList<>( OntologyServiceResolver.getNames( o ) );
        try {
            vo.name = o.getName();
            vo.description = o.getDescription();
            vo.enabled = o.isEnabled();
            vo.loaded = o.isOntologyLoaded();
            vo.initializing = o.isInitializationThreadAlive();
            vo.initializationCancelled = o.isInitializationThreadCancelled();
            vo.inferenceMode = o.getInferenceMode() != null ? o.getInferenceMode().name() : null;
            vo.languageLevel = o.getLanguageLevel() != null ? o.getLanguageLevel().name() : null;
            vo.searchEnabled = o.isSearchEnabled();
            vo.processImports = o.getProcessImports();
            vo.slimmable = o instanceof ubic.gemma.core.ontology.providers.SlimmableOntologyService;
            if ( includeTermCount && Boolean.TRUE.equals( vo.loaded ) ) {
                try {
                    vo.termCount = (long) o.getAllURIs().size();
                } catch ( RuntimeException e ) {
                    log.warn( "Failed to count terms for " + vo.className + ": " + e.getMessage() );
                    vo.termCount = -1L;
                }
            }
        } catch ( RuntimeException e ) {
            log.warn( "Failed to inspect ontology bean " + vo.className + ": " + e.getMessage() );
            vo.error = e.getClass().getSimpleName() + ": " + e.getMessage();
        }
        return vo;
    }

    /* ===== Obsolete term usage ===== */

    /**
     * In-application port of {@code FindObsoleteTermsCli}: which obsolete ontology terms do Gemma's annotations
     * still use, and what does each owning ontology say should replace them.
     * <p>
     * The CLI existed because the check needed ontologies in memory and a CLI had to load them itself — which is
     * why it refuses to run unless {@code load.ontologies=false} and spends its first stretch warming up. A running
     * application already holds them, so the only work left here is one grouped query over CHARACTERISTIC plus a
     * lookup per distinct URI.
     * <p>
     * Read-only. Correcting the terms is a separate, deliberate action: see {@code autoCorrectable} on each row for
     * whether a correction could be derived from the ontology at all.
     */
    @GET
    @Path("/ontologies/obsolete-terms")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Report obsolete ontology terms still used by annotations",
            description = "In-application port of `FindObsoleteTermsCli`. Groups CHARACTERISTIC by term URI across "
                    + "the subject, predicate and object slots, checks each distinct URI against the loaded "
                    + "ontologies, and reports those that are obsolete along with the experiment count and the "
                    + "replacement the ontology asserts via `IAO:0100001`. Gene Ontology annotations are excluded, "
                    + "matching the CLI. Rows where `autoCorrectable` is true are ones whose replacement is asserted "
                    + "by the ontology and itself resolves and is current; everything else names why in "
                    + "`blockedReason` and needs a curator. Requires the ontologies to be loaded — with "
                    + "`load.ontologies=false` every term reads as unresolvable and the report comes back empty.",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "503", description = "Resolving terms exceeded the timeout; the ontologies are probably still loading.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<List<ObsoleteTermUsage>> getObsoleteTerms(
            @Parameter(description = "Budget in seconds for resolving terms against the loaded ontologies.")
            @QueryParam("timeoutSeconds") @DefaultValue("120") Integer timeoutSeconds
    ) {
        try {
            return respond( ontologyFacade.findObsoleteTermsInUse( timeoutSeconds, TimeUnit.SECONDS ) );
        } catch ( TimeoutException e ) {
            // Almost always "the ontologies have not finished loading", which is a state that passes rather than an
            // error in the request; 503 says come back, 500 would say something broke.
            throw new ServiceUnavailableException( "Timed out resolving terms against the loaded ontologies after "
                    + timeoutSeconds + "s; they may still be loading. Check /admin/ontologies." );
        }
    }

    /**
     * Rewrite annotations that use an obsolete ontology term to the successor its ontology asserts.
     * <p>
     * <b>Dry run unless {@code dryRun=false} is passed explicitly.</b> The default is the safe one because this
     * writes to production annotations, and a dry run returns the counts a live run would produce, so there is no
     * reason to skip the rehearsal.
     * <p>
     * Only {@code autoCorrectable} terms are touched — those whose replacement was derived from the ontology
     * rather than decided by a person. Terms offering only {@code oboInOwl:consider} candidates are never
     * corrected here; see {@code GET /admin/ontologies/obsolete-terms} for what they are and why.
     */
    @POST
    @Path("/ontologies/obsolete-terms/apply")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Correct annotations using obsolete ontology terms (dry run by default)",
            description = "Rewrites every slot the obsolete term occupies — category, value, predicate, object — to "
                    + "the successor asserted by `IAO:0100001` or by a merge record, and records the correction in "
                    + "each characteristic's `supportingEvidence` with an `assertedBy` naming the rule that derived "
                    + "it. Afterwards it rebuilds EE2C and ANNOTATION_RELATION for each affected experiment and "
                    + "writes one `AutomatedAnnotationEvent` per experiment.\n\n"
                    + "**Runs as a task**: returns 202 with a task id; poll `/tasks/{taskId}`.\n\n"
                    + "**`dryRun` defaults to true.** Pass `dryRun=false` to write.\n\n"
                    + "`uris` restricts the run to specific obsolete terms; omit it to take every auto-correctable "
                    + "term. Two terms are deferred and are skipped by a blanket run — EFO_0000408 (the `disease` "
                    + "category, ~7,600 experiments) and OBI_0003109 (single-nucleus, whose successor discards the "
                    + "nuclei/cells distinction) — naming one explicitly in `uris` overrides that.",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = {
                    @ApiResponse(responseCode = "202", description = "Correction task submitted.",
                            content = @Content(schema = @Schema(implementation = ResponseDataObject.class))) })
    public Response applyObsoleteTermCorrections(
            @Parameter(description = "Set false to actually write. Defaults to a dry run.")
            @QueryParam("dryRun") @DefaultValue("true") Boolean dryRun,
            @Parameter(description = "Restrict to these obsolete term URIs; omit for every auto-correctable term.")
            @QueryParam("uris") List<String> uris,
            @Parameter(description = "Budget in seconds for resolving terms against the loaded ontologies.")
            @QueryParam("timeoutSeconds") @DefaultValue("600") Integer timeoutSeconds
    ) {
        ObsoleteTermCorrectionTaskCommand cmd = new ObsoleteTermCorrectionTaskCommand(
                uris != null ? uris : Collections.emptyList(),
                !Boolean.FALSE.equals( dryRun ),
                timeoutSeconds );
        String jobId = taskRunningService.submitTaskCommand( cmd );
        ObsoleteTermCorrectionSubmission body = new ObsoleteTermCorrectionSubmission();
        body.submittedJobId = jobId;
        body.dryRun = !Boolean.FALSE.equals( dryRun );
        body.uris = uris != null ? uris : Collections.emptyList();
        return Response.status( Response.Status.ACCEPTED )
                .location( URI.create( "/tasks/" + jobId ) )
                .entity( respond( body ) )
                .build();
    }

    @Schema(description = "Accepted obsolete-term correction task.")
    public static class ObsoleteTermCorrectionSubmission {
        public String submittedJobId;
        @Schema(description = "True when the submitted run will write nothing.")
        public boolean dryRun;
        public List<String> uris;
    }

    /* ===== Database connection pool ===== */

    /**
     * HikariCP pool snapshot. Reports the live connection census plus the configured
     * upper bound, so the admin panel can show "12 / 50 active" at a glance and
     * surface "threads awaiting" when the pool is saturated.
     */
    @GET
    @Path("/db/pool")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Database connection pool snapshot",
            description = "Returns HikariCP pool stats: active / idle / total connections, threads currently awaiting a connection, plus the configured maximum pool size and connection-timeout. 503 if the configured DataSource is not a HikariDataSource.",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(implementation = ResponseDataObject.class))),
                    @ApiResponse(responseCode = "503", description = "Configured DataSource is not HikariCP",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public Response getDbPool() {
        if ( !( dataSource instanceof HikariDataSource ) ) {
            return Response.status( Response.Status.SERVICE_UNAVAILABLE )
                    .entity( "DataSource is not HikariCP (type=" + dataSource.getClass().getSimpleName() + ")" )
                    .build();
        }
        HikariDataSource hikari = (HikariDataSource) dataSource;
        HikariPoolMXBean mx = hikari.getHikariPoolMXBean();
        DbPoolResponse body = new DbPoolResponse();
        body.poolName = hikari.getPoolName();
        body.maximumPoolSize = hikari.getMaximumPoolSize();
        body.minimumIdle = hikari.getMinimumIdle();
        body.connectionTimeoutMillis = hikari.getConnectionTimeout();
        body.idleTimeoutMillis = hikari.getIdleTimeout();
        body.maxLifetimeMillis = hikari.getMaxLifetime();
        if ( mx != null ) {
            body.activeConnections = mx.getActiveConnections();
            body.idleConnections = mx.getIdleConnections();
            body.totalConnections = mx.getTotalConnections();
            body.threadsAwaitingConnection = mx.getThreadsAwaitingConnection();
        }
        return Response.ok( respond( body ) ).build();
    }

    /* ===== Curation-agents liveness ===== */

    /**
     * Out-of-process liveness probe for the gemma-curation-agents Python service.
     * Configured via {@code gemma.curationAgent.healthUrl} (unset = endpoint reports
     * "not configured" with 200, so the admin UI can render a neutral pill instead
     * of an alarming red one).
     */
    @GET
    @Path("/curation-agent/health")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Curation-agents service liveness probe",
            description = "Issues a GET against `gemma.curationAgent.healthUrl` with a `gemma.curationAgent.healthTimeoutMs`-millisecond timeout (default 3000). Returns `status` = UP / DOWN / NOT_CONFIGURED, latencyMillis, and either the upstream HTTP status code or the exception class name. Always returns HTTP 200 — even when DOWN — so the UI can poll without triggering error handlers.",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(implementation = ResponseDataObject.class)))
            })
    public ResponseDataObject<CurationAgentHealthResponse> getCurationAgentHealth() {
        CurationAgentHealthResponse body = new CurationAgentHealthResponse();
        body.url = curationAgentHealthUrl == null || curationAgentHealthUrl.trim().isEmpty() ? null : curationAgentHealthUrl;
        body.timeoutMillis = curationAgentHealthTimeoutMs;
        if ( body.url == null ) {
            body.status = "NOT_CONFIGURED";
            return respond( body );
        }
        long start = System.currentTimeMillis();
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout( Duration.ofMillis( curationAgentHealthTimeoutMs ) )
                    .build();
            HttpRequest req = HttpRequest.newBuilder( URI.create( body.url ) )
                    .timeout( Duration.ofMillis( curationAgentHealthTimeoutMs ) )
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send( req, HttpResponse.BodyHandlers.ofString() );
            body.latencyMillis = System.currentTimeMillis() - start;
            body.httpStatus = resp.statusCode();
            body.status = resp.statusCode() >= 200 && resp.statusCode() < 400 ? "UP" : "DOWN";
        } catch ( Exception e ) {
            body.latencyMillis = System.currentTimeMillis() - start;
            body.status = "DOWN";
            body.error = e.getClass().getSimpleName() + ": " + e.getMessage();
            log.debug( "curation-agent health probe failed: " + body.error );
        }
        return respond( body );
    }

    /* ===== GEO scrape (preview without import) ===== */

    /**
     * Scrape GEO record metadata by accession without importing into Gemma. Port of
     * {@code GeoGrabberCli}'s -e / --acc mode. Synchronous: NCBI E-utilities responses
     * are sub-second per accession in the typical case, so the curation-UI can call
     * this on-demand to preview a GEO record before triggering a full import.
     *
     * <p>Returns one {@link GeoRecordValueObject} per requested accession that GEO
     * successfully returns; accessions GEO doesn't know about are silently dropped
     * (matching the CLI's behavior).</p>
     */
    @POST
    @Path("/tasks/geo-grab")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Scrape GEO record metadata by accession",
            description = "Fetches GEO series metadata for the given accessions and returns it without importing into Gemma. Useful for previewing a GEO record from the curation-UI before triggering a full import. Synchronous; expect sub-second latency per accession. Uses the DETAILED retrieve preset (sub-series status, MeSH headings, library strategy, sample details, errors ignored).",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(implementation = ResponseDataObject.class))),
                    @ApiResponse(responseCode = "400", description = "Empty or missing accessions list",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "502", description = "GEO E-utilities request failed",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public ResponseDataObject<GeoGrabResponse> grabGeoRecords( GeoGrabRequest req ) {
        if ( req == null || req.accessions == null || req.accessions.isEmpty() ) {
            throw new BadRequestException( "accessions list is required and must be non-empty" );
        }
        List<String> cleaned = new ArrayList<>();
        for ( String a : req.accessions ) {
            if ( a != null && !a.trim().isEmpty() ) {
                cleaned.add( a.trim() );
            }
        }
        if ( cleaned.isEmpty() ) {
            throw new BadRequestException( "accessions list contains only blank entries" );
        }
        GeoBrowser browser = resolveGeoBrowser();
        Collection<GeoRecord> records;
        try {
            // DETAILED preset matches GeoGrabberCli.getDatasets — sub-series, MeSH,
            // library strategy, sample details. ignoreErrors=true on the preset so
            // a single bad sub-fetch doesn't poison the whole batch.
            records = browser.getGeoRecords( GeoRecordType.SERIES, cleaned, GeoRetrieveConfig.builder()
                    .subSeriesStatus( true )
                    .meshHeadings( true )
                    .libraryStrategy( true )
                    .sampleDetails( true )
                    .ignoreErrors( true )
                    .build() );
        } catch ( IOException e ) {
            log.warn( "GEO scrape failed for accessions=" + cleaned + ": " + e.getMessage() );
            throw new jakarta.ws.rs.ServerErrorException( "GEO E-utilities request failed: " + e.getMessage(),
                    Response.Status.BAD_GATEWAY );
        }
        List<GeoRecordValueObject> vos = new ArrayList<>( records.size() );
        for ( GeoRecord r : records ) {
            vos.add( toGeoRecordValueObject( r ) );
        }
        GeoGrabResponse body = new GeoGrabResponse();
        body.requestedCount = cleaned.size();
        body.returnedCount = vos.size();
        body.records = vos;
        return respond( body );
    }

    private synchronized GeoBrowser resolveGeoBrowser() {
        if ( geoBrowser == null ) {
            geoBrowser = new GeoBrowserImpl( ncbiApiKey == null ? "" : ncbiApiKey );
        }
        return geoBrowser;
    }

    /** Test seam: package-private setter so unit tests can inject a mock GeoBrowser. */
    void setGeoBrowser( GeoBrowser geoBrowser ) {
        this.geoBrowser = geoBrowser;
    }

    private static GeoRecordValueObject toGeoRecordValueObject( GeoRecord r ) {
        GeoRecordValueObject vo = new GeoRecordValueObject();
        vo.geoAccession = r.getGeoAccession();
        vo.title = r.getTitle();
        vo.summary = r.getSummary();
        vo.overallDesign = r.getOverallDesign();
        vo.organisms = r.getOrganisms() == null ? null : new ArrayList<>( r.getOrganisms() );
        vo.platform = r.getPlatform();
        vo.releaseDate = r.getReleaseDate();
        vo.seriesType = r.getSeriesType();
        vo.numSamples = r.getNumSamples();
        vo.subSeries = r.isSubSeries();
        vo.subSeriesOf = r.getSubSeriesOf();
        vo.superSeries = r.isSuperSeries();
        vo.pubMedIds = r.getPubMedIds() == null ? null : new ArrayList<>( r.getPubMedIds() );
        vo.meshHeadings = r.getMeshHeadings() == null ? null : new ArrayList<>( r.getMeshHeadings() );
        vo.libraryStrategy = r.getLibraryStrategy();
        vo.librarySource = r.getLibrarySource();
        vo.sampleDetails = r.getSampleDetails();
        vo.contactName = r.getContactName();
        return vo;
    }

    /* ===== GEO scrape & preboard pipeline ===== */

    /**
     * Submit a GEO scrape &amp; preboard run. Iterates recent GEO records via
     * {@link GeoScrapeService}, filters by taxon + matcher criteria, and
     * creates {@code PreboardedExperiment} rows for matches.
     *
     * <p>Two modes:</p>
     * <ul>
     * <li>{@code dryRun=false} (default) — async; returns 202 + submitted
     *     task ID. Poll {@code /tasks/{taskId}} for progress or
     *     {@code GET /admin/geo-scrape/last} for the lifecycle watermark.</li>
     * <li>{@code dryRun=true} — synchronous; matchers run but no
     *     watermark / preboarded / ticket rows are written. Returns 200
     *     with the candidate list inline. Used by gemma-curation-agents
     *     to evaluate curation methods locally without writing to prod
     *     gemd.</li>
     * </ul>
     */
    @POST
    @Path("/tasks/geo-scrape")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Submit a GEO scrape & preboard run (async, or sync dry-run)",
            description = "Iterates recent GEO records, filters to human/mouse/rat expression profiling, evaluates the registered matchers (subset selectable via `criteria`: {brain, scbrain, tfperturb}). With dryRun=false (default) creates PreboardedExperiment rows and returns 202 + async task ID. With dryRun=true evaluates only and returns 200 + the candidate list inline (no watermark, no preboarded rows, no ticket). "
                    + "DRY RUNS MUST BE KEPT SMALL: the sync branch is subject to the 60-second proxy timeout in front of this API, "
                    + "and the scrape grows superlinearly because every Entrez call passes through a global rate gate "
                    + "(333 ms between calls without an NCBI API key, 100 ms with one). Measured 2026-08-12 against live: "
                    + "50 records 6s, 100 records 22s, 150 records 37s, 200 records 502 Proxy Error at 60s, and the 1000 default "
                    + "cannot complete synchronously at all. Keep dryRun batches at or under ~100 records and walk a backlog by "
                    + "moving the `since`/`until` window — NOT by repeating `maxRecords`, which always restarts from record 0. "
                    + "dryRun=false is unaffected: it is already async and returns immediately.",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "dryRun=true — candidates inline.",
                            content = @Content(schema = @Schema(implementation = ResponseDataObject.class))),
                    @ApiResponse(responseCode = "202",
                            description = "dryRun=false — async submission.",
                            content = @Content(schema = @Schema(implementation = ResponseDataObject.class)))
            })
    public Response submitGeoScrape( @Nullable GeoScrapeRequest body ) {
        boolean dryRun = body != null && body.dryRun != null && body.dryRun;
        if ( dryRun ) {
            GeoScrapeService.ScrapeRequest req = new GeoScrapeService.ScrapeRequest();
            req.setSince( body.since );
            req.setUntil( body.until );
            req.setMaxRecords( body.maxRecords );
            req.setCriteria( body.criteria );
            req.setStartAt( body.startAt );
            req.setSkip( body.skip );
            req.setDryRun( true );
            GeoScrapeService.DryRunResult result;
            try {
                result = geoScrapeService.scrapeDryRun( req );
            } catch ( IllegalArgumentException e ) {
                // Unresolvable `startAt`. No global IllegalArgumentException mapper exists, so wrap
                // here or the caller gets a 500 for what is a bad request.
                throw new BadRequestException( e.getMessage(), e );
            }
            // `data` stays the candidate array it has always been -- the scan cursor and the
            // degraded-record list ride alongside it, so existing clients keep parsing unchanged.
            GeoScrapeDryRunResponse dryRunResponse = new GeoScrapeDryRunResponse();
            dryRunResponse.data = result.getCandidates();
            dryRunResponse.lastScannedAccession = result.getLastScannedAccession();
            dryRunResponse.lastScannedDate = result.getLastScannedDate();
            dryRunResponse.incompleteRecords = result.getIncompleteRecords();
            dryRunResponse.nextOffset = result.getNextOffset();
            return Response.ok( dryRunResponse ).build();
        }
        GeoScrapeTaskCommand cmd = new GeoScrapeTaskCommand();
        if ( body != null ) {
            cmd.setSince( body.since );
            cmd.setUntil( body.until );
            cmd.setMaxRecords( body.maxRecords );
            cmd.setCriteria( body.criteria );
            cmd.setStartAt( body.startAt );
            cmd.setSkip( body.skip );
            cmd.setDryRun( false );
        }
        String jobId = taskRunningService.submitTaskCommand( cmd );
        GeoScrapeSubmitResponse responseBody = new GeoScrapeSubmitResponse();
        responseBody.submittedJobId = jobId;
        return Response.status( Response.Status.ACCEPTED )
                .location( URI.create( "/tasks/" + jobId ) )
                .entity( respond( responseBody ) )
                .build();
    }

    /**
     * Latest {@link GeoScrapeWatermark} row. Returns 404 when no scrape has
     * been run.
     */
    @GET
    @Path("/geo-scrape/last")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Most recent GEO scrape watermark",
            description = "Returns the most recently created GeoScrapeWatermark row (IN_PROGRESS / COMPLETED / FAILED / CANCELLED), or 404 if no scrape has ever been run.",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(implementation = ResponseDataObject.class))),
                    @ApiResponse(responseCode = "404", description = "No scrape has been run.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public ResponseDataObject<GeoScrapeWatermarkValueObject> getLastGeoScrape() {
        GeoScrapeWatermark wm = geoScrapeService.getLastWatermark();
        if ( wm == null ) {
            throw new NotFoundException( "No GEO scrape has been run." );
        }
        return respond( toWatermarkVo( wm ) );
    }

    private static GeoScrapeWatermarkValueObject toWatermarkVo( GeoScrapeWatermark wm ) {
        GeoScrapeWatermarkValueObject vo = new GeoScrapeWatermarkValueObject();
        vo.id = wm.getId();
        vo.scannedAt = wm.getScannedAt();
        vo.scanFrom = wm.getScanFrom();
        vo.scanTo = wm.getScanTo();
        vo.recordsScanned = wm.getRecordsScanned();
        vo.recordsMatched = wm.getRecordsMatched();
        vo.criteriaApplied = wm.getCriteriaApplied();
        vo.status = wm.getStatus() == null ? null : wm.getStatus().name();
        vo.errorMessage = wm.getErrorMessage();
        return vo;
    }

    /* ===== Curation lifecycle status ===== */

    /**
     * Snapshot of the annotation-set -&gt; ticket lifecycle: per-role
     * {@link ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSet}
     * counts in the recent windows, open-ticket counts by {@link TicketType},
     * distinct agent run id count, and latest-createdAt timestamp.
     * <p>
     * Backs the curation-UI "what's the Python agent doing right now"
     * indicator. Counts are computed with bounded aggregates against the
     * ANNOTATION_SET and TICKET tables — no per-row fetch.
     */
    @GET
    @Path("/curation-status")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Annotation-set + ticket lifecycle snapshot",
            description = "Per-role breakdown of AnnotationSet rows (last 24h / 7d / by role) plus open-ticket counts by TicketType and the oldest open-ticket age. Read-only.",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(implementation = ResponseDataObject.class)))
            })
    public ResponseDataObject<CurationStatusResponse> getCurationStatus() {
        Date now = new Date();
        Date since24h = new Date( now.getTime() - TimeUnit.HOURS.toMillis( 24 ) );
        Date since7d = new Date( now.getTime() - TimeUnit.DAYS.toMillis( 7 ) );

        CurationStatusResponse body = new CurationStatusResponse();

        ProposalsBlock proposals = new ProposalsBlock();
        proposals.totalLast24h = annotationSetService.countSince( since24h, null );
        proposals.totalLast7d = annotationSetService.countSince( since7d, null );
        // byRole: lifetime breakdown of annotation sets by role
        // (PROPOSAL / DRAFT / SNAPSHOT).
        Map<AnnotationSetRole, Long> byRole = annotationSetService.countByRoleSince( null );
        Map<String, Long> byRoleWire = new LinkedHashMap<>( byRole.size() );
        for ( Map.Entry<AnnotationSetRole, Long> e : byRole.entrySet() ) {
            String key = e.getKey() != null ? e.getKey().getDbValue() : "null";
            byRoleWire.put( key, e.getValue() );
        }
        proposals.byRole = byRoleWire;
        body.proposals = proposals;

        TicketsBlock tickets = new TicketsBlock();
        Map<TicketType, Long> openByType = ticketService.countOpenByType();
        Map<String, Long> openByTypeWire = new LinkedHashMap<>( openByType.size() );
        for ( Map.Entry<TicketType, Long> e : openByType.entrySet() ) {
            String key = e.getKey() != null ? e.getKey().name() : "null";
            openByTypeWire.put( key, e.getValue() );
        }
        tickets.openCountByType = openByTypeWire;
        tickets.openCount = ticketService.countOpen();
        Date oldest = ticketService.findOldestOpenCreatedAt();
        tickets.oldestOpenAgeDays = oldest == null
                ? null
                : TimeUnit.MILLISECONDS.toDays( now.getTime() - oldest.getTime() );
        body.tickets = tickets;

        AgentRunsBlock runs = new AgentRunsBlock();
        // Distinct runIds in the 7d window across PROPOSAL rows (agent
        // emissions). Bounded; lifetime distinct count would scan the full
        // table on prod and isn't useful.
        runs.distinctRunIds = annotationSetService.countDistinctRunIdsSince(
                since7d, AnnotationSetRole.PROPOSAL );
        runs.lastRanAt = annotationSetService.findLatestCreatedAt( AnnotationSetRole.PROPOSAL );
        body.agentRuns = runs;

        return respond( body );
    }

    /* ===== User management (read-only listing) ===== */

    /**
     * Admin user listing. Soft-deleted users (DELETED_AT IS NOT NULL) are hidden
     * by default; pass {@code ?includeDeleted=true} to surface them.
     */
    @GET
    @Path("/users")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "List all users",
            description = "Returns every User row with their username, email, enabled flag, granted group authorities, signup-token-pending flag, soft-delete timestamps. Soft-deleted users are excluded unless `includeDeleted=true`. Sorted alphabetically by username.",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(implementation = ResponseDataObject.class)))
            })
    public ResponseDataObject<UsersListResponse> getUsers(
            @QueryParam("includeDeleted") @DefaultValue("false") boolean includeDeleted ) {
        Collection<User> users = userManager.loadAll();
        List<UserValueObject> rows = new ArrayList<>( users.size() );
        int enabled = 0, pending = 0, deleted = 0;
        for ( User u : users ) {
            boolean isDeleted = u.getDeletedAt() != null;
            if ( isDeleted ) deleted++;
            if ( isDeleted && !includeDeleted ) continue;
            rows.add( toUserValueObject( u ) );
            if ( u.isEnabled() ) enabled++;
            if ( u.getSignupToken() != null && !u.isEnabled() && !isDeleted ) pending++;
        }
        rows.sort( Comparator.comparing( ( UserValueObject v ) -> v.username,
                Comparator.nullsLast( String.CASE_INSENSITIVE_ORDER ) ) );
        UsersListResponse body = new UsersListResponse();
        body.total = rows.size();
        body.enabledCount = enabled;
        body.pendingSignupCount = pending;
        body.deletedCount = deleted;
        body.users = rows;
        return respond( body );
    }

    private UserValueObject toUserValueObject( User u ) {
        UserValueObject vo = new UserValueObject();
        vo.username = u.getUserName();
        vo.email = u.getEmail();
        vo.enabled = u.isEnabled();
        vo.signupTokenPending = u.getSignupToken() != null && !u.isEnabled() && u.getDeletedAt() == null;
        vo.signupTokenDate = u.getSignupTokenDatestamp();
        vo.deletedAt = u.getDeletedAt();
        vo.deletedBy = u.getDeletedBy();
        if ( u.getGroups() != null ) {
            List<String> groupNames = new ArrayList<>();
            for ( UserGroup g : u.getGroups() ) {
                groupNames.add( g.getName() );
            }
            groupNames.sort( Comparator.naturalOrder() );
            vo.groups = groupNames;
            vo.isAdmin = groupNames.contains( AuthorityConstants.ADMIN_GROUP_NAME );
        }
        return vo;
    }

    /**
     * Create a new active user with a server-generated one-time temporary password.
     * The plaintext password is returned in the response body — pass it to the new
     * user out-of-band. It is not stored anywhere recoverable; if lost, an admin
     * must reset it.
     */
    @POST
    @Path("/users")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Create a user with a temporary password",
            description = "Creates an immediately-enabled user (no email confirmation flow), generates a 16-character secure-random temp password, optionally grants admin role, and returns the plaintext temp password in the response body. The password appears in the response exactly once — store it elsewhere before navigating away. Returns 409 if username or email already exists.",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = {
                    @ApiResponse(responseCode = "201",
                            content = @Content(schema = @Schema(implementation = ResponseDataObject.class))),
                    @ApiResponse(responseCode = "400", description = "Missing or malformed username/email",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "409", description = "Username or email already taken",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public Response createUser( CreateUserRequest req ) {
        if ( req == null || req.username == null || req.username.trim().isEmpty() ) {
            throw new BadRequestException( "username is required" );
        }
        if ( req.email == null || req.email.trim().isEmpty() ) {
            throw new BadRequestException( "email is required" );
        }
        String username = req.username.trim();
        String email = req.email.trim();
        if ( userManager.userExists( username ) ) {
            throw new ClientErrorException( "username '" + username + "' is already taken", Response.Status.CONFLICT );
        }
        if ( userManager.userWithEmailExists( email ) ) {
            throw new ClientErrorException( "email '" + email + "' is already taken", Response.Status.CONFLICT );
        }
        String tempPassword = RandomStringUtils.secureStrong().nextAlphanumeric( 16 );
        // enabled=true, no signup token — admin-created users skip email confirm.
        UserDetailsImpl details = new UserDetailsImpl( tempPassword, username, true, null, email, null, null );
        userManager.createUser( details );
        if ( req.isAdmin ) {
            userManager.addUserToGroup( username, AuthorityConstants.ADMIN_GROUP_NAME );
        }
        User created = userManager.findByUserName( username );
        CreateUserResponse body = new CreateUserResponse();
        body.user = toUserValueObject( created );
        body.temporaryPassword = tempPassword;
        body.warning = "Pass this temporary password to the user out-of-band. It is not stored anywhere recoverable.";
        return Response.status( Response.Status.CREATED ).entity( respond( body ) ).build();
    }

    /**
     * Partial update — toggle the enabled flag (lock/unlock) and/or admin role.
     * Other User fields (email, password, name) are not touched by this endpoint;
     * those go through the user-profile flow.
     */
    @PATCH
    @Path("/users/{username}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Toggle a user's enabled flag and/or admin role",
            description = "Body fields are optional; only the supplied fields are applied. `enabled=false` locks the account (the user can't log in). `isAdmin=true` adds the user to the Administrators group; `isAdmin=false` removes them. 404 if the username doesn't exist; 400 if the body is empty.",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(implementation = ResponseDataObject.class))),
                    @ApiResponse(responseCode = "400", description = "Empty body",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "404", description = "No user with that username",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public ResponseDataObject<UserValueObject> patchUser( @PathParam("username") String username, UpdateUserRequest req ) {
        if ( req == null || ( req.enabled == null && req.isAdmin == null ) ) {
            throw new BadRequestException( "request body must specify at least one of: enabled, isAdmin" );
        }
        User u = userManager.findByUserName( username );
        if ( u == null ) {
            throw new NotFoundException( "No user with name=" + username );
        }
        if ( u.getDeletedAt() != null ) {
            throw new ClientErrorException( "user '" + username + "' is soft-deleted; restore is not supported via this endpoint",
                    Response.Status.CONFLICT );
        }
        if ( req.enabled != null && req.enabled != u.isEnabled() ) {
            // Round-trip through UserDetailsImpl so UserManagerImpl.updateUser
            // sees the right fields. Keep email + password unchanged.
            UserDetailsImpl ud = new UserDetailsImpl( u.getPassword(), u.getUserName(), req.enabled, null,
                    u.getEmail(), u.getSignupToken(), u.getSignupTokenDatestamp() );
            userManager.updateUser( ud );
        }
        if ( req.isAdmin != null ) {
            boolean currentlyAdmin = false;
            for ( UserGroup g : u.getGroups() ) {
                if ( AuthorityConstants.ADMIN_GROUP_NAME.equals( g.getName() ) ) {
                    currentlyAdmin = true;
                    break;
                }
            }
            if ( req.isAdmin && !currentlyAdmin ) {
                userManager.addUserToGroup( username, AuthorityConstants.ADMIN_GROUP_NAME );
            } else if ( !req.isAdmin && currentlyAdmin ) {
                userManager.removeUserFromGroup( username, AuthorityConstants.ADMIN_GROUP_NAME );
            }
        }
        User refreshed = userManager.findByUserName( username );
        return respond( toUserValueObject( refreshed ) );
    }

    /**
     * Administrative password reset — set a user's password to a fresh server-generated
     * one-time temporary password. Does not require the user's current password (this is
     * the recovery path for a locked-out or forgetful user). The plaintext temp password
     * is returned once; pass it to the user out-of-band. The user should then change it via
     * the self-service {@code PUT /users/me/password} flow. Leaves the account enabled;
     * distinct from the email-confirmation reset flow.
     */
    @POST
    @Path("/users/{username}/password")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Reset a user's password to a temporary password",
            description = "Generates a 16-character secure-random temp password, sets it (encoded) as the user's password, and returns the plaintext once. The password appears in the response exactly once — store it elsewhere before navigating away. 404 if the username doesn't exist; 409 if the user is soft-deleted.",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(implementation = ResponseDataObject.class))),
                    @ApiResponse(responseCode = "404", description = "No user with that username",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "409", description = "User is soft-deleted",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public ResponseDataObject<ResetPasswordResponse> resetUserPassword( @PathParam("username") String username ) {
        User u = userManager.findByUserName( username );
        if ( u == null ) {
            throw new NotFoundException( "No user with name=" + username );
        }
        if ( u.getDeletedAt() != null ) {
            throw new ClientErrorException( "user '" + username + "' is soft-deleted; restore it before resetting the password",
                    Response.Status.CONFLICT );
        }
        String tempPassword = RandomStringUtils.secureStrong().nextAlphanumeric( 16 );
        userManager.adminChangePassword( username, tempPassword );
        ResetPasswordResponse body = new ResetPasswordResponse();
        body.temporaryPassword = tempPassword;
        body.warning = "Pass this temporary password to the user out-of-band. It is not stored anywhere recoverable; the user should change it via /users/me/password.";
        return respond( body );
    }

    /**
     * Soft delete — marks the account as deleted, disables it, and preserves
     * the row so dependent references (ACL sids, audit-event authorship FKs)
     * don't dangle. Hard delete is intentionally not exposed via REST.
     */
    @DELETE
    @Path("/users/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Soft-delete a user",
            description = "Sets deletedAt = now, deletedBy = the current admin's username, and enabled = false. The User row stays in the database — hard delete via REST is forbidden because ACL sids and audit-event authorship FKs reference the user. Idempotent: a second DELETE on an already-deleted user returns 204.",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = {
                    @ApiResponse(responseCode = "204", description = "User soft-deleted (or already was)."),
                    @ApiResponse(responseCode = "404", description = "No user with that username",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public Response deleteUser( @PathParam("username") String username ) {
        User u = userManager.findByUserName( username );
        if ( u == null ) {
            throw new NotFoundException( "No user with name=" + username );
        }
        String issuingAdmin = currentUsername();
        if ( username.equals( issuingAdmin ) ) {
            throw new ClientErrorException( "an admin cannot soft-delete their own account", Response.Status.CONFLICT );
        }
        userManager.softDeleteUser( username, issuingAdmin );
        return Response.noContent().build();
    }

    private String currentUsername() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if ( principal instanceof org.springframework.security.core.userdetails.UserDetails ) {
                return ( (org.springframework.security.core.userdetails.UserDetails) principal ).getUsername();
            }
            return principal != null ? principal.toString() : "unknown";
        } catch ( RuntimeException e ) {
            return "unknown";
        }
    }

    /* ===== Blacklist ===== */

    /**
     * Adds a single blacklist entry. Port of the {@code -accession}/{@code -reason} arm of
     * {@code BlacklistCli.doAuthenticatedWork()}: validates the accession, looks up the GEO
     * {@link ExternalDatabase}, and creates either a {@link BlacklistedPlatform} (GPL*) or
     * {@link BlacklistedExperiment} (GSE*) row with the supplied reason.
     */
    @POST
    @Path("/blacklist")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Add a blacklist entry",
            description = "Creates a blacklist row for the given GEO accession (GPL* → BlacklistedPlatform; GSE* → BlacklistedExperiment). "
                    + "Synchronous: the row is persisted before the response returns. Returns 201 with the new entry's value object. "
                    + "Returns 400 when accession or reason is missing, or when the accession prefix is neither GPL nor GSE. "
                    + "Returns 409 when an entry for that accession already exists (to update the reason, delete the entry and re-add it).",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = {
                    @ApiResponse(responseCode = "201",
                            content = @Content(schema = @Schema(implementation = ResponseDataObject.class))),
                    @ApiResponse(responseCode = "400", description = "Body missing, accession blank, reason blank, or unrecognised prefix",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "409", description = "Accession is already blacklisted",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public Response addBlacklistEntry( @Nullable BlacklistRequest body ) {
        if ( body == null || body.accession == null || body.accession.trim().isEmpty() ) {
            throw new BadRequestException( "`accession` is required" );
        }
        if ( body.reason == null || body.reason.trim().isEmpty() ) {
            throw new BadRequestException( "`reason` is required" );
        }
        String accession = body.accession.trim();
        String reason = body.reason.trim();

        if ( blacklistedEntityService.findByAccession( accession ) != null ) {
            throw new ClientErrorException( accession + " is already blacklisted. To update the reason, delete the entry and re-add it.",
                    Response.Status.CONFLICT );
        }

        BlacklistedEntity entity;
        if ( accession.startsWith( "GPL" ) ) {
            entity = new BlacklistedPlatform();
        } else if ( accession.startsWith( "GSE" ) ) {
            entity = new BlacklistedExperiment();
        } else {
            throw new BadRequestException( "Unrecognised accession prefix for '" + accession
                    + "': expected something starting with GPL or GSE." );
        }

        ExternalDatabase geo = externalDatabaseReadService.findByName( ExternalDatabases.GEO );
        if ( geo == null ) {
            throw new IllegalStateException( "GEO not found as an external database in the system" );
        }

        entity.setShortName( accession );
        entity.setReason( reason );
        DatabaseEntry d = DatabaseEntry.Factory.newInstance( accession, geo );
        entity.setExternalAccession( d );

        BlacklistedEntity created = blacklistedEntityService.create( entity );
        return Response.status( Response.Status.CREATED )
                .entity( respond( BlacklistedValueObject.fromEntity( created ) ) )
                .build();
    }

    /**
     * Removes a blacklist entry by its accession. Port of the {@code -accession -undo} arm
     * of {@code BlacklistCli}. Returns 204 on success, 404 when the accession is not on the
     * blacklist.
     */
    @DELETE
    @Path("/blacklist/{accession}")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Remove a blacklist entry",
            description = "Removes the blacklist row matching `{accession}`. Returns 204 on success, 404 when no entry exists.",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = {
                    @ApiResponse(responseCode = "204", description = "Entry removed."),
                    @ApiResponse(responseCode = "404", description = "No blacklist entry for that accession",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public Response deleteBlacklistEntry( @PathParam("accession") String accession ) {
        BlacklistedEntity entity = blacklistedEntityService.findByAccession( accession );
        if ( entity == null ) {
            throw new NotFoundException( "No blacklist entry for accession=" + accession );
        }
        blacklistedEntityService.remove( entity );
        return Response.noContent().build();
    }

    /**
     * Lists current blacklist entries. Convenience sibling for the curation-UI; the CLI has
     * no equivalent. The underlying service exposes only {@code loadAll()}, so pagination
     * is applied in-process: results are sorted by accession (alphabetic, nulls last) and
     * sliced by {@code offset}/{@code limit}.
     */
    @GET
    @Path("/blacklist")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "List blacklist entries",
            description = "Returns the current blacklist sorted by accession with `limit`/`offset` pagination applied in-process. "
                    + "`limit` defaults to 100 and is capped at 1000; `offset` defaults to 0. The response includes the total count of "
                    + "entries on the blacklist (independent of paging) so the UI can render a page navigator.",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(implementation = ResponseDataObject.class)))
            })
    public ResponseDataObject<BlacklistListResponse> listBlacklistEntries(
            @QueryParam("limit") @DefaultValue("100") int limit,
            @QueryParam("offset") @DefaultValue("0") int offset ) {
        if ( limit < 0 ) {
            throw new BadRequestException( "`limit` must be non-negative" );
        }
        if ( offset < 0 ) {
            throw new BadRequestException( "`offset` must be non-negative" );
        }
        int effectiveLimit = Math.min( limit, MAX_BLACKLIST_PAGE_SIZE );

        Collection<BlacklistedEntity> all = blacklistedEntityService.loadAll();
        List<BlacklistedEntity> sorted = new ArrayList<>( all );
        sorted.sort( Comparator.comparing( ( BlacklistedEntity e ) -> {
            DatabaseEntry de = e.getExternalAccession();
            return de != null ? de.getAccession() : e.getShortName();
        }, Comparator.nullsLast( Comparator.naturalOrder() ) ) );

        List<BlacklistedValueObject> page;
        if ( offset >= sorted.size() || effectiveLimit == 0 ) {
            page = Collections.emptyList();
        } else {
            int end = Math.min( offset + effectiveLimit, sorted.size() );
            page = new ArrayList<>( end - offset );
            for ( BlacklistedEntity e : sorted.subList( offset, end ) ) {
                page.add( BlacklistedValueObject.fromEntity( e ) );
            }
        }

        BlacklistListResponse body = new BlacklistListResponse();
        body.total = sorted.size();
        body.limit = effectiveLimit;
        body.offset = offset;
        body.count = page.size();
        body.entries = page;
        return respond( body );
    }

    /** Upper bound on per-page entries returned by {@link #listBlacklistEntries(int, int)}. */
    static final int MAX_BLACKLIST_PAGE_SIZE = 1000;

    /* ===== DTOs ===== */

    public static class ImportGeoBatchRequest {
        /** GEO (or ArrayExpress) accessions to import. Must be non-empty and at most {@value #MAX_IMPORT_GEO_BATCH}. */
        public List<String> accessions;
        @Nullable
        public String arrayDesignName;
        @Nullable
        public Boolean loadPlatformOnly;
        @Nullable
        public Boolean suppressMatching;
        @Nullable
        public Boolean splitByPlatform;
        @Nullable
        public Boolean aggressiveQtRemoval;
        @Nullable
        public Boolean allowSuperSeriesLoad;
        @Nullable
        public Boolean allowArrayExpressDesign;
        @Nullable
        public Boolean isArrayExpress;
    }

    public static class ImportGeoBatchResponse {
        /** Number of tasks actually submitted (post-blank-skip). */
        public int count;
        /** Submitted task IDs in the same order as the cleaned accession list. Poll each at `/tasks/{taskId}`. */
        public List<String> submittedJobIds;
    }

    public static class MultifunctionalityRecomputeResponse {
        /** Submitted task ID. Poll at `/tasks/{taskId}`. */
        public String submittedJobId;
        /** Resolved Gemma taxon ID. */
        @Nullable
        public Long taxonId;
        /** Resolved taxon common name (e.g. "human"). */
        @Nullable
        public String taxonName;
    }

    public static class CacheListResponse {
        public int count;
        /** Names only — preserved for backward compat with pre-stats clients. */
        public List<String> names;
        /** Per-cache stats roll-up; same ordering as {@link #names}. Stat fields are
         *  null on caches whose JCache stats MBean is missing (non-JCache or stats
         *  disabled). Drives the admin Systems Monitoring Caches table. */
        public List<CacheStatRow> caches;
    }

    public static class CacheStatRow {
        public String name;
        @Nullable
        public Long hits;
        @Nullable
        public Long misses;
        /** Total gets — hits + misses; some providers report it directly. */
        @Nullable
        public Long gets;
        @Nullable
        public Long puts;
        @Nullable
        public Long removals;
        @Nullable
        public Long evictions;
        /** Percentage in [0, 100]; null if stats unavailable or never queried. */
        @Nullable
        public Float hitPercentage;
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

    public static class SystemSnapshotResponse {
        public MemoryBlock heap;
        public MemoryBlock nonHeap;
        public ThreadBlock threads;
        public long startTimeMillis;
        public long uptimeMillis;
        public String osName;
        public String osVersion;
        public String osArch;
        public int availableProcessors;
        /** -1.0 on platforms where the load average is unavailable (e.g. some Windows JVMs). */
        public double systemLoadAverage;
    }

    public static class MemoryBlock {
        public long usedBytes;
        public long committedBytes;
        /** -1 when the JVM does not define a maximum (e.g. non-heap on some collectors). */
        public long maxBytes;

        public MemoryBlock() {
        }

        public MemoryBlock( long usedBytes, long committedBytes, long maxBytes ) {
            this.usedBytes = usedBytes;
            this.committedBytes = committedBytes;
            this.maxBytes = maxBytes;
        }
    }

    public static class ThreadBlock {
        public int liveCount;
        public int daemonCount;
        public int peakCount;

        public ThreadBlock() {
        }

        public ThreadBlock( int liveCount, int daemonCount, int peakCount ) {
            this.liveCount = liveCount;
            this.daemonCount = daemonCount;
            this.peakCount = peakCount;
        }
    }

    public static class SessionsResponse {
        /** Distinct authenticated principals currently tracked by Spring Security's SessionRegistry. */
        public int authenticatedUserCount;
        /** Sum of non-expired sessions across all principals. */
        public int activeSessionCount;
        public List<SessionPrincipalValueObject> principals;
    }

    public static class SessionPrincipalValueObject {
        public String username;
        public int sessionCount;
        /** Most recent {@code SessionInformation#getLastRequest()} across this principal's sessions. */
        @Nullable
        public Date lastRequest;
        /** Granted authorities, alphabetically sorted; null when the principal isn't a UserDetails. */
        @Nullable
        public List<String> authorities;
    }

    public static class DbPoolResponse {
        public String poolName;
        public int maximumPoolSize;
        public int minimumIdle;
        public long connectionTimeoutMillis;
        public long idleTimeoutMillis;
        public long maxLifetimeMillis;
        public int activeConnections;
        public int idleConnections;
        public int totalConnections;
        public int threadsAwaitingConnection;
    }

    public static class CurationAgentHealthResponse {
        /** UP, DOWN, or NOT_CONFIGURED. */
        public String status;
        @Nullable
        public String url;
        public int timeoutMillis;
        public long latencyMillis;
        /** HTTP status code from the upstream service; 0 when the request didn't complete. */
        @Nullable
        public Integer httpStatus;
        /** Populated when the probe threw (timeout, connection refused, malformed URL, etc.). */
        @Nullable
        public String error;
    }

    public static class UsersListResponse {
        public int total;
        public int enabledCount;
        /** Users with a signup token outstanding AND not yet enabled (unverified). */
        public int pendingSignupCount;
        /** Total count of soft-deleted users (regardless of whether they're included in the list). */
        public int deletedCount;
        public List<UserValueObject> users;
    }

    public static class UserValueObject {
        public String username;
        @Nullable
        public String email;
        public boolean enabled;
        public boolean isAdmin;
        @Nullable
        public List<String> groups;
        /** True when a signup token is present and the user is not yet enabled — never-completed signup. */
        public boolean signupTokenPending;
        @Nullable
        public Date signupTokenDate;
        /** Non-null when the user has been soft-deleted. */
        @Nullable
        public Date deletedAt;
        @Nullable
        public String deletedBy;
    }

    public static class CreateUserRequest {
        public String username;
        public String email;
        /** Optional; defaults to false. If true the user is added to the Administrators group. */
        public boolean isAdmin;
    }

    public static class CreateUserResponse {
        public UserValueObject user;
        /** Server-generated 16-char temp password. Shown exactly once — copy it before navigating away. */
        public String temporaryPassword;
        public String warning;
    }

    public static class UpdateUserRequest {
        @Nullable
        public Boolean enabled;
        @Nullable
        public Boolean isAdmin;
    }

    public static class ResetPasswordResponse {
        /** Server-generated 16-char temp password. Shown exactly once — copy it before navigating away. */
        public String temporaryPassword;
        public String warning;
    }

    public static class OntologiesResponse {
        public int count;
        public int enabledCount;
        public int loadedCount;
        public int initializingCount;
        public List<OntologyStatusValueObject> ontologies;
    }

    public static class OntologyStatusValueObject {
        /** Java simple class name (always available, even if inspection fails). */
        public String className;
        /**
         * Stable, space-free handle for this ontology — its well-known abbreviation when it has one
         * (CLO, HPO, TGEMO, …), otherwise its cache name. Always available, even before the ontology
         * is loaded, and always usable as the {@code {name}} path argument of the refresh and
         * rebuild-slim endpoints. This is what a client should display and send back.
         */
        public String identifier;
        /** Every spelling the refresh / rebuild-slim endpoints accept for this ontology. */
        public List<String> acceptedNames;
        /** The ontology's {@code dc:title}; null when it doesn't declare one or isn't loaded yet. */
        @Nullable
        public String name;
        @Nullable
        public String description;
        @Nullable
        public Boolean enabled;
        @Nullable
        public Boolean loaded;
        /** True while the background load thread is alive. */
        @Nullable
        public Boolean initializing;
        @Nullable
        public Boolean initializationCancelled;
        @Nullable
        public String inferenceMode;
        @Nullable
        public String languageLevel;
        @Nullable
        public Boolean searchEnabled;
        @Nullable
        public Boolean processImports;
        /** Populated only when the request specifies `includeTermCount=true` AND the ontology is loaded. -1 on query failure. */
        @Nullable
        public Long termCount;
        /** True if the underlying bean implements {@code SlimmableOntologyService} — i.e.
         *  it supports the {@code /admin/ontologies/{name}/rebuild-slim} endpoint. Drives
         *  the admin UI's per-row "rebuild slim" action button. */
        @Nullable
        public Boolean slimmable;
        /** Set if inspection threw; the rest of the row will be partially populated. */
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

    /**
     * Response shape for {@link #getCurationStatus()}. The wire field names
     * mirror the JSON contract documented on the endpoint.
     */
    public static class CurationStatusResponse {
        public ProposalsBlock proposals;
        public TicketsBlock tickets;
        public AgentRunsBlock agentRuns;
    }

    public static class ProposalsBlock {
        public long totalLast24h;
        public long totalLast7d;
        /** Lifetime per-role counts (proposal / draft / snapshot). */
        public Map<String, Long> byRole;
    }

    public static class TicketsBlock {
        /** Open-ticket counts keyed by {@link TicketType#name()}. */
        public Map<String, Long> openCountByType;
        public long openCount;
        /** Days since the oldest non-terminal ticket was created; null when no open tickets exist. */
        @Nullable
        public Long oldestOpenAgeDays;
    }

    public static class GeoGrabRequest {
        /** GEO series accessions (e.g. {@code GSE12345}) to fetch metadata for. */
        public List<String> accessions;
    }

    public static class GeoGrabResponse {
        /** Number of accessions in the request after trimming blanks. */
        public int requestedCount;
        /** Number of GEO records actually returned by NCBI (may be less than requestedCount). */
        public int returnedCount;
        public List<GeoRecordValueObject> records;
    }

    public static class GeoRecordValueObject {
        public String geoAccession;
        @Nullable
        public String title;
        @Nullable
        public String summary;
        @Nullable
        public String overallDesign;
        @Nullable
        public List<String> organisms;
        /** Semicolon-delimited platform accessions (e.g. {@code GPL570;GPL96}). */
        @Nullable
        public String platform;
        @Nullable
        public Date releaseDate;
        @Nullable
        public String seriesType;
        public int numSamples;
        public boolean subSeries;
        @Nullable
        public String subSeriesOf;
        public boolean superSeries;
        @Nullable
        public List<String> pubMedIds;
        @Nullable
        public List<String> meshHeadings;
        @Nullable
        public String libraryStrategy;
        @Nullable
        public String librarySource;
        @Nullable
        public String sampleDetails;
        @Nullable
        public String contactName;
    }

    public static class AgentRunsBlock {
        /** Distinct {@code runId}s seen on AnnotationSet rows with role=PROPOSAL in the last 7 days. */
        public long distinctRunIds;
        /** Most recent {@code createdAt} across PROPOSAL-role AnnotationSet rows; null if none. */
        @Nullable
        public Date lastRanAt;
    }

    public static class BlacklistRequest {
        /** GEO accession (GPL* for platform; GSE* for experiment). Required. */
        public String accession;
        /** Reason for blacklisting. Required and non-blank. */
        public String reason;
    }

    public static class GeoScrapeRequest {
        /** Lower bound of the scrape window. Null means "resume from last successful scrape's scanTo". */
        @Nullable
        public Date since;
        /** Upper bound of the scrape window (publication date inclusive). Null means "today". */
        @Nullable
        public Date until;
        /**
         * Hard cap on number of GEO records examined, counted from the HEAD of the result set.
         * Null means the service default (1000).
         * <p>
         * 🛑 This is a cap, NOT a page size — there is no cursor or offset on this request, and the
         * scrape restarts at record 0 on every call. Two calls with maxRecords=50 return the same
         * 50 records; a client looping on it re-scans the same head forever and never advances.
         * Verified 2026-08-12 against live: the 25-record run's candidates are a strict prefix of
         * the 50's, which prefix the 100's, which prefix the 150's.
         * <p>
         * To walk a backlog, window with {@link #since} / {@link #until} instead — those are the
         * only parameters that move.
         */
        @Nullable
        public Integer maxRecords;
        /** Subset of matcher names to apply (e.g. {@code ["brain","tfperturb"]}); null/empty = all available. */
        @Nullable
        public Collection<String> criteria;
        /** If true, evaluate matches but do not persist any PreboardedExperiment rows. */
        @Nullable
        public Boolean dryRun;
        /**
         * GEO series accession to resume from, e.g. {@code "GSE342847"} — the last record you
         * processed. Its release date becomes the upper bound of the window, so the scan picks up
         * where the previous batch stopped and walks backwards. This is the cursor to use for
         * batching; `maxRecords` is a head cap and cannot advance.
         * <p>
         * An accession rather than an offset because GEO returns newest-first: a numeric offset
         * shifts whenever a new series is published, so an offset-paging client silently skips
         * records. Series released the same day reappear — that overlap is intentional.
         * <p>
         * An explicit `until` wins over this. An accession that cannot be resolved is a 400, not a
         * silent fallback, because ignoring the cursor rescans from the newest record.
         */
        @Nullable
        public String startAt;
        /**
         * Records to skip at the start of the resolved window — record-level resumption.
         * `startAt` resolves to a release DATE and GEO's filter is day-granular, so resuming at X
         * re-scans X's whole day; when that day is wider than `maxRecords` the scan cannot advance
         * and stepping past the day discards whatever it never reached. Pass the previous
         * response's `nextOffset` here alongside the same `startAt` to continue at record level.
         */
        @Nullable
        public Integer skip;
    }

    /**
     * Dry-run response. {@code data} is the candidate array unchanged; the rest is what a batching
     * caller could not previously work out for itself.
     */
    public static class GeoScrapeDryRunResponse {
        /** The candidates, in scan order. Unchanged shape. */
        public List<GeoScrapeDryRunCandidate> data;
        /**
         * The last record the scan LOOKED at, matched or not — cursor on this rather than on the
         * oldest candidate. `maxRecords` caps records SCANNED while the batch counts candidates
         * RETURNED, so when a batch's matches sit near the head the next request re-scans the same
         * span for nothing: 38 of 101 requests bought nothing on a measured 2026-06-01..08-12 walk,
         * each a full synchronous scan against the 60-second proxy budget. Null if nothing was
         * examined.
         */
        @Nullable
        public String lastScannedAccession;
        /** Release date of `lastScannedAccession`, so `until` can be stepped without a lookup. */
        @Nullable
        public Date lastScannedDate;
        /**
         * Accessions examined on degraded information — GEO served unusable MINiML, so the record
         * was kept on its summary rather than failing the batch. Detail-dependent matchers may have
         * under-matched on these, so a caller can report its list as incomplete and name them.
         * Usually transient; worth retrying later. Empty when everything parsed.
         */
        public List<String> incompleteRecords;
        /**
         * Absolute offset into the resolved window where this scan stopped. Hand it back as `skip`
         * with the same `startAt` to resume at record level instead of restarting that day. Null
         * when nothing was scanned.
         */
        @Nullable
        public Integer nextOffset;
    }

    public static class GeoScrapeSubmitResponse {
        public String submittedJobId;
    }

    public static class GeoScrapeWatermarkValueObject {
        public Long id;
        public Date scannedAt;
        @Nullable
        public Date scanFrom;
        @Nullable
        public Date scanTo;
        public int recordsScanned;
        public int recordsMatched;
        @Nullable
        public String criteriaApplied;
        @Nullable
        public String status;
        @Nullable
        public String errorMessage;
    }

    public static class BlacklistListResponse {
        /** Total number of blacklist entries (independent of paging). */
        public int total;
        /** Effective limit applied (capped at {@value #MAX_BLACKLIST_PAGE_SIZE}). */
        public int limit;
        public int offset;
        /** Number of entries actually returned in {@code entries}. */
        public int count;
        public List<BlacklistedValueObject> entries;
    }
}
