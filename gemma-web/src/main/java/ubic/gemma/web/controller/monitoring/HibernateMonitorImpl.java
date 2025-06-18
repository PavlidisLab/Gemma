/*
 * The Gemma project
 *
 * Copyright (c) 2007 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.web.controller.monitoring;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.CollectionStatistics;
import org.hibernate.stat.EntityStatistics;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;

/**
 * Monitoring of Hibernate status.
 *
 * @author pavlidis
 */
@Component
public class HibernateMonitorImpl implements HibernateMonitor {

    private static final Log log = LogFactory.getLog( HibernateMonitorImpl.class.getName() );

    @Autowired
    private SessionFactory sessionFactory;

    @Override
    public String getStats() {
        return this.getStats( false, false, false );
    }

    @Override
    public String getStats( boolean showEntityStats, boolean showCollectionStats,
            boolean showSecondLevelCacheDetails ) {

        Statistics stats = sessionFactory.getStatistics();

        StringBuilder buf = new StringBuilder();
        buf.append( "Statistics started at: " ).append( new Date( stats.getStartTime() ) ).append( "\n" );
        long flushes = stats.getFlushCount();
        long trans = stats.getTransactionCount();
        long prep = stats.getPrepareStatementCount();
        long open = stats.getSessionOpenCount();
        long close = stats.getSessionCloseCount();
        long ex = stats.getQueryExecutionCount();

        buf.append( "Queries executed: " ).append( ex ).append( "\n" );

        buf.append( open ).append( " sessions opened, " ).append( close ).append( " closed\n" );
        buf.append( prep ).append( " statements prepared, " ).append( trans ).append( " transactions completed, " )
                .append( flushes ).append( " flushes.\n" );
        String slowQuery = stats.getQueryExecutionMaxTimeQueryString();
        long queryExecutionMaxTime = stats.getQueryExecutionMaxTime();
        if ( queryExecutionMaxTime > 1000 ) {
            buf.append( "Slowest query [" ).append( queryExecutionMaxTime ).append( "ms]: " )
                    .append( StringUtils.abbreviate( slowQuery, 150 ) ).append( "\n" );
        }
        buf.append( "\n------------------- Query Cache stats -----------------------\n" );
        long queryCacheHitCount = stats.getQueryCacheHitCount();
        long queryCacheMissCount = stats.getQueryCacheMissCount();
        long queryCachePutCount = stats.getQueryCachePutCount();

        buf.append( "Puts: " ).append( queryCachePutCount ).append( "\n" );
        buf.append( "Hits: " ).append( queryCacheHitCount ).append( "\n" );
        buf.append( "Misses: " ).append( queryCacheMissCount ).append( "\n" );

        buf.append( "\n------------------- Second Level Cache stats -----------------------\n" );
        long secCacheHits = stats.getSecondLevelCacheHitCount();
        long secCacheMiss = stats.getSecondLevelCacheMissCount();
        long secCachePut = stats.getSecondLevelCachePutCount();
        buf.append( "Puts: " ).append( secCachePut ).append( "\n" );
        buf.append( "Hits: " ).append( secCacheHits ).append( "\n" );
        buf.append( "Misses: " ).append( secCacheMiss ).append( "\n" );

        if ( showSecondLevelCacheDetails ) {
            String[] regions = stats.getSecondLevelCacheRegionNames();
            Arrays.sort( regions );
            for ( String region : regions ) {
                SecondLevelCacheStatistics secondLevelCacheStatistics = stats.getSecondLevelCacheStatistics( region );
                long hitCount = secondLevelCacheStatistics.getHitCount();
                long missCount = secondLevelCacheStatistics.getMissCount();
                long putCount = secondLevelCacheStatistics.getPutCount();
                long size = secondLevelCacheStatistics.getSizeInMemory();
                long count = secondLevelCacheStatistics.getElementCountInMemory();
                long diskCount = secondLevelCacheStatistics.getElementCountOnDisk();

                if ( putCount > 0 || hitCount > 0 || missCount > 0 ) {
                    buf.append( region ).append( ": " ).append( hitCount ).append( " hits; " ).append( missCount )
                            .append( " misses; " ).append( putCount ).append( " puts; Memcount=" ).append( count )
                            .append( "; Diskcount=" ).append( diskCount ).append( " MemSizeBytes=" ).append( size )
                            .append( "\n" );
                }
            }
        }

        if ( showCollectionStats ) {
            buf.append( "\n------------------- Collection stats -----------------------\n" );
            String[] collectionRoleNames = stats.getCollectionRoleNames();
            Arrays.sort( collectionRoleNames );
            for ( String string : collectionRoleNames ) {
                CollectionStatistics collectionStatistics = stats.getCollectionStatistics( string );
                long fetchCount = collectionStatistics.getFetchCount();
                long loadCount = collectionStatistics.getLoadCount();
                long updateCount = collectionStatistics.getUpdateCount();
                if ( fetchCount > 0 || loadCount > 0 || updateCount > 0 ) {
                    buf.append( string ).append( ": " ).append( fetchCount ).append( " fetches, " ).append( loadCount )
                            .append( " loads, " ).append( updateCount ).append( " updates\n" );
                }
            }
        }

        if ( showEntityStats ) {
            buf.append( "\n------------------- Entity stats -----------------------\n" );
            String[] entityNames = stats.getEntityNames();
            Arrays.sort( entityNames );
            for ( String string : entityNames ) {
                EntityStatistics entityStats = stats.getEntityStatistics( string );
                long changes =
                        entityStats.getInsertCount() + entityStats.getUpdateCount() + entityStats.getDeleteCount();
                if ( changes > 0 ) {
                    String shortName;
                    try {
                        shortName = Class.forName( string ).getSimpleName().replaceFirst( "Impl", "" );
                        buf.append( shortName ).append( " updates: " ).append( changes ).append( " \n" );
                    } catch ( ClassNotFoundException e ) {
                        HibernateMonitorImpl.log.error( e, e );
                    }
                }
                long reads = entityStats.getLoadCount();
                if ( reads > 0 ) {
                    String shortName;
                    try {
                        shortName = Class.forName( string ).getSimpleName().replaceFirst( "Impl", "" );
                        buf.append( shortName ).append( " read: " ).append( reads ).append( " \n" );
                    } catch ( ClassNotFoundException e ) {
                        HibernateMonitorImpl.log.error( e, e );
                    }
                }

            }
        }
        return buf.toString();
    }

    @Override
    public void resetStats() {
        sessionFactory.getStatistics().clear();
    }

}
