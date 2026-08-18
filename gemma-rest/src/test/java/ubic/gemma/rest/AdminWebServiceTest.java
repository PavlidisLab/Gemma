/*
 * The Gemma project.
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

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import ubic.gemma.core.job.SubmittedTask;
import ubic.gemma.core.job.TaskRunningService;
import ubic.gemma.core.loader.expression.geo.model.GeoRecord;
import ubic.gemma.core.loader.expression.geo.service.GeoBrowser;
import ubic.gemma.core.loader.expression.geo.service.GeoRecordType;
import ubic.gemma.core.loader.expression.geo.service.GeoRetrieveConfig;
import ubic.gemma.core.geoscrape.GeoScrapeDryRunCandidate;
import ubic.gemma.core.geoscrape.GeoScrapeService;
import ubic.gemma.core.tasks.analysis.expression.ExpressionExperimentLoadTaskCommand;
import ubic.gemma.core.tasks.maintenance.GeoScrapeTaskCommand;
import ubic.gemma.core.tasks.maintenance.MultifunctionalityTaskCommand;
import ubic.gemma.model.expression.experiment.GeoScrapeWatermark;
import ubic.gemma.core.security.AuthorityConstants;
import ubic.gemma.core.security.authentication.UserDetailsImpl;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.model.blacklist.BlacklistedEntity;
import ubic.gemma.model.blacklist.BlacklistedExperiment;
import ubic.gemma.model.blacklist.BlacklistedPlatform;
import ubic.gemma.model.common.auditAndSecurity.UserGroup;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketType;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.persistence.service.blacklist.BlacklistedEntityService;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.TicketService;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseReadService;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetRole;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.AnnotationSetService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.args.TaxonArg;
import ubic.gemma.rest.util.args.TaxonArgService;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure-Mockito tests for {@link AdminWebService}: cache list / clear / clear-named,
 * Hibernate stats snapshot.
 */
@ExtendWith(MockitoExtension.class)
public class AdminWebServiceTest {

    @Mock
    private CacheManager cacheManager;
    @Mock
    private SessionFactory sessionFactory;
    @Mock
    private Statistics statistics;
    @Mock
    private Cache fooCache;
    @Mock
    private Cache barCache;
    @Mock
    private TaskRunningService taskRunningService;
    @Mock
    private SessionRegistry sessionRegistry;
    @Mock
    private DataSource dataSource;
    @Mock
    private UserManager userManager;
    @Mock
    private AnnotationSetService annotationSetService;
    @Mock
    private TicketService ticketService;
    @Mock
    private GeoBrowser geoBrowser;
    // TaxonArgService cannot be Mockito-mocked under JDK 25 + Mockito 5.21 because its
    // abstract-supertype generics confuse ByteBuddy's instrumentation. Use a real instance
    // built from mocked dependencies; tests stub through the inner TaxonService.
    private TaxonArgService taxonArgService;
    @Mock
    private BlacklistedEntityService blacklistedEntityService;
    @Mock
    private ExternalDatabaseReadService externalDatabaseReadService;
    @Mock
    private GeoScrapeService geoScrapeService;
    @Mock
    private ubic.gemma.core.search.indexer.IndexerService indexerService;
    @Mock
    private ubic.gemma.core.ontology.OntologyService ontologyFacade;

    private AdminWebService webService;

    @org.mockito.Mock
    private ubic.gemma.persistence.service.genome.taxon.TaxonService innerTaxonService;
    @org.mockito.Mock
    private ubic.gemma.persistence.service.genome.ChromosomeService innerChromosomeService;
    @org.mockito.Mock
    private ubic.gemma.persistence.service.genome.gene.GeneService innerGeneService;

    @BeforeEach
    public void setUp() {
        taxonArgService = new TaxonArgService( innerTaxonService, innerChromosomeService, innerGeneService );
        webService = new AdminWebService( cacheManager, sessionFactory, taskRunningService, sessionRegistry,
                Collections.emptyList(), ontologyFacade, dataSource, userManager, annotationSetService, ticketService,
                taxonArgService, blacklistedEntityService, externalDatabaseReadService, geoScrapeService,
                indexerService );
    }

    /* ===== /admin/caches ===== */

    @Test
    public void listCachesReturnsAlphabeticallySortedNames() {
        when( cacheManager.getCacheNames() ).thenReturn( Arrays.asList( "zzz", "abc", "foo" ) );

        ResponseDataObject<AdminWebService.CacheListResponse> resp = webService.getCaches();

        assertThat( resp.getData().count ).isEqualTo( 3 );
        assertThat( resp.getData().names ).containsExactly( "abc", "foo", "zzz" );
    }

    @Test
    public void clearAllCachesIteratesEveryCache() {
        when( cacheManager.getCacheNames() ).thenReturn( Arrays.asList( "foo", "bar" ) );
        when( cacheManager.getCache( "foo" ) ).thenReturn( fooCache );
        when( cacheManager.getCache( "bar" ) ).thenReturn( barCache );

        Response resp = webService.clearAllCaches();

        assertThat( resp.getStatus() ).isEqualTo( 204 );
        verify( fooCache ).clear();
        verify( barCache ).clear();
    }

    @Test
    public void clearAllCachesTolleratesMissingCache() {
        // CacheManager.getCache may return null even for a name listed in
        // getCacheNames() under race; the loop must not NPE.
        when( cacheManager.getCacheNames() ).thenReturn( Arrays.asList( "foo", "bar" ) );
        when( cacheManager.getCache( "foo" ) ).thenReturn( fooCache );
        when( cacheManager.getCache( "bar" ) ).thenReturn( null );

        Response resp = webService.clearAllCaches();

        assertThat( resp.getStatus() ).isEqualTo( 204 );
        verify( fooCache ).clear();
    }

    @Test
    public void clearNamedCacheReturns204() {
        when( cacheManager.getCache( "foo" ) ).thenReturn( fooCache );

        Response resp = webService.clearCache( "foo" );

        assertThat( resp.getStatus() ).isEqualTo( 204 );
        verify( fooCache ).clear();
    }

    @Test
    public void clearMissingCacheThrows404() {
        when( cacheManager.getCache( "nope" ) ).thenReturn( null );

        assertThatThrownBy( () -> webService.clearCache( "nope" ) )
                .isInstanceOf( NotFoundException.class );
        verify( fooCache, never() ).clear();
    }

    /* ===== /admin/hibernate/stats ===== */

