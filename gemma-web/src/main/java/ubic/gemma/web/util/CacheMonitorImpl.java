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
package ubic.gemma.web.util;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Statistics;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.persistence.util.Settings;

import java.util.Arrays;

/**
 * Get statistics about and manage caches.
 *
 * @author paul
 */
@Component
public class CacheMonitorImpl implements CacheMonitor {

    private static final Log log = LogFactory.getLog( CacheMonitorImpl.class );

    @Autowired
    private CacheManager cacheManager;

    @Override
    public void clearAllCaches() {
        CacheMonitorImpl.log.info( "Clearing all caches" );
        cacheManager.clearAll();
    }

    @Override
    public void clearCache( String cacheName ) {
        Cache cache = this.cacheManager.getCache( cacheName );
        if ( cache != null ) {
            cache.removeAll();
            CacheMonitorImpl.log.info( "Cleared cache: " + cache.getName() );
        } else {
            throw new IllegalArgumentException( "No cache found with name=" + cacheName );
        }
    }

    @Override
    public void disableStatistics() {
        CacheMonitorImpl.log.info( "Disabling statistics" );
        this.setStatisticsEnabled( false );
    }

    @Override
    public void enableStatistics() {
        CacheMonitorImpl.log.info( "Enabling statistics" );
        this.setStatisticsEnabled( true );

    }

    @Override
    public String getStats() {

        StringBuilder buf = new StringBuilder();
        String[] cacheNames = cacheManager.getCacheNames();
        Arrays.sort( cacheNames );

        for ( String cacheName : cacheNames ) {
            // Terracotta clustered?
            boolean isTerracottaClustered = cacheManager.getCache( cacheName ).getCacheConfiguration().isTerracottaClustered();
            buf.append( "Distributed caching is " );
            buf.append( isTerracottaClustered ? "enabled" : "disabled" );
            buf.append( " in the configuration file for " + cacheName );
            buf.append( ".<br/>" );
        }

        buf.append( cacheNames.length ).append( " caches; only non-empty caches listed below." );
        // FIXME make these sortable.
        buf.append( "<br/>&nbsp;To clear all caches click here: <img src='" + Settings.getRootContext()
                + "/images/icons/arrow_rotate_anticlockwise.png' onClick=\"clearAllCaches()\" alt='Flush caches' title='Clear caches' />&nbsp;&nbsp;" );
        buf.append( "<br/>&nbsp;To start statistics collection click here: <img src='" + Settings.getRootContext()
                + "/images/icons/arrow_rotate_anticlockwise.png' onClick=\"enableStatistics()\" alt='Enable stats' title='Enable stats' />&nbsp;&nbsp;" );
        buf.append( "<br/>&nbsp;To stop statistics collection click here: <img src='" + Settings.getRootContext()
                + "/images/icons/arrow_rotate_anticlockwise.png' onClick=\"disableStatistics()\" alt='Disable stats' title='Disable stats' />&nbsp;&nbsp;" );

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

            buf.append( "<tr><td>" ).append( this.getClearCacheHtml( rawCacheName ) ).append( cacheName )
                    .append( "</td>" );
            long hits = statistics.getCacheHits();
            long misses = statistics.getCacheMisses();
            long inMemoryHits = statistics.getInMemoryHits();
            long inMemoryMisses = statistics.getInMemoryMisses();

            long onDiskHits = statistics.getOnDiskHits();
            long evictions = statistics.getEvictionCount();

            if ( hits + misses > 0 ) {

                buf.append( this.makeTableCellForStat( String.format( "%.2f", ( double ) hits / ( hits + misses ) ) ) );
            } else {
                buf.append( "<td></td>" );
            }
            buf.append( this.makeTableCellForStat( hits ) );

            buf.append( this.makeTableCellForStat( misses ) );
            buf.append( this.makeTableCellForStat( objectCount ) );
            buf.append( this.makeTableCellForStat( inMemoryHits ) );
            buf.append( this.makeTableCellForStat( inMemoryMisses ) );
            buf.append( this.makeTableCellForStat( onDiskHits ) );
            buf.append( this.makeTableCellForStat( evictions ) );

            CacheConfiguration cacheConfiguration = cache.getCacheConfiguration();
            boolean eternal = cacheConfiguration.isEternal();
            buf.append( "<td>" ).append( eternal ? "&bull;" : "" ).append( "</td>" );

            buf.append( "<td>" ).append( cacheConfiguration.getMaxElementsInMemory() ).append( "</td>" );

            if ( eternal ) {
                // timeouts are irrelevant.
                buf.append( "<td>-</td>" );
                buf.append( "<td>-</td>" );
            } else {
                buf.append( "<td>" ).append( cacheConfiguration.getTimeToIdleSeconds() ).append( "</td>" );
                buf.append( "<td>" ).append( cacheConfiguration.getTimeToLiveSeconds() ).append( "</td>" );
            }
            buf.append( "</tr>" );

            if ( ++count % 25 == 0 ) {
                buf.append( "<tr>" ).append( header ).append( "</tr>" );
            }
        }
        buf.append( "</table>" );
        return buf.toString();

    }

    private String getClearCacheHtml( String cacheName ) {
        return "<img src='" + Settings.getRootContext()
                + "/images/icons/arrow_rotate_anticlockwise.png' onClick=\"clearCache('" + cacheName
                + "')\" alt='Clear cache' title='Clear cache' />&nbsp;&nbsp;";
    }

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
