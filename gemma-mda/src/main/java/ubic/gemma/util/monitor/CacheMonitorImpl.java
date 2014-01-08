/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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

import java.util.Arrays;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Statistics;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.TerracottaClientConfiguration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Get statistics about and manage caches.
 * 
 * @author paul
 * @version $Id$
 */
@Component
public class CacheMonitorImpl implements CacheMonitor {

    private static Log log = LogFactory.getLog( CacheMonitorImpl.class );

    @Autowired
    private CacheManager cacheManager;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.monitor.CacheMonitor#clearAllCaches()
     */
    @Override
    public void clearAllCaches() {
        log.info( "Clearing all caches" );
        cacheManager.clearAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.monitor.CacheMonitor#clearCache(java.lang.String)
     */
    @Override
    public void clearCache( String cacheName ) {
        Cache cache = this.cacheManager.getCache( cacheName );
        if ( cache != null ) {
            cache.removeAll();
            log.info( "Cleared cache: " + cache.getName() );
        } else {
            throw new IllegalArgumentException( "No cache found with name=" + cacheName );
        }
    }

    @Override
    public void disableStatistics() {
        log.info( "Disabling statistics" );
        setStatisticsEnabled( false );
    }

    @Override
    public void enableStatistics() {
        log.info( "Enabling statistics" );
        setStatisticsEnabled( true );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.monitor.CacheMonitor#getStats()
     */
    @Override
    public String getStats() {

        StringBuilder buf = new StringBuilder();
        String[] cacheNames = cacheManager.getCacheNames();
        Arrays.sort( cacheNames );

        // Terracotta clustered?
        TerracottaClientConfiguration terracottaConfig = cacheManager.getConfiguration().getTerracottaConfiguration();
        buf.append( "Distributed caching is " );
        buf.append( terracottaConfig != null ? "enabled" : "disabled" );
        buf.append( " in the configuration file" );
        buf.append( terracottaConfig != null ? ". The cache server's configuration URL is at ["
                + terracottaConfig.getUrl() + "]" : "" );
        buf.append( ".<br/>" );
        
        buf.append( cacheNames.length + " caches; only non-empty caches listed below." );
        // FIXME make these sortable.
        buf.append( "<br/>&nbsp;To clear all caches click here: <img src='/Gemma/images/icons/arrow_rotate_anticlockwise.png' onClick=\"clearAllCaches()\" alt='Flush caches' title='Clear caches' />&nbsp;&nbsp;" );
        buf.append( "<br/>&nbsp;To start statistics collection click here: <img src='/Gemma/images/icons/arrow_rotate_anticlockwise.png' onClick=\"enableStatistics()\" alt='Enable stats' title='Enable stats' />&nbsp;&nbsp;" );
        buf.append( "<br/>&nbsp;To stop statistics collection click here: <img src='/Gemma/images/icons/arrow_rotate_anticlockwise.png' onClick=\"disableStatistics()\" alt='Disable stats' title='Disable stats' />&nbsp;&nbsp;" );

        buf.append( "<table style='font-size:small'  ><tr>" );
        String header = "<th>Name</th><th>HitRate</th><th>Hits</th><th>Misses</th><th>Count</th><th>MemHits</th><th>MemMiss</th><th>DiskHits</th><th>Evicted</th> <th>Eternal?</th><th>UseDisk?</th> <th>MaxInMem</th><th>LifeTime</th><th>IdleTime</th>";
        buf.append( header );
        buf.append( "</tr>" );

        int count = 0;
        for ( String rawCacheName : cacheNames ) {
            Cache cache = cacheManager.getCache( rawCacheName );
            Statistics statistics = cache.getStatistics();

            long objectCount = statistics.getObjectCount();

            if ( objectCount == 0 ) {
                continue;
            }

            // a little shorter...
            String cacheName = rawCacheName.replaceFirst( "ubic.gemma.model.", "u.g.m." );

            buf.append( "<tr><td>" + getClearCacheHtml( rawCacheName ) + cacheName + "</td>" );
            long hits = statistics.getCacheHits();
            long misses = statistics.getCacheMisses();
            long inMemoryHits = statistics.getInMemoryHits();
            long inMemoryMisses = statistics.getInMemoryMisses();

            long onDiskHits = statistics.getOnDiskHits();
            long evictions = statistics.getEvictionCount();

            if ( hits + misses > 0 ) {

                buf.append( makeTableCellForStat( String.format( "%.2f", ( double ) hits / ( hits + misses ) ) ) );
            } else {
                buf.append( "<td></td>" );
            }
            buf.append( makeTableCellForStat( hits ) );

            buf.append( makeTableCellForStat( misses ) );
            buf.append( makeTableCellForStat( objectCount ) );
            buf.append( makeTableCellForStat( inMemoryHits ) );
            buf.append( makeTableCellForStat( inMemoryMisses ) );
            buf.append( makeTableCellForStat( onDiskHits ) );
            buf.append( makeTableCellForStat( evictions ) );

            CacheConfiguration cacheConfiguration = cache.getCacheConfiguration();
            boolean eternal = cacheConfiguration.isEternal();
            buf.append( "<td>" + ( eternal ? "&bull;" : "" ) + "</td>" );
            buf.append( "<td>" + ( cacheConfiguration.isOverflowToDisk() ? "&bull;" : "" ) + "</td>" );
            buf.append( "<td>" + cacheConfiguration.getMaxEntriesLocalHeap() + "</td>" );

            if ( eternal ) {
                // timeouts are irrelevant.
                buf.append( "<td>-</td>" );
                buf.append( "<td>-</td>" );
            } else {
                buf.append( "<td>" + cacheConfiguration.getTimeToIdleSeconds() + "</td>" );
                buf.append( "<td>" + cacheConfiguration.getTimeToLiveSeconds() + "</td>" );
            }
            buf.append( "</tr>" );

            if ( ++count % 25 == 0 ) {
                buf.append( "<tr>" + header + "</tr>" );
            }
        }
        buf.append( "</table>" );
        return buf.toString();

    }

    private String getClearCacheHtml( String cacheName ) {
        return "<img src='/Gemma/images/icons/arrow_rotate_anticlockwise.png' onClick=\"clearCache('" + cacheName
                + "')\" alt='Clear cache' title='Clear cache' />&nbsp;&nbsp;";
    }

    /**
     * @param hits
     * @return
     */
    private String makeTableCellForStat( long hits ) {
        return "<td>" + ( hits > 0 ? hits : "" ) + "</td>";
    }

    private String makeTableCellForStat( String s ) {
        return "<td>" + s + "</td>";
    }

    private synchronized void setStatisticsEnabled( boolean b ) {
        String[] cacheNames = cacheManager.getCacheNames();

        for ( String rawCacheName : cacheNames ) {
            Cache cache = cacheManager.getCache( rawCacheName );
            cache.setSampledStatisticsEnabled( b );
        }
    }

}
