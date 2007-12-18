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
package ubic.gemma.persistence;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.CollectionStatistics;
import org.hibernate.stat.EntityStatistics;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;

/**
 * Monitoring of Hibernate status.
 * 
 * @spring.bean id="hibernateMonitor"
 * @spring.property name="sessionFactory" ref="sessionFactory"
 * @author pavlidis
 * @version $Id$
 */
public class HibernateMonitor {

    private static Log log = LogFactory.getLog( HibernateMonitor.class.getName() );

    SessionFactory sessionFactory;

    private boolean showQueryCacheStats = true;
    private boolean showEntityStats = false;
    private boolean showCollectionStats = false;
    private boolean showSecondLevelCacheDetails = false;

    /**
     * Log some statistics.
     */
    public void getStats() {

        Statistics stats = sessionFactory.getStatistics();

        StringBuilder buf = new StringBuilder();
        buf.append( "\n------------------- Hibernate stats -----------------------\n" );

        long flushes = stats.getFlushCount();
        long trans = stats.getTransactionCount();
        long prep = stats.getPrepareStatementCount();
        long open = stats.getSessionOpenCount();
        long close = stats.getSessionCloseCount();

        buf.append( open + " sessions opened, " + close + " closed\n" );
        buf.append( prep + " statements prepared, " + trans + " transactions completed, " + flushes + " flushes.\n" );

        if ( showQueryCacheStats ) {
            long queryCacheHitCount = stats.getQueryCacheHitCount();
            long queryCacheMissCount = stats.getQueryCacheMissCount();
            long queryCachePutCount = stats.getQueryCachePutCount();

            buf.append( "Query cache: " + queryCacheHitCount + " hits; " + queryCacheMissCount + " miss; "
                    + queryCachePutCount + " put\n" );
        }

        long secCacheHits = stats.getSecondLevelCacheHitCount();
        long secCacheMiss = stats.getSecondLevelCacheMissCount();
        long secCachePut = stats.getSecondLevelCachePutCount();
        buf.append( "2' Cache: " + secCacheHits + " hits; " + secCacheMiss + " miss; " + secCachePut + " put\n" );

        if ( showSecondLevelCacheDetails ) {
            String[] regions = stats.getSecondLevelCacheRegionNames();
            for ( String string : regions ) {
                SecondLevelCacheStatistics secondLevelCacheStatistics = stats.getSecondLevelCacheStatistics( string );
                long hitCount = secondLevelCacheStatistics.getHitCount();
                long missCount = secondLevelCacheStatistics.getMissCount();
                long putCount = secondLevelCacheStatistics.getPutCount();
                if ( hitCount > 0 || missCount > 0 || putCount > 0 ) {
                    try {
                        String shortName = Class.forName( string ).getSimpleName().replaceFirst( "Impl", "" );
                        buf.append( "    " + shortName + ": " + hitCount + " hits; " + missCount + " misses; "
                                + putCount + " puts" + "\n" );
                    } catch ( ClassNotFoundException e ) {
                        buf.append( "    " + string + ": " + hitCount + " hits; " + missCount + " misses; " + putCount
                                + " puts" + "\n" );
                    }
                }
            }
        }

        if ( showCollectionStats ) {
            String[] collectionRoleNames = stats.getCollectionRoleNames();
            for ( String string : collectionRoleNames ) {
                CollectionStatistics collectionStatistics = stats.getCollectionStatistics( string );
                long fetchCount = collectionStatistics.getFetchCount();
                long loadCount = collectionStatistics.getLoadCount();
                long updateCount = collectionStatistics.getUpdateCount();
                if ( fetchCount > 0 || loadCount > 0 || updateCount > 0 ) {
                    buf.append( "Collection of role " + string + ": " + fetchCount + " fetches, " + loadCount
                            + " loads, " + updateCount + " updates\n" );
                }
            }
        }

        if ( showEntityStats ) {
            String[] entityNames = stats.getEntityNames();
            for ( String string : entityNames ) {
                EntityStatistics entityStats = stats.getEntityStatistics( string );
                long changes = entityStats.getInsertCount() + entityStats.getUpdateCount()
                        + entityStats.getDeleteCount();
                if ( changes > 0 ) {
                    String shortName;
                    try {
                        shortName = Class.forName( string ).getSimpleName().replaceFirst( "Impl", "" );
                        buf.append( shortName + " changed " + changes + " \n" );
                    } catch ( ClassNotFoundException e ) {
                        log.error( e, e );
                    }
                }
                long reads = entityStats.getLoadCount();
                if ( reads > 0 ) {
                    String shortName;
                    try {
                        shortName = Class.forName( string ).getSimpleName().replaceFirst( "Impl", "" );
                        buf.append( shortName + " read " + reads + " \n" );
                    } catch ( ClassNotFoundException e ) {
                        log.error( e, e );
                    }
                }

            }
        }

        buf.append( "----------------------------------------------------------\n" );
        log.info( buf );

    }

    public void setSessionFactory( SessionFactory sessionFactory ) {
        this.sessionFactory = sessionFactory;
    }
}
