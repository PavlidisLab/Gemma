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

import java.lang.management.ManagementFactory;
import java.util.Arrays;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.management.ManagementService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hibernate.jmx.StatisticsService;
import org.hibernate.stat.CollectionStatistics;
import org.hibernate.stat.EntityStatistics;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Monitoring of Hibernate status.
 * 
 * @spring.bean id="hibernateMonitor"
 * @spring.property name="sessionFactory" ref="sessionFactory"
 * @author pavlidis
 * @version $Id$
 */
public class HibernateMonitor implements InitializingBean, DisposableBean {

    /**
     * 
     */
    private static final String HIBERNATE_MBEAN_OBJECTNAME = "Hibernate:type=statistics,application=Gemma";

    private static Log log = LogFactory.getLog( HibernateMonitor.class.getName() );

    SessionFactory sessionFactory;

    private boolean showQueryCacheStats = true;

    /**
     * Log some statistics.
     */
    public String getStats() {
        return getStats( false, false, false );
    }

    /**
     * Log some statistics. Parameters control the section that are populated.
     * 
     * @param showEntityStats
     * @param showCollectionStats
     * @param showSecondLevelCacheDetails
     */
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

    public void setSessionFactory( SessionFactory sessionFactory ) {
        this.sessionFactory = sessionFactory;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {

        // Define ObjectName of the MBean
        ObjectName on = new ObjectName( HIBERNATE_MBEAN_OBJECTNAME );

        // Enable Hibernate JMX Statistics
        StatisticsService statsMBean = new StatisticsService();
        statsMBean.setSessionFactory( this.sessionFactory );
        statsMBean.setStatisticsEnabled( true );
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        try {
            mbs.getMBeanInfo( on );
        } catch ( InstanceNotFoundException e ) {
            try {
                mbs.registerMBean( statsMBean, on );
            } catch ( InstanceAlreadyExistsException e1 ) {
                // no problem
            }
        }

        /**
         * Enable Ehcache JMX Statistics Use CacheManager.getInstance() instead of new CacheManager() as
         * net.sf.ehcache.hibernate.SingletonEhCacheProvider is used to ensure reference to the same CacheManager
         * instance as used by Hibernate
         */
        try {
            CacheManager cacheMgr = CacheManager.getInstance();
            ManagementService.registerMBeans( cacheMgr, mbs, true, true, true, true );
        } catch ( RuntimeException e1 ) {
            // no problem, it already exists...
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.DisposableBean#destroy()
     */
    public void destroy() throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName on = new ObjectName( HIBERNATE_MBEAN_OBJECTNAME );
        mbs.unregisterMBean( on );

        // CacheManager cacheMgr = CacheManager.getInstance();

    }
}