    @Test
    public void hibernateStatsSnapshotMirrorsStatisticsFields() {
        when( sessionFactory.getStatistics() ).thenReturn( statistics );
        when( statistics.isStatisticsEnabled() ).thenReturn( true );
        when( statistics.getStartTime() ).thenReturn( 1_700_000_000_000L );
        when( statistics.getSessionOpenCount() ).thenReturn( 100L );
        when( statistics.getSessionCloseCount() ).thenReturn( 95L );
        when( statistics.getTransactionCount() ).thenReturn( 50L );
        when( statistics.getFlushCount() ).thenReturn( 40L );
        when( statistics.getPrepareStatementCount() ).thenReturn( 200L );
        when( statistics.getQueryExecutionCount() ).thenReturn( 150L );
        when( statistics.getQueryExecutionMaxTime() ).thenReturn( 1234L );
        when( statistics.getQueryExecutionMaxTimeQueryString() ).thenReturn( "select * from ee" );
        when( statistics.getQueryCacheHitCount() ).thenReturn( 11L );
        when( statistics.getQueryCacheMissCount() ).thenReturn( 22L );
        when( statistics.getQueryCachePutCount() ).thenReturn( 33L );
        when( statistics.getSecondLevelCacheHitCount() ).thenReturn( 44L );
        when( statistics.getSecondLevelCacheMissCount() ).thenReturn( 55L );
        when( statistics.getSecondLevelCachePutCount() ).thenReturn( 66L );

        ResponseDataObject<AdminWebService.HibernateStatsResponse> resp = webService.getHibernateStats();
        AdminWebService.HibernateStatsResponse body = resp.getData();

        assertThat( body.statisticsEnabled ).isTrue();
        assertThat( body.startTime.getTime() ).isEqualTo( 1_700_000_000_000L );
        assertThat( body.sessionOpenCount ).isEqualTo( 100L );
        assertThat( body.sessionCloseCount ).isEqualTo( 95L );
        assertThat( body.transactionCount ).isEqualTo( 50L );
        assertThat( body.flushCount ).isEqualTo( 40L );
        assertThat( body.prepareStatementCount ).isEqualTo( 200L );
        assertThat( body.queryExecutionCount ).isEqualTo( 150L );
        assertThat( body.queryExecutionMaxTimeMillis ).isEqualTo( 1234L );
        assertThat( body.queryExecutionMaxTimeQuery ).isEqualTo( "select * from ee" );
        assertThat( body.queryCacheHitCount ).isEqualTo( 11L );
        assertThat( body.queryCacheMissCount ).isEqualTo( 22L );
        assertThat( body.queryCachePutCount ).isEqualTo( 33L );
        assertThat( body.secondLevelCacheHitCount ).isEqualTo( 44L );
        assertThat( body.secondLevelCacheMissCount ).isEqualTo( 55L );
        assertThat( body.secondLevelCachePutCount ).isEqualTo( 66L );
    }

    /* ===== /admin/hibernate/reset ===== */

    @Test
    public void resetHibernateStatsClearsStatistics() {
        when( sessionFactory.getStatistics() ).thenReturn( statistics );

        Response resp = webService.resetHibernateStats();

        assertThat( resp.getStatus() ).isEqualTo( 204 );
        verify( statistics ).clear();
    }

    /* ===== /admin/jobs ===== */

    @Test
    public void getJobsReturnsEmptyListWhenNoTasks() {
        when( taskRunningService.getSubmittedTasks() ).thenReturn( Collections.emptyList() );

        ResponseDataObject<AdminWebService.JobsListResponse> resp = webService.getJobs();
        AdminWebService.JobsListResponse body = resp.getData();

        assertThat( body.total ).isZero();
        assertThat( body.queued ).isZero();
        assertThat( body.running ).isZero();
        assertThat( body.completed ).isZero();
        assertThat( body.failed ).isZero();
        assertThat( body.tasks ).isEmpty();
    }

    @Test
    public void getJobsAggregatesStatusCountsAndSortsNewestFirst() {
        SubmittedTask qTask = mockTask( "q-1", SubmittedTask.Status.QUEUED, new Date( 1_000L ) );
        SubmittedTask rTask = mockTask( "r-1", SubmittedTask.Status.RUNNING, new Date( 3_000L ) );
        SubmittedTask cTask = mockTask( "c-1", SubmittedTask.Status.COMPLETED, new Date( 2_000L ) );
        SubmittedTask fTask = mockTask( "f-1", SubmittedTask.Status.FAILED, new Date( 4_000L ) );
        when( taskRunningService.getSubmittedTasks() ).thenReturn( Arrays.asList( qTask, rTask, cTask, fTask ) );

        ResponseDataObject<AdminWebService.JobsListResponse> resp = webService.getJobs();
        AdminWebService.JobsListResponse body = resp.getData();

        assertThat( body.total ).isEqualTo( 4 );
        assertThat( body.queued ).isEqualTo( 1 );
        assertThat( body.running ).isEqualTo( 1 );
        assertThat( body.completed ).isEqualTo( 1 );
        assertThat( body.failed ).isEqualTo( 1 );
        assertThat( body.tasks ).extracting( TaskStatusValueObject::getTaskId )
                .containsExactly( "f-1", "r-1", "c-1", "q-1" );
    }

    /* ===== /admin/system ===== */

    @Test
    public void getSystemReturnsHeapNonHeapThreadsAndOsBlocks() {
        ResponseDataObject<AdminWebService.SystemSnapshotResponse> resp = webService.getSystem();
        AdminWebService.SystemSnapshotResponse body = resp.getData();

        // Heap/non-heap and thread MX beans are always present on the JVM;
        // the only assertion we can make is non-null and non-negative-ish.
        assertThat( body.heap ).isNotNull();
        assertThat( body.heap.usedBytes ).isGreaterThanOrEqualTo( 0L );
        assertThat( body.heap.committedBytes ).isGreaterThanOrEqualTo( 0L );
        assertThat( body.nonHeap ).isNotNull();
        assertThat( body.threads ).isNotNull();
        assertThat( body.threads.liveCount ).isPositive();
        assertThat( body.osName ).isNotBlank();
        assertThat( body.osArch ).isNotBlank();
        assertThat( body.availableProcessors ).isPositive();
        assertThat( body.uptimeMillis ).isGreaterThanOrEqualTo( 0L );
    }

    /* ===== /admin/sessions ===== */

    @Test
    public void getSessionsReturnsEmptyWhenNoPrincipals() {
        when( sessionRegistry.getAllPrincipals() ).thenReturn( Collections.emptyList() );

        ResponseDataObject<AdminWebService.SessionsResponse> resp = webService.getSessions();
        AdminWebService.SessionsResponse body = resp.getData();

        assertThat( body.authenticatedUserCount ).isZero();
        assertThat( body.activeSessionCount ).isZero();
        assertThat( body.principals ).isEmpty();
    }

    @Test
    public void getSessionsSkipsPrincipalsWithNoActiveSessions() {
        // SessionRegistry retains principals after all sessions expire; we filter those out.
        Object stalePrincipal = "ghost-user";
        when( sessionRegistry.getAllPrincipals() ).thenReturn( Collections.singletonList( stalePrincipal ) );
        when( sessionRegistry.getAllSessions( stalePrincipal, false ) )
                .thenReturn( Collections.emptyList() );

        ResponseDataObject<AdminWebService.SessionsResponse> resp = webService.getSessions();
        AdminWebService.SessionsResponse body = resp.getData();

        assertThat( body.authenticatedUserCount ).isZero();
        assertThat( body.activeSessionCount ).isZero();
        assertThat( body.principals ).isEmpty();
    }

    @Test
    public void getSessionsAggregatesUserDetailsAndStringPrincipals() {
        UserDetails alice = new User( "alice", "x",
                Arrays.asList( new SimpleGrantedAuthority( "GROUP_USER" ),
                        new SimpleGrantedAuthority( "GROUP_ADMIN" ) ) );
        Object bob = "bob"; // basic-auth principals can be plain strings
        when( sessionRegistry.getAllPrincipals() ).thenReturn( Arrays.asList( alice, bob ) );

        SessionInformation aliceS1 = sessionInfo( alice, "s-a1", new Date( 1_000L ) );
        SessionInformation aliceS2 = sessionInfo( alice, "s-a2", new Date( 5_000L ) );
        SessionInformation bobS1 = sessionInfo( bob, "s-b1", new Date( 3_000L ) );
        when( sessionRegistry.getAllSessions( alice, false ) ).thenReturn( Arrays.asList( aliceS1, aliceS2 ) );
        when( sessionRegistry.getAllSessions( bob, false ) ).thenReturn( Collections.singletonList( bobS1 ) );

        ResponseDataObject<AdminWebService.SessionsResponse> resp = webService.getSessions();
        AdminWebService.SessionsResponse body = resp.getData();

        assertThat( body.authenticatedUserCount ).isEqualTo( 2 );
        assertThat( body.activeSessionCount ).isEqualTo( 3 );
        // Most recent activity first: alice (last 5_000) ahead of bob (last 3_000)
        assertThat( body.principals ).extracting( vo -> vo.username )
                .containsExactly( "alice", "bob" );

        AdminWebService.SessionPrincipalValueObject aliceVo = body.principals.get( 0 );
        assertThat( aliceVo.sessionCount ).isEqualTo( 2 );
        assertThat( aliceVo.lastRequest ).isEqualTo( new Date( 5_000L ) );
        assertThat( aliceVo.authorities ).containsExactly( "GROUP_ADMIN", "GROUP_USER" );

        AdminWebService.SessionPrincipalValueObject bobVo = body.principals.get( 1 );
        assertThat( bobVo.sessionCount ).isEqualTo( 1 );
        assertThat( bobVo.lastRequest ).isEqualTo( new Date( 3_000L ) );
        assertThat( bobVo.authorities ).isNull();
    }

