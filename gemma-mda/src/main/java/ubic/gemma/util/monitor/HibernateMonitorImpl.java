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
package ubic.gemma.util.monitor;

//import java.lang.management.ManagementFactory;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.CollectionStatistics;
import org.hibernate.stat.EntityStatistics;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Monitoring of Hibernate status.
 * 
 * @author pavlidis
 * @version $Id$
 */
@Component
public class HibernateMonitorImpl implements HibernateMonitor {

    private static Log log = LogFactory.getLog( HibernateMonitorImpl.class.getName() );

    @Autowired
    private SessionFactory sessionFactory;

    private boolean showQueryCacheStats = true;

    /* (non-Javadoc)
     * @see ubic.gemma.util.monitor.HibernateMonitor#getStats()
     */
    @Override
    public String getStats() {
        return getStats( false, false, false );
    }

    /* (non-Javadoc)
     * @see ubic.gemma.util.monitor.HibernateMonitor#getStats(boolean, boolean, boolean)
     */
    @Override
    public String getStats( boolean showEntityStats, boolean showCollectionStats, boolean showSecondLevelCacheDetails ) {

        Statistics stats = sessionFactory.getStatistics();

        StringBuilder buf = new StringBuilder();
        long flushes = stats.getFlushCount();
        long trans = stats.getTransactionCount();
        long prep = stats.getPrepareStatementCount();
        long open = stats.getSessionOpenCount();
        long close = stats.getSessionCloseCount();

        buf.append( open + " sessions opened, " + close + " closed\n" );
        buf.append( prep + " statements prepared, " + trans + " transactions completed, " + flushes + " flushes.\n" );

        if ( showQueryCacheStats ) {
            buf.append( "\n------------------- Query Cache stats -----------------------\n" );
            long queryCacheHitCount = stats.getQueryCacheHitCount();
            long queryCacheMissCount = stats.getQueryCacheMissCount();
            long queryCachePutCount = stats.getQueryCachePutCount();
            long queryCacheExecutions = stats.getQueryExecutionCount();

            buf.append( "Hits: " + queryCacheHitCount + "\n" );
            buf.append( "Misses: " + queryCacheMissCount + "\n" );
            buf.append( "Puts: " + queryCachePutCount + "\n" );
            buf.append( "Executions: " + queryCacheExecutions + "\n" );

        }

        buf.append( "\n------------------- Second Level Cache stats -----------------------\n" );
        long secCacheHits = stats.getSecondLevelCacheHitCount();
        long secCacheMiss = stats.getSecondLevelCacheMissCount();
        long secCachePut = stats.getSecondLevelCachePutCount();
        buf.append( "2' Cache: " + secCacheHits + " hits; " + secCacheMiss + " miss; " + secCachePut + " put\n" );

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
                    buf.append( region + ": " + hitCount + " hits; " + missCount + " misses; " + putCount
                            + " puts; Memcount=" + count + "; Diskcount=" + diskCount + " MemSizeBytes=" + size + "\n" );
                }
            }
        }

        String slowQuery = stats.getQueryExecutionMaxTimeQueryString();
        long queryExecutionMaxTime = stats.getQueryExecutionMaxTime();
        if ( queryExecutionMaxTime > 1000 ) {
            buf.append( "Slowest query [" + queryExecutionMaxTime + "ms]: " + slowQuery + "\n" );
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
                    buf.append( string + ": " + fetchCount + " fetches, " + loadCount + " loads, " + updateCount
                            + " updates\n" );
                }
            }
        }

        if ( showEntityStats ) {
            buf.append( "\n------------------- Entity stats -----------------------\n" );
            String[] entityNames = stats.getEntityNames();
            Arrays.sort( entityNames );
            for ( String string : entityNames ) {
                EntityStatistics entityStats = stats.getEntityStatistics( string );
                long changes = entityStats.getInsertCount() + entityStats.getUpdateCount()
                        + entityStats.getDeleteCount();
                if ( changes > 0 ) {
                    String shortName;
                    try {
                        shortName = Class.forName( string ).getSimpleName().replaceFirst( "Impl", "" );
                        buf.append( shortName + " updates: " + changes + " \n" );
                    } catch ( ClassNotFoundException e ) {
                        log.error( e, e );
                    }
                }
                long reads = entityStats.getLoadCount();
                if ( reads > 0 ) {
                    String shortName;
                    try {
                        shortName = Class.forName( string ).getSimpleName().replaceFirst( "Impl", "" );
                        buf.append( shortName + " read: " + reads + " \n" );
                    } catch ( ClassNotFoundException e ) {
                        log.error( e, e );
                    }
                }

            }
        }
        return buf.toString();
    }

    /* (non-Javadoc)
     * @see ubic.gemma.util.monitor.HibernateMonitor#resetStats()
     */
    @Override
    public void resetStats() {
        sessionFactory.getStatistics().clear();
    }

    public void setSessionFactory( SessionFactory sessionFactory ) {
        this.sessionFactory = sessionFactory;
    }

}
