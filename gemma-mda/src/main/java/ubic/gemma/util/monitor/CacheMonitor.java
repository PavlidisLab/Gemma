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

/**
 * Get statistics about object caches.
 * 
 * @author paul
 * @version $Id$
 */
public class CacheMonitor {

    private CacheManager cacheManager;

    /**
     * @param cacheManager the cacheManager to set
     */
    public void setCacheManager( CacheManager cacheManager ) {
        this.cacheManager = cacheManager;
    }

    /**
     * @return
     */
    public String getStats() {

        StringBuilder buf = new StringBuilder();
        String[] cacheNames = cacheManager.getCacheNames();
        Arrays.sort( cacheNames );

        buf.append( cacheNames.length + " caches; only non-empty caches listed below." );

        buf.append( "<table style='font-size:small'  ><tr>" );
        String header = "<th>Name</th><th>Hits</th><th>Misses</th><th>Count</th><th>MemHits</th><th>DiskHits</th><th>Evicted</th> <th>Eternal?</th><th>UseDisk?</th> <th>MaxInMem</th><th>LifeTime</th><th>IdleTime</th>";
        buf.append( header );
        buf.append( "</tr>" );

        int count = 0;
        for ( String cacheName : cacheNames ) {
            Cache cache = cacheManager.getCache( cacheName );
            Statistics statistics = cache.getStatistics();

            long objectCount = statistics.getObjectCount();

            if ( objectCount == 0 ) {
                continue;
            }

            // a little shorter...
            cacheName = cacheName.replaceFirst( "ubic.gemma.model.", "[entity] " );

            buf.append( "<tr><td>" + cacheName + "</td>" );
            long hits = statistics.getCacheHits();
            long misses = statistics.getCacheMisses();
            long inMemoryHits = statistics.getInMemoryHits();
            long onDiskHits = statistics.getOnDiskHits();
            long evictions = statistics.getEvictionCount();

            buf.append( "<td>" + hits + "</td>" );
            buf.append( "<td>" + misses + "</td>" );
            buf.append( "<td>" + objectCount + "</td>" );
            buf.append( "<td>" + inMemoryHits + "</td>" );
            buf.append( "<td>" + onDiskHits + "</td>" );
            buf.append( "<td>" + evictions + "</td>" );

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

}