    private SessionInformation sessionInfo( Object principal, String sessionId, Date lastRequest ) {
        return new SessionInformation( principal, sessionId, lastRequest );
    }

    private SubmittedTask mockTask( String id, SubmittedTask.Status status, Date submittedAt ) {
        SubmittedTask t = org.mockito.Mockito.mock( SubmittedTask.class );
        when( t.getTaskId() ).thenReturn( id );
        when( t.getStatus() ).thenReturn( status );
        when( t.getSubmissionTime() ).thenReturn( submittedAt );
        return t;
    }

    /* ===== /admin/curation-status ===== */

    @Test
    public void getCurationStatusSnapshotComposesAnnotationSetAndTicketAggregates() {
        Date lastRan = new Date( 1_700_000_000_000L );
        when( annotationSetService.countSince( org.mockito.ArgumentMatchers.any( Date.class ),
                org.mockito.ArgumentMatchers.isNull() ) )
                .thenReturn( 5L )   // first call: 24h
                .thenReturn( 42L ); // second call: 7d
        Map<AnnotationSetRole, Long> byRole = new LinkedHashMap<>();
        byRole.put( AnnotationSetRole.PROPOSAL, 18L );
        byRole.put( AnnotationSetRole.DRAFT, 1003L );
        byRole.put( AnnotationSetRole.SNAPSHOT, 7L );
        when( annotationSetService.countByRoleSince( null ) ).thenReturn( byRole );
        when( annotationSetService.countDistinctRunIdsSince(
                org.mockito.ArgumentMatchers.any( Date.class ),
                eq( AnnotationSetRole.PROPOSAL ) ) )
                .thenReturn( 12L );
        when( annotationSetService.findLatestCreatedAt( AnnotationSetRole.PROPOSAL ) )
                .thenReturn( lastRan );

        Map<TicketType, Long> openByType = new LinkedHashMap<>();
        openByType.put( TicketType.BATCH_INFO_NEEDED, 14L );
        openByType.put( TicketType.QUALITY_REVIEW, 27L );
        when( ticketService.countOpenByType() ).thenReturn( openByType );
        when( ticketService.countOpen() ).thenReturn( 41L );
        Date now = new Date();
        Date fiveDaysAgo = new Date( now.getTime() - TimeUnit.DAYS.toMillis( 5 ) );
        when( ticketService.findOldestOpenCreatedAt() ).thenReturn( fiveDaysAgo );

        ResponseDataObject<AdminWebService.CurationStatusResponse> resp = webService.getCurationStatus();
        AdminWebService.CurationStatusResponse body = resp.getData();

        assertThat( body.proposals.totalLast24h ).isEqualTo( 5L );
        assertThat( body.proposals.totalLast7d ).isEqualTo( 42L );
        assertThat( body.proposals.byRole )
                .containsEntry( "proposal", 18L )
                .containsEntry( "draft", 1003L )
                .containsEntry( "snapshot", 7L );

        assertThat( body.tickets.openCount ).isEqualTo( 41L );
        assertThat( body.tickets.openCountByType )
                .containsEntry( "BATCH_INFO_NEEDED", 14L )
                .containsEntry( "QUALITY_REVIEW", 27L );
        assertThat( body.tickets.oldestOpenAgeDays ).isEqualTo( 5L );

        assertThat( body.agentRuns.distinctRunIds ).isEqualTo( 12L );
        assertThat( body.agentRuns.lastRanAt ).isEqualTo( lastRan );
    }

    @Test
    public void getCurationStatusEmptyTablesReturnsZeroes() {
        when( annotationSetService.countSince( org.mockito.ArgumentMatchers.any( Date.class ),
                org.mockito.ArgumentMatchers.isNull() ) ).thenReturn( 0L );
        when( annotationSetService.countByRoleSince( null ) ).thenReturn( Collections.emptyMap() );
        when( annotationSetService.countDistinctRunIdsSince(
                org.mockito.ArgumentMatchers.any( Date.class ),
                eq( AnnotationSetRole.PROPOSAL ) ) )
                .thenReturn( 0L );
        when( annotationSetService.findLatestCreatedAt( AnnotationSetRole.PROPOSAL ) ).thenReturn( null );
        when( ticketService.countOpenByType() ).thenReturn( Collections.emptyMap() );
        when( ticketService.countOpen() ).thenReturn( 0L );
        when( ticketService.findOldestOpenCreatedAt() ).thenReturn( null );

        ResponseDataObject<AdminWebService.CurationStatusResponse> resp = webService.getCurationStatus();
        AdminWebService.CurationStatusResponse body = resp.getData();

        assertThat( body.proposals.totalLast24h ).isZero();
        assertThat( body.proposals.totalLast7d ).isZero();
        assertThat( body.proposals.byRole ).isEmpty();
        assertThat( body.tickets.openCount ).isZero();
        assertThat( body.tickets.openCountByType ).isEmpty();
        assertThat( body.tickets.oldestOpenAgeDays ).isNull();
        assertThat( body.agentRuns.distinctRunIds ).isZero();
        assertThat( body.agentRuns.lastRanAt ).isNull();
    }

    /* ===== /admin/users (CRUD) ===== */

    @AfterEach
    public void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /** Build a minimally-populated User model (Gemma's User, not Spring's). */
    private ubic.gemma.model.common.auditAndSecurity.User gemmaUser( String username, boolean enabled, Date deletedAt,
            String... groupNames ) {
        ubic.gemma.model.common.auditAndSecurity.User u = new ubic.gemma.model.common.auditAndSecurity.User();
        u.setUserName( username );
        u.setEnabled( enabled );
        u.setEmail( username + "@example.org" );
        u.setDeletedAt( deletedAt );
        Set<UserGroup> groups = new HashSet<>();
        for ( String g : groupNames ) {
            UserGroup ug = new UserGroup();
            ug.setName( g );
            groups.add( ug );
        }
        u.setGroups( groups );
        return u;
    }

    @Test
    public void getUsers_excludesSoftDeleted_byDefault() {
        ubic.gemma.model.common.auditAndSecurity.User alice = gemmaUser( "alice", true, null );
        ubic.gemma.model.common.auditAndSecurity.User bob = gemmaUser( "bob", true, null );
        ubic.gemma.model.common.auditAndSecurity.User ghost = gemmaUser( "ghost", false, new Date() );
        when( userManager.loadAll() ).thenReturn( Arrays.asList( alice, bob, ghost ) );

        ResponseDataObject<AdminWebService.UsersListResponse> resp = webService.getUsers( false );
        AdminWebService.UsersListResponse body = resp.getData();

        assertThat( body.users ).hasSize( 2 );
        assertThat( body.users ).extracting( v -> v.username ).containsExactly( "alice", "bob" );
        assertThat( body.deletedCount ).isEqualTo( 1 );
    }

