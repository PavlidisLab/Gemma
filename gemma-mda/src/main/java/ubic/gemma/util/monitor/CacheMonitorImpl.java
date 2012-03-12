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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Statistics;

import net.sf.ehcache.config.CacheConfiguration;

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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.monitor.CacheMonitor#clearAllCaches()
     */
    @Override
    public void clearAllCaches() {
        cacheManager.clearAll();
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

        buf.append( cacheNames.length + " caches; only non-empty caches listed below." );
        buf.append( "&nbsp;To clear all caches click here: <img src='/Gemma/images/icons/arrow_rotate_anticlockwise.png' onClick=\"clearAllCaches()\" alt='Flush caches' title='Clear caches' />&nbsp;&nbsp;" );
        buf.append( "<table style='font-size:small'  ><tr>" );
        String header = "<th>Name</th><th>Hits</th><th>Misses</th><th>Count</th><th>MemHits</th><th>DiskHits</th><th>Evicted</th> <th>Eternal?</th><th>UseDisk?</th> <th>MaxInMem</th><th>LifeTime</th><th>IdleTime</th>";
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
            String cacheName = rawCacheName.replaceFirst( "ubic.gemma.model.", "[entity] " );

            buf.append( "<tr><td>" + getClearCacheHtml( rawCacheName ) + cacheName + "</td>" );
            long hits = statistics.getCacheHits();
            long misses = statistics.getCacheMisses();
            long inMemoryHits = statistics.getInMemoryHits();
            long onDiskHits = statistics.getOnDiskHits();
            long evictions = statistics.getEvictionCount();

            buf.append( "<td>" + ( hits > 0 ? hits : "" ) + "</td>" );
            buf.append( "<td>" + ( misses > 0 ? misses : "" ) + "</td>" );
            buf.append( "<td>" + ( objectCount > 0 ? objectCount : "" ) + "</td>" );
            buf.append( "<td>" + ( inMemoryHits > 0 ? inMemoryHits : "" ) + "</td>" );
            buf.append( "<td>" + ( onDiskHits > 0 ? onDiskHits : "" ) + "</td>" );
            buf.append( "<td>" + ( evictions > 0 ? evictions : "" ) + "</td>" );

            CacheConfiguration cacheConfiguration = cache.getCacheConfiguration();
            boolean eternal = cacheConfiguration.isEternal();
            buf.append( "<td>" + ( eternal ? "&bull;" : "" ) + "</td>" );
            buf.append( "<td>" + ( cacheConfiguration.isOverflowToDisk() ? "&bull;" : "" ) + "</td>" );
            buf.append( "<td>" + cacheConfiguration.getMaxElementsInMemory() + "</td>" );

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

}
