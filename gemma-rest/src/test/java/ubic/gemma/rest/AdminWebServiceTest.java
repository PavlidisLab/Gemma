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

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import ubic.gemma.core.job.SubmittedTask;
import ubic.gemma.core.job.TaskRunningService;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketType;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.TicketService;
import ubic.gemma.persistence.service.expression.experiment.AgentProposalService;
import ubic.gemma.rest.util.ResponseDataObject;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private AgentProposalService agentProposalService;
    @Mock
    private TicketService ticketService;

    private AdminWebService webService;

    @BeforeEach
    public void setUp() {
        webService = new AdminWebService( cacheManager, sessionFactory, taskRunningService, sessionRegistry,
                Collections.emptyList(), dataSource, userManager, agentProposalService, ticketService );
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
    public void getCurationStatusSnapshotComposesProposalAndTicketAggregates() {
        // Proposals: 5 in last 24h, 42 in last 7d, lifetime status histogram, 3 distinct runs in 7d,
        // most recent ranAt = +/- now.
        Date lastRan = new Date( 1_700_000_000_000L );
        when( agentProposalService.countSince( org.mockito.ArgumentMatchers.any( Date.class ) ) )
                .thenReturn( 5L )   // first call: 24h
                .thenReturn( 42L ); // second call: 7d
        Map<String, Long> byStatus = new LinkedHashMap<>();
        byStatus.put( "OPEN", 18L );
        byStatus.put( "FINALIZED", 1003L );
        byStatus.put( "REOPENED", 7L );
        when( agentProposalService.countByStatusSince( null ) ).thenReturn( byStatus );
        when( agentProposalService.countDistinctRunIdsSince( org.mockito.ArgumentMatchers.any( Date.class ) ) )
                .thenReturn( 12L );
        when( agentProposalService.findLatestRanAt() ).thenReturn( lastRan );

        // Tickets: 14 BATCH_INFO_NEEDED + 27 QUALITY_REVIEW open, total 41 open, oldest 5d ago.
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
        assertThat( body.proposals.byStatus )
                .containsEntry( "OPEN", 18L )
                .containsEntry( "FINALIZED", 1003L )
                .containsEntry( "REOPENED", 7L );

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
        when( agentProposalService.countSince( org.mockito.ArgumentMatchers.any( Date.class ) ) ).thenReturn( 0L );
        when( agentProposalService.countByStatusSince( null ) ).thenReturn( Collections.emptyMap() );
        when( agentProposalService.countDistinctRunIdsSince( org.mockito.ArgumentMatchers.any( Date.class ) ) )
                .thenReturn( 0L );
        when( agentProposalService.findLatestRanAt() ).thenReturn( null );
        when( ticketService.countOpenByType() ).thenReturn( Collections.emptyMap() );
        when( ticketService.countOpen() ).thenReturn( 0L );
        when( ticketService.findOldestOpenCreatedAt() ).thenReturn( null );

        ResponseDataObject<AdminWebService.CurationStatusResponse> resp = webService.getCurationStatus();
        AdminWebService.CurationStatusResponse body = resp.getData();

        assertThat( body.proposals.totalLast24h ).isZero();
        assertThat( body.proposals.totalLast7d ).isZero();
        assertThat( body.proposals.byStatus ).isEmpty();
        assertThat( body.tickets.openCount ).isZero();
        assertThat( body.tickets.openCountByType ).isEmpty();
        assertThat( body.tickets.oldestOpenAgeDays ).isNull();
        assertThat( body.agentRuns.distinctRunIds ).isZero();
        assertThat( body.agentRuns.lastRanAt ).isNull();
    }
}
