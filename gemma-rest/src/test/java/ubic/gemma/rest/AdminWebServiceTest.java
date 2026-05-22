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
import ubic.gemma.rest.util.ResponseDataObject;

import java.util.Arrays;

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

    private AdminWebService webService;

    @BeforeEach
    public void setUp() {
        webService = new AdminWebService( cacheManager, sessionFactory );
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
}