    @Test
    public void getUsers_includesSoftDeleted_whenFlagTrue() {
        ubic.gemma.model.common.auditAndSecurity.User alice = gemmaUser( "alice", true, null );
        ubic.gemma.model.common.auditAndSecurity.User bob = gemmaUser( "bob", true, null );
        ubic.gemma.model.common.auditAndSecurity.User ghost = gemmaUser( "ghost", false, new Date() );
        when( userManager.loadAll() ).thenReturn( Arrays.asList( alice, bob, ghost ) );

        ResponseDataObject<AdminWebService.UsersListResponse> resp = webService.getUsers( true );
        AdminWebService.UsersListResponse body = resp.getData();

        assertThat( body.users ).hasSize( 3 );
        assertThat( body.deletedCount ).isEqualTo( 1 );
    }

    @Test
    public void getUsers_marksAdminFlag_whenAdministratorsGroupPresent() {
        ubic.gemma.model.common.auditAndSecurity.User admin = gemmaUser( "alice", true, null, AuthorityConstants.ADMIN_GROUP_NAME );
        when( userManager.loadAll() ).thenReturn( Collections.singletonList( admin ) );

        ResponseDataObject<AdminWebService.UsersListResponse> resp = webService.getUsers( false );
        AdminWebService.UsersListResponse body = resp.getData();

        assertThat( body.users ).hasSize( 1 );
        assertThat( body.users.get( 0 ).isAdmin ).isTrue();
        assertThat( body.users.get( 0 ).groups ).contains( AuthorityConstants.ADMIN_GROUP_NAME );
    }

    @Test
    public void createUser_returnsTempPassword_andCallsManager() {
        when( userManager.userExists( "alice" ) ).thenReturn( false );
        when( userManager.userWithEmailExists( "alice@example.org" ) ).thenReturn( false );
        ubic.gemma.model.common.auditAndSecurity.User created = gemmaUser( "alice", true, null );
        when( userManager.findByUserName( "alice" ) ).thenReturn( created );

        AdminWebService.CreateUserRequest req = new AdminWebService.CreateUserRequest();
        req.username = "alice";
        req.email = "alice@example.org";
        req.isAdmin = false;

        Response resp = webService.createUser( req );

        assertThat( resp.getStatus() ).isEqualTo( 201 );
        @SuppressWarnings("unchecked")
        ResponseDataObject<AdminWebService.CreateUserResponse> dataObj =
                ( ResponseDataObject<AdminWebService.CreateUserResponse> ) resp.getEntity();
        AdminWebService.CreateUserResponse body = dataObj.getData();
        assertThat( body.temporaryPassword ).isNotNull();
        assertThat( body.temporaryPassword ).hasSize( 16 );
        assertThat( body.temporaryPassword ).matches( "[A-Za-z0-9]+" );

        ArgumentCaptor<UserDetails> ud = ArgumentCaptor.forClass( UserDetails.class );
        verify( userManager ).createUser( ud.capture() );
        assertThat( ud.getValue() ).isInstanceOf( UserDetailsImpl.class );
        assertThat( ud.getValue().getUsername() ).isEqualTo( "alice" );
        assertThat( ud.getValue().isEnabled() ).isTrue();
    }

    @Test
    public void createUser_returns409_whenUsernameTaken() {
        when( userManager.userExists( "alice" ) ).thenReturn( true );

        AdminWebService.CreateUserRequest req = new AdminWebService.CreateUserRequest();
        req.username = "alice";
        req.email = "alice@example.org";

        assertThatThrownBy( () -> webService.createUser( req ) )
                .isInstanceOf( ClientErrorException.class )
                .matches( ex -> ( ( ClientErrorException ) ex ).getResponse().getStatus() == 409 );
    }

    @Test
    public void createUser_returns409_whenEmailTaken() {
        when( userManager.userExists( "alice" ) ).thenReturn( false );
        when( userManager.userWithEmailExists( "alice@example.org" ) ).thenReturn( true );

        AdminWebService.CreateUserRequest req = new AdminWebService.CreateUserRequest();
        req.username = "alice";
        req.email = "alice@example.org";

        assertThatThrownBy( () -> webService.createUser( req ) )
                .isInstanceOf( ClientErrorException.class )
                .matches( ex -> ( ( ClientErrorException ) ex ).getResponse().getStatus() == 409 );
    }

    @Test
    public void createUser_addsToAdminGroup_whenIsAdminTrue() {
        when( userManager.userExists( "alice" ) ).thenReturn( false );
        when( userManager.userWithEmailExists( "alice@example.org" ) ).thenReturn( false );
        when( userManager.findByUserName( "alice" ) )
                .thenReturn( gemmaUser( "alice", true, null, AuthorityConstants.ADMIN_GROUP_NAME ) );

        AdminWebService.CreateUserRequest req = new AdminWebService.CreateUserRequest();
        req.username = "alice";
        req.email = "alice@example.org";
        req.isAdmin = true;

        webService.createUser( req );

        verify( userManager ).addUserToGroup( "alice", AuthorityConstants.ADMIN_GROUP_NAME );
    }

    @Test
    public void patchUser_togglesEnabled() {
        ubic.gemma.model.common.auditAndSecurity.User u = gemmaUser( "alice", true, null );
        when( userManager.findByUserName( "alice" ) ).thenReturn( u );

        AdminWebService.UpdateUserRequest req = new AdminWebService.UpdateUserRequest();
        req.enabled = false;

        webService.patchUser( "alice", req );

        ArgumentCaptor<UserDetails> ud = ArgumentCaptor.forClass( UserDetails.class );
        verify( userManager ).updateUser( ud.capture() );
        assertThat( ud.getValue() ).isInstanceOf( UserDetailsImpl.class );
        assertThat( ud.getValue().getUsername() ).isEqualTo( "alice" );
        assertThat( ud.getValue().isEnabled() ).isFalse();
    }

    @Test
    public void patchUser_addsToAdminGroup_whenIsAdminTrueAndNotAlready() {
        ubic.gemma.model.common.auditAndSecurity.User u = gemmaUser( "alice", true, null );
        when( userManager.findByUserName( "alice" ) ).thenReturn( u );

        AdminWebService.UpdateUserRequest req = new AdminWebService.UpdateUserRequest();
        req.isAdmin = true;

        webService.patchUser( "alice", req );

        verify( userManager ).addUserToGroup( "alice", AuthorityConstants.ADMIN_GROUP_NAME );
        verify( userManager, never() ).removeUserFromGroup( anyString(), anyString() );
    }

    @Test
    public void patchUser_removesFromAdminGroup_whenIsAdminFalseAndCurrentlyAdmin() {
        ubic.gemma.model.common.auditAndSecurity.User u = gemmaUser( "alice", true, null, AuthorityConstants.ADMIN_GROUP_NAME );
        when( userManager.findByUserName( "alice" ) ).thenReturn( u );

        AdminWebService.UpdateUserRequest req = new AdminWebService.UpdateUserRequest();
        req.isAdmin = false;

        webService.patchUser( "alice", req );

        verify( userManager ).removeUserFromGroup( "alice", AuthorityConstants.ADMIN_GROUP_NAME );
        verify( userManager, never() ).addUserToGroup( anyString(), anyString() );
    }

    @Test
    public void patchUser_returns400_whenBodyEmpty() {
        assertThatThrownBy( () -> webService.patchUser( "alice", null ) )
                .isInstanceOf( BadRequestException.class );

        AdminWebService.UpdateUserRequest empty = new AdminWebService.UpdateUserRequest();
        assertThatThrownBy( () -> webService.patchUser( "alice", empty ) )
                .isInstanceOf( BadRequestException.class );
    }

    @Test
    public void patchUser_returns404_whenUserNotFound() {
        when( userManager.findByUserName( "ghost" ) ).thenReturn( null );

        AdminWebService.UpdateUserRequest req = new AdminWebService.UpdateUserRequest();
        req.enabled = false;

        assertThatThrownBy( () -> webService.patchUser( "ghost", req ) )
                .isInstanceOf( NotFoundException.class );
    }

    @Test
    public void deleteUser_callsSoftDelete_andReturns204() {
        // The current admin is "admin", target is "bob" — no self-delete.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken( "admin", "x", Collections.emptyList() ) );
        ubic.gemma.model.common.auditAndSecurity.User bob = gemmaUser( "bob", true, null );
        when( userManager.findByUserName( "bob" ) ).thenReturn( bob );

        Response resp = webService.deleteUser( "bob" );

        assertThat( resp.getStatus() ).isEqualTo( 204 );
        verify( userManager ).softDeleteUser( eq( "bob" ), anyString() );
    }

    @Test
    public void deleteUser_returns404_whenUserNotFound() {
        when( userManager.findByUserName( "ghost" ) ).thenReturn( null );

        assertThatThrownBy( () -> webService.deleteUser( "ghost" ) )
                .isInstanceOf( NotFoundException.class );
        verify( userManager, never() ).softDeleteUser( anyString(), anyString() );
    }

    @Test
    public void resetUserPassword_returnsTempPassword_andCallsManager() {
        when( userManager.findByUserName( "alice" ) ).thenReturn( gemmaUser( "alice", true, null ) );

        ResponseDataObject<AdminWebService.ResetPasswordResponse> resp = webService.resetUserPassword( "alice" );

        AdminWebService.ResetPasswordResponse body = resp.getData();
        assertThat( body.temporaryPassword ).isNotNull();
        assertThat( body.temporaryPassword ).hasSize( 16 );
        assertThat( body.temporaryPassword ).matches( "[A-Za-z0-9]+" );

        // The generated temp password is what gets handed to the service for encoding.
        verify( userManager ).adminChangePassword( "alice", body.temporaryPassword );
    }

    @Test
    public void resetUserPassword_returns404_whenUserNotFound() {
        when( userManager.findByUserName( "ghost" ) ).thenReturn( null );

        assertThatThrownBy( () -> webService.resetUserPassword( "ghost" ) )
                .isInstanceOf( NotFoundException.class );
        verify( userManager, never() ).adminChangePassword( anyString(), anyString() );
    }

    @Test
    public void resetUserPassword_returns409_whenUserSoftDeleted() {
        when( userManager.findByUserName( "bob" ) ).thenReturn( gemmaUser( "bob", false, new Date() ) );

        assertThatThrownBy( () -> webService.resetUserPassword( "bob" ) )
                .isInstanceOf( ClientErrorException.class )
                .matches( ex -> ( ( ClientErrorException ) ex ).getResponse().getStatus() == 409 );
        verify( userManager, never() ).adminChangePassword( anyString(), anyString() );
    }

    /* ===== /admin/tasks/import-geo ===== */

    @Test
    public void importGeoBatch_happyPath_submitsOneTaskPerAccession() {
        when( taskRunningService.submitTaskCommand( org.mockito.ArgumentMatchers.any( ExpressionExperimentLoadTaskCommand.class ) ) )
                .thenReturn( "task-1", "task-2", "task-3" );

        AdminWebService.ImportGeoBatchRequest req = new AdminWebService.ImportGeoBatchRequest();
        req.accessions = Arrays.asList( "GSE1", "GSE2", "GSE3" );
        req.loadPlatformOnly = false;
        req.suppressMatching = true;

        Response resp = webService.importGeoBatch( req );

        assertThat( resp.getStatus() ).isEqualTo( 202 );
        @SuppressWarnings("unchecked")
        ResponseDataObject<AdminWebService.ImportGeoBatchResponse> dataObj =
                ( ResponseDataObject<AdminWebService.ImportGeoBatchResponse> ) resp.getEntity();
        AdminWebService.ImportGeoBatchResponse body = dataObj.getData();
        assertThat( body.count ).isEqualTo( 3 );
        assertThat( body.submittedJobIds ).containsExactly( "task-1", "task-2", "task-3" );

        ArgumentCaptor<ExpressionExperimentLoadTaskCommand> cmd =
                ArgumentCaptor.forClass( ExpressionExperimentLoadTaskCommand.class );
        verify( taskRunningService, org.mockito.Mockito.times( 3 ) ).submitTaskCommand( cmd.capture() );
        assertThat( cmd.getAllValues() ).extracting( ExpressionExperimentLoadTaskCommand::getAccession )
                .containsExactly( "GSE1", "GSE2", "GSE3" );
        // Flags propagate to every submitted command.
        assertThat( cmd.getAllValues() ).allMatch( c -> !c.isLoadPlatformOnly() && c.isSuppressMatching() );
    }

    @Test
    public void importGeoBatch_skipsBlankEntries_butSubmitsTheRest() {
        when( taskRunningService.submitTaskCommand( org.mockito.ArgumentMatchers.any( ExpressionExperimentLoadTaskCommand.class ) ) )
                .thenReturn( "task-a", "task-b" );

        AdminWebService.ImportGeoBatchRequest req = new AdminWebService.ImportGeoBatchRequest();
        req.accessions = Arrays.asList( "  GSE1  ", "", "   ", "GSE2", null );

        Response resp = webService.importGeoBatch( req );

        assertThat( resp.getStatus() ).isEqualTo( 202 );
        @SuppressWarnings("unchecked")
        ResponseDataObject<AdminWebService.ImportGeoBatchResponse> dataObj =
                ( ResponseDataObject<AdminWebService.ImportGeoBatchResponse> ) resp.getEntity();
        assertThat( dataObj.getData().submittedJobIds ).containsExactly( "task-a", "task-b" );

        ArgumentCaptor<ExpressionExperimentLoadTaskCommand> cmd =
                ArgumentCaptor.forClass( ExpressionExperimentLoadTaskCommand.class );
        verify( taskRunningService, org.mockito.Mockito.times( 2 ) ).submitTaskCommand( cmd.capture() );
        assertThat( cmd.getAllValues() ).extracting( ExpressionExperimentLoadTaskCommand::getAccession )
                .containsExactly( "GSE1", "GSE2" );
    }

    @Test
    public void importGeoBatch_returns400_whenBodyMissing() {
        assertThatThrownBy( () -> webService.importGeoBatch( null ) )
                .isInstanceOf( BadRequestException.class );
        verify( taskRunningService, never() )
                .submitTaskCommand( org.mockito.ArgumentMatchers.any( ExpressionExperimentLoadTaskCommand.class ) );
    }

    @Test
    public void importGeoBatch_returns400_whenAccessionsEmpty() {
        AdminWebService.ImportGeoBatchRequest req = new AdminWebService.ImportGeoBatchRequest();
        req.accessions = Collections.emptyList();

        assertThatThrownBy( () -> webService.importGeoBatch( req ) )
                .isInstanceOf( BadRequestException.class );
        verify( taskRunningService, never() )
                .submitTaskCommand( org.mockito.ArgumentMatchers.any( ExpressionExperimentLoadTaskCommand.class ) );
    }

    @Test
    public void importGeoBatch_returns400_whenAllAccessionsBlank() {
        AdminWebService.ImportGeoBatchRequest req = new AdminWebService.ImportGeoBatchRequest();
        req.accessions = Arrays.asList( "", "   ", null );

        assertThatThrownBy( () -> webService.importGeoBatch( req ) )
                .isInstanceOf( BadRequestException.class );
        verify( taskRunningService, never() )
                .submitTaskCommand( org.mockito.ArgumentMatchers.any( ExpressionExperimentLoadTaskCommand.class ) );
    }

    @Test
    public void importGeoBatch_returns400_whenBatchExceedsCap() {
        java.util.List<String> tooMany = new java.util.ArrayList<>();
        for ( int i = 0; i < AdminWebService.MAX_IMPORT_GEO_BATCH + 1; i++ ) {
            tooMany.add( "GSE" + i );
        }
        AdminWebService.ImportGeoBatchRequest req = new AdminWebService.ImportGeoBatchRequest();
        req.accessions = tooMany;

        assertThatThrownBy( () -> webService.importGeoBatch( req ) )
                .isInstanceOf( BadRequestException.class );
        verify( taskRunningService, never() )
                .submitTaskCommand( org.mockito.ArgumentMatchers.any( ExpressionExperimentLoadTaskCommand.class ) );
    }

    @Test
    public void deleteUser_returns409_whenSelfDelete() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken( "alice", "x", Collections.emptyList() ) );
        ubic.gemma.model.common.auditAndSecurity.User alice = gemmaUser( "alice", true, null );
        when( userManager.findByUserName( "alice" ) ).thenReturn( alice );

        assertThatThrownBy( () -> webService.deleteUser( "alice" ) )
                .isInstanceOf( ClientErrorException.class )
                .matches( ex -> ( ( ClientErrorException ) ex ).getResponse().getStatus() == 409 );
        verify( userManager, never() ).softDeleteUser( anyString(), anyString() );
    }

    /* ===== /admin/tasks/geo-grab ===== */

    @Test
    public void grabGeoRecords_returnsMappedValueObjects() throws IOException {
        webService.setGeoBrowser( geoBrowser );
        GeoRecord rec = new GeoRecord();
        rec.setGeoAccession( "GSE12345" );
        rec.setTitle( "A study of foo" );
        rec.setSummary( "Some summary" );
        rec.setOverallDesign( "Some design" );
        rec.setOrganisms( Arrays.asList( "Homo sapiens" ) );
        rec.setPlatform( "GPL570" );
        rec.setSeriesType( "Expression profiling by array" );
        rec.setNumSamples( 24 );
        rec.setReleaseDate( new Date( 1_700_000_000_000L ) );
        when( geoBrowser.getGeoRecords( eq( GeoRecordType.SERIES ),
                eq( Collections.singletonList( "GSE12345" ) ),
                org.mockito.ArgumentMatchers.any( GeoRetrieveConfig.class ) ) )
                .thenReturn( Collections.singletonList( rec ) );

        AdminWebService.GeoGrabRequest req = new AdminWebService.GeoGrabRequest();
        req.accessions = Collections.singletonList( "GSE12345" );

        ResponseDataObject<AdminWebService.GeoGrabResponse> resp = webService.grabGeoRecords( req );
        AdminWebService.GeoGrabResponse body = resp.getData();

        assertThat( body.requestedCount ).isEqualTo( 1 );
        assertThat( body.returnedCount ).isEqualTo( 1 );
        assertThat( body.records ).hasSize( 1 );
        AdminWebService.GeoRecordValueObject vo = body.records.get( 0 );
        assertThat( vo.geoAccession ).isEqualTo( "GSE12345" );
        assertThat( vo.title ).isEqualTo( "A study of foo" );
        assertThat( vo.summary ).isEqualTo( "Some summary" );
        assertThat( vo.overallDesign ).isEqualTo( "Some design" );
        assertThat( vo.organisms ).containsExactly( "Homo sapiens" );
        assertThat( vo.platform ).isEqualTo( "GPL570" );
        assertThat( vo.seriesType ).isEqualTo( "Expression profiling by array" );
        assertThat( vo.numSamples ).isEqualTo( 24 );
        assertThat( vo.releaseDate.getTime() ).isEqualTo( 1_700_000_000_000L );
    }

    @Test
    public void grabGeoRecords_returns400_whenAccessionsEmpty() {
        AdminWebService.GeoGrabRequest req = new AdminWebService.GeoGrabRequest();
        req.accessions = Collections.emptyList();

        assertThatThrownBy( () -> webService.grabGeoRecords( req ) )
                .isInstanceOf( BadRequestException.class );

        AdminWebService.GeoGrabRequest blank = new AdminWebService.GeoGrabRequest();
        blank.accessions = Arrays.asList( "  ", null, "" );
        assertThatThrownBy( () -> webService.grabGeoRecords( blank ) )
                .isInstanceOf( BadRequestException.class );

        assertThatThrownBy( () -> webService.grabGeoRecords( null ) )
                .isInstanceOf( BadRequestException.class );
    }

    @Test
    public void grabGeoRecords_returns502_whenGeoBrowserThrowsIOException() throws IOException {
        webService.setGeoBrowser( geoBrowser );
        when( geoBrowser.getGeoRecords( eq( GeoRecordType.SERIES ),
                org.mockito.ArgumentMatchers.anyCollection(),
                org.mockito.ArgumentMatchers.any( GeoRetrieveConfig.class ) ) )
                .thenThrow( new IOException( "NCBI down" ) );

        AdminWebService.GeoGrabRequest req = new AdminWebService.GeoGrabRequest();
        req.accessions = Collections.singletonList( "GSE12345" );

        assertThatThrownBy( () -> webService.grabGeoRecords( req ) )
                .isInstanceOf( jakarta.ws.rs.ServerErrorException.class )
                .matches( ex -> ( ( jakarta.ws.rs.ServerErrorException ) ex ).getResponse().getStatus() == 502 );
    }

    /* ===== /admin/tasks/geo-scrape ===== */

    @Test
    public void submitGeoScrape_happyPath_submits202() {
        when( taskRunningService.submitTaskCommand(
                org.mockito.ArgumentMatchers.any( GeoScrapeTaskCommand.class ) ) )
                .thenReturn( "task-scrape-1" );

        AdminWebService.GeoScrapeRequest req = new AdminWebService.GeoScrapeRequest();
        req.maxRecords = 50;
        req.criteria = Arrays.asList( "brain", "tfperturb" );
        req.dryRun = false;

        Response resp = webService.submitGeoScrape( req );

        assertThat( resp.getStatus() ).isEqualTo( 202 );
        assertThat( resp.getLocation() ).hasToString( "/tasks/task-scrape-1" );
        @SuppressWarnings("unchecked")
        ResponseDataObject<AdminWebService.GeoScrapeSubmitResponse> dataObj =
                ( ResponseDataObject<AdminWebService.GeoScrapeSubmitResponse> ) resp.getEntity();
        assertThat( dataObj.getData().submittedJobId ).isEqualTo( "task-scrape-1" );

        ArgumentCaptor<GeoScrapeTaskCommand> captor = ArgumentCaptor.forClass( GeoScrapeTaskCommand.class );
        verify( taskRunningService ).submitTaskCommand( captor.capture() );
        GeoScrapeTaskCommand cmd = captor.getValue();
        assertThat( cmd.getMaxRecords() ).isEqualTo( 50 );
        assertThat( cmd.getCriteria() ).containsExactly( "brain", "tfperturb" );
        assertThat( cmd.isDryRun() ).isFalse();
        verify( geoScrapeService, org.mockito.Mockito.never() )
                .scrapeDryRun( org.mockito.ArgumentMatchers.any() );
    }

    @Test
    public void submitGeoScrape_dryRunTrue_returnsSyncCandidateList() {
        GeoScrapeDryRunCandidate c = new GeoScrapeDryRunCandidate();
        c.preboardedId = null;
        c.accession = "GSE12345";
        c.source = "GEO";
        c.identifyingMetadata = "{\"geoAccession\":\"GSE12345\"}";
        c.state = "Preboarded";
        c.proposalCount = 0L;
        c.matchedCriteria = Arrays.asList( "brain" );
        when( geoScrapeService.scrapeDryRun( org.mockito.ArgumentMatchers.any() ) )
                .thenReturn( new GeoScrapeService.DryRunResult( Arrays.asList( c ), "GSE99999",
                        new java.util.GregorianCalendar( 2026, java.util.Calendar.AUGUST, 1 ).getTime(),
                        Arrays.asList( "GSE304614" ), 142 ) );

        AdminWebService.GeoScrapeRequest req = new AdminWebService.GeoScrapeRequest();
        req.maxRecords = 25;
        req.criteria = Arrays.asList( "brain" );
        req.dryRun = true;

        Response resp = webService.submitGeoScrape( req );

        assertThat( resp.getStatus() ).isEqualTo( 200 );
        AdminWebService.GeoScrapeDryRunResponse dataObj =
                ( AdminWebService.GeoScrapeDryRunResponse ) resp.getEntity();
        // `data` must stay the candidate array it has always been -- existing clients parse it.
        assertThat( dataObj.data ).hasSize( 1 );
        GeoScrapeDryRunCandidate got = dataObj.data.get( 0 );
        assertThat( got.accession ).isEqualTo( "GSE12345" );
        assertThat( got.preboardedId ).isNull();
        assertThat( got.matchedCriteria ).containsExactly( "brain" );
        // and the two new fields ride alongside it
        assertThat( dataObj.lastScannedAccession ).isEqualTo( "GSE99999" );
        assertThat( dataObj.lastScannedDate ).isNotNull();
        assertThat( dataObj.incompleteRecords ).containsExactly( "GSE304614" );
        assertThat( dataObj.nextOffset )
                .as( "record-level resumption: handed straight back as `skip`" )
                .isEqualTo( 142 );

        ArgumentCaptor<GeoScrapeService.ScrapeRequest> captor =
                ArgumentCaptor.forClass( GeoScrapeService.ScrapeRequest.class );
        verify( geoScrapeService ).scrapeDryRun( captor.capture() );
        GeoScrapeService.ScrapeRequest sr = captor.getValue();
        assertThat( sr.getMaxRecords() ).isEqualTo( 25 );
        assertThat( sr.getCriteria() ).containsExactly( "brain" );
        assertThat( sr.isDryRun() ).isTrue();
        assertThat( sr.getSkip() ).isNull();
        verify( taskRunningService, org.mockito.Mockito.never() )
                .submitTaskCommand( org.mockito.ArgumentMatchers.any( GeoScrapeTaskCommand.class ) );
    }

    @Test
    public void submitGeoScrape_nullBody_submitsDefaults() {
        when( taskRunningService.submitTaskCommand(
                org.mockito.ArgumentMatchers.any( GeoScrapeTaskCommand.class ) ) )
                .thenReturn( "task-scrape-2" );

        Response resp = webService.submitGeoScrape( null );

        assertThat( resp.getStatus() ).isEqualTo( 202 );
        ArgumentCaptor<GeoScrapeTaskCommand> captor = ArgumentCaptor.forClass( GeoScrapeTaskCommand.class );
        verify( taskRunningService ).submitTaskCommand( captor.capture() );
        assertThat( captor.getValue().isDryRun() ).isFalse();
        assertThat( captor.getValue().getMaxRecords() ).isNull();
    }

    /* ===== GET /admin/geo-scrape/last ===== */

    @Test
    public void getLastGeoScrape_happyPath_returnsValueObject() {
        GeoScrapeWatermark wm = new GeoScrapeWatermark();
        wm.setId( 7L );
        wm.setScannedAt( new Date( 1_700_000_000_000L ) );
        wm.setScanFrom( new Date( 1_699_000_000_000L ) );
        wm.setScanTo( new Date( 1_700_500_000_000L ) );
        wm.setRecordsScanned( 200 );
        wm.setRecordsMatched( 8 );
        wm.setCriteriaApplied( "brain,tfperturb" );
        wm.setStatus( GeoScrapeWatermark.Status.COMPLETED );
        when( geoScrapeService.getLastWatermark() ).thenReturn( wm );

        ResponseDataObject<AdminWebService.GeoScrapeWatermarkValueObject> resp = webService.getLastGeoScrape();

        AdminWebService.GeoScrapeWatermarkValueObject vo = resp.getData();
        assertThat( vo.id ).isEqualTo( 7L );
        assertThat( vo.recordsScanned ).isEqualTo( 200 );
        assertThat( vo.recordsMatched ).isEqualTo( 8 );
        assertThat( vo.criteriaApplied ).isEqualTo( "brain,tfperturb" );
        assertThat( vo.status ).isEqualTo( "COMPLETED" );
    }

    @Test
    public void getLastGeoScrape_returns404_whenNoWatermark() {
        when( geoScrapeService.getLastWatermark() ).thenReturn( null );

        assertThatThrownBy( () -> webService.getLastGeoScrape() )
                .isInstanceOf( NotFoundException.class );
    }

    /* ===== /admin/tasks/multifunctionality ===== */

    @Test
    public void submitMultifunctionalityRecompute_happyPath_submitsTaskWithResolvedTaxon() {
        Taxon human = new Taxon();
        human.setId( 1L );
        human.setCommonName( "human" );
        TaxonArg<?> taxonArg = TaxonArg.valueOf( "human" );
        // Real TaxonArgService routes the "human" lookup through TaxonService.findByCommonName.
        when( innerTaxonService.findByCommonName( "human" ) ).thenReturn( human );
        when( taskRunningService.submitTaskCommand( org.mockito.ArgumentMatchers.any( MultifunctionalityTaskCommand.class ) ) )
                .thenReturn( "task-mf-1" );

        Response resp = webService.submitMultifunctionalityRecompute( taxonArg );

        assertThat( resp.getStatus() ).isEqualTo( 202 );
        assertThat( resp.getLocation() ).hasToString( "/tasks/task-mf-1" );
        @SuppressWarnings("unchecked")
        ResponseDataObject<AdminWebService.MultifunctionalityRecomputeResponse> dataObj =
                ( ResponseDataObject<AdminWebService.MultifunctionalityRecomputeResponse> ) resp.getEntity();
        AdminWebService.MultifunctionalityRecomputeResponse body = dataObj.getData();
        assertThat( body.submittedJobId ).isEqualTo( "task-mf-1" );
        assertThat( body.taxonId ).isEqualTo( 1L );
        assertThat( body.taxonName ).isEqualTo( "human" );

        ArgumentCaptor<MultifunctionalityTaskCommand> cmd =
                ArgumentCaptor.forClass( MultifunctionalityTaskCommand.class );
        verify( taskRunningService ).submitTaskCommand( cmd.capture() );
        assertThat( cmd.getValue().getTaxon() ).isSameAs( human );
    }

    /* ===== /admin/blacklist ===== */

    @Test
    public void addBlacklistEntry_happyPath_createsBlacklistedExperiment() {
        when( blacklistedEntityService.findByAccession( "GSE99999" ) ).thenReturn( null );
        ExternalDatabase geo = new ExternalDatabase();
        geo.setName( "GEO" );
        when( externalDatabaseReadService.findByName( "GEO" ) ).thenReturn( geo );
        ArgumentCaptor<BlacklistedEntity> captor = ArgumentCaptor.forClass( BlacklistedEntity.class );
        when( blacklistedEntityService.create( captor.capture() ) )
                .thenAnswer( inv -> inv.getArgument( 0 ) );

        AdminWebService.BlacklistRequest req = new AdminWebService.BlacklistRequest();
        req.accession = "GSE99999";
        req.reason = "withdrawn by submitter";

        Response resp = webService.addBlacklistEntry( req );

        assertThat( resp.getStatus() ).isEqualTo( 201 );
        BlacklistedEntity created = captor.getValue();
        assertThat( created ).isInstanceOf( BlacklistedExperiment.class );
        assertThat( created.getShortName() ).isEqualTo( "GSE99999" );
        assertThat( created.getReason() ).isEqualTo( "withdrawn by submitter" );
        DatabaseEntry de = created.getExternalAccession();
        assertThat( de ).isNotNull();
        assertThat( de.getAccession() ).isEqualTo( "GSE99999" );
        assertThat( de.getExternalDatabase() ).isSameAs( geo );
    }

    @Test
    public void addBlacklistEntry_happyPath_createsBlacklistedPlatform() {
        when( blacklistedEntityService.findByAccession( "GPL1234" ) ).thenReturn( null );
        ExternalDatabase geo = new ExternalDatabase();
        geo.setName( "GEO" );
        when( externalDatabaseReadService.findByName( "GEO" ) ).thenReturn( geo );
        ArgumentCaptor<BlacklistedEntity> captor = ArgumentCaptor.forClass( BlacklistedEntity.class );
        when( blacklistedEntityService.create( captor.capture() ) )
                .thenAnswer( inv -> inv.getArgument( 0 ) );

        AdminWebService.BlacklistRequest req = new AdminWebService.BlacklistRequest();
        req.accession = "GPL1234";
        req.reason = "deprecated platform";

        Response resp = webService.addBlacklistEntry( req );

        assertThat( resp.getStatus() ).isEqualTo( 201 );
        assertThat( captor.getValue() ).isInstanceOf( BlacklistedPlatform.class );
    }

    @Test
    public void addBlacklistEntry_returns409_whenAlreadyBlacklisted() {
        BlacklistedExperiment existing = new BlacklistedExperiment();
        existing.setShortName( "GSE99999" );
        when( blacklistedEntityService.findByAccession( "GSE99999" ) ).thenReturn( existing );

        AdminWebService.BlacklistRequest req = new AdminWebService.BlacklistRequest();
        req.accession = "GSE99999";
        req.reason = "dup";

        assertThatThrownBy( () -> webService.addBlacklistEntry( req ) )
                .isInstanceOf( ClientErrorException.class )
                .matches( ex -> ( ( ClientErrorException ) ex ).getResponse().getStatus() == 409 );
        verify( blacklistedEntityService, never() ).create( org.mockito.ArgumentMatchers.any( BlacklistedEntity.class ) );
    }

    @Test
    public void addBlacklistEntry_returns400_whenBodyMissing() {
        assertThatThrownBy( () -> webService.addBlacklistEntry( null ) )
                .isInstanceOf( BadRequestException.class );
        verify( blacklistedEntityService, never() ).create( org.mockito.ArgumentMatchers.any( BlacklistedEntity.class ) );
    }

    @Test
    public void addBlacklistEntry_returns400_whenReasonBlank() {
        AdminWebService.BlacklistRequest req = new AdminWebService.BlacklistRequest();
        req.accession = "GSE1";
        req.reason = "   ";

        assertThatThrownBy( () -> webService.addBlacklistEntry( req ) )
                .isInstanceOf( BadRequestException.class );
        verify( blacklistedEntityService, never() ).create( org.mockito.ArgumentMatchers.any( BlacklistedEntity.class ) );
    }

    @Test
    public void addBlacklistEntry_returns400_whenAccessionPrefixUnrecognised() {
        when( blacklistedEntityService.findByAccession( "FOO123" ) ).thenReturn( null );

        AdminWebService.BlacklistRequest req = new AdminWebService.BlacklistRequest();
        req.accession = "FOO123";
        req.reason = "test";

        assertThatThrownBy( () -> webService.addBlacklistEntry( req ) )
                .isInstanceOf( BadRequestException.class );
        verify( blacklistedEntityService, never() ).create( org.mockito.ArgumentMatchers.any( BlacklistedEntity.class ) );
    }

    @Test
    public void deleteBlacklistEntry_happyPath_returns204() {
        BlacklistedExperiment existing = new BlacklistedExperiment();
        existing.setShortName( "GSE99999" );
        when( blacklistedEntityService.findByAccession( "GSE99999" ) ).thenReturn( existing );

        Response resp = webService.deleteBlacklistEntry( "GSE99999" );

        assertThat( resp.getStatus() ).isEqualTo( 204 );
        verify( blacklistedEntityService ).remove( existing );
    }

    @Test
    public void deleteBlacklistEntry_returns404_whenAccessionNotFound() {
        when( blacklistedEntityService.findByAccession( "GSE0" ) ).thenReturn( null );

        assertThatThrownBy( () -> webService.deleteBlacklistEntry( "GSE0" ) )
                .isInstanceOf( NotFoundException.class );
        verify( blacklistedEntityService, never() ).remove( org.mockito.ArgumentMatchers.any( BlacklistedEntity.class ) );
    }

    @Test
    public void listBlacklistEntries_paginatesAndSortsByAccession() {
        ExternalDatabase geo = new ExternalDatabase();
        geo.setName( "GEO" );
        BlacklistedExperiment a = makeBlacklistedExperiment( "GSE1", "r1", geo );
        BlacklistedExperiment b = makeBlacklistedExperiment( "GSE2", "r2", geo );
        BlacklistedPlatform c = makeBlacklistedPlatform( "GPL3", "r3", geo );
        when( blacklistedEntityService.loadAll() ).thenReturn( Arrays.asList( b, a, c ) );

        // Page 1: limit=2, offset=0 → GPL3, GSE1
        ResponseDataObject<AdminWebService.BlacklistListResponse> resp1 =
                webService.listBlacklistEntries( 2, 0 );
        AdminWebService.BlacklistListResponse body1 = resp1.getData();
        assertThat( body1.total ).isEqualTo( 3 );
        assertThat( body1.limit ).isEqualTo( 2 );
        assertThat( body1.offset ).isEqualTo( 0 );
        assertThat( body1.count ).isEqualTo( 2 );
        assertThat( body1.entries ).extracting( v -> v.getAccession() )
                .containsExactly( "GPL3", "GSE1" );

        // Page 2: limit=2, offset=2 → GSE2
        ResponseDataObject<AdminWebService.BlacklistListResponse> resp2 =
                webService.listBlacklistEntries( 2, 2 );
        AdminWebService.BlacklistListResponse body2 = resp2.getData();
        assertThat( body2.total ).isEqualTo( 3 );
        assertThat( body2.count ).isEqualTo( 1 );
        assertThat( body2.entries ).extracting( v -> v.getAccession() )
                .containsExactly( "GSE2" );

        // Offset past end: empty
        ResponseDataObject<AdminWebService.BlacklistListResponse> resp3 =
                webService.listBlacklistEntries( 100, 10 );
        assertThat( resp3.getData().count ).isEqualTo( 0 );
        assertThat( resp3.getData().entries ).isEmpty();
    }

    @Test
    public void listBlacklistEntries_returns400_whenLimitNegative() {
        assertThatThrownBy( () -> webService.listBlacklistEntries( -1, 0 ) )
                .isInstanceOf( BadRequestException.class );
        assertThatThrownBy( () -> webService.listBlacklistEntries( 10, -1 ) )
                .isInstanceOf( BadRequestException.class );
    }

    @Test
    public void listBlacklistEntries_capsLimitAtMaxPageSize() {
        when( blacklistedEntityService.loadAll() ).thenReturn( Collections.emptyList() );

        ResponseDataObject<AdminWebService.BlacklistListResponse> resp =
                webService.listBlacklistEntries( AdminWebService.MAX_BLACKLIST_PAGE_SIZE + 500, 0 );

        assertThat( resp.getData().limit ).isEqualTo( AdminWebService.MAX_BLACKLIST_PAGE_SIZE );
    }

    private BlacklistedExperiment makeBlacklistedExperiment( String acc, String reason, ExternalDatabase db ) {
        BlacklistedExperiment e = new BlacklistedExperiment();
        e.setShortName( acc );
        e.setReason( reason );
        DatabaseEntry de = DatabaseEntry.Factory.newInstance( acc, db );
        e.setExternalAccession( de );
        return e;
    }

    private BlacklistedPlatform makeBlacklistedPlatform( String acc, String reason, ExternalDatabase db ) {
        BlacklistedPlatform p = new BlacklistedPlatform();
        p.setShortName( acc );
        p.setReason( reason );
        DatabaseEntry de = DatabaseEntry.Factory.newInstance( acc, db );
        p.setExternalAccession( de );
        return p;
    }
}
