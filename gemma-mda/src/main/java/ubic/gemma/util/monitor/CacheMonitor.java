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

/**
 * Get statistics about object caches.
 * 
 * @author paul
 * @version $Id$
 */
public class CacheMonitor {

    /**
     * @return
     */
    public static String getStats() {

        CacheManager cm = CacheManager.getInstance();
        StringBuilder buf = new StringBuilder();
        String[] cacheNames = cm.getCacheNames();
        Arrays.sort( cacheNames );

        buf.append( cacheNames.length + " caches" );

        buf.append( "<table style='font-size:small'><tr>" );
        buf.append( "<th>Name</th><th>Hits</th><th>Misses</th><th>Count</th><th>MemHits</th><th>DiskHits</th>" );
        buf.append( "<th>Eternal?</th><th>UseDisk?</th> <th>MaxInMem</th><th>LifeTime</th><th>IdleTime</th>" );
        buf.append( "</tr>" );

        for ( String cacheName : cacheNames ) {
            Cache cache = cm.getCache( cacheName );
            Statistics statistics = cache.getStatistics();

            int objectCount = statistics.getObjectCount();

            if ( objectCount == 0 ) {
                continue;
            }

            buf.append( "<tr><td>" + cacheName + "</td>" );
            int hits = statistics.getCacheHits();
            int misses = statistics.getCacheMisses();
            int inMemoryHits = statistics.getInMemoryHits();
            int onDiskHits = statistics.getOnDiskHits();

            buf.append( "<td>" + hits + "</td>" );
            buf.append( "<td>" + misses + "</td>" );
            buf.append( "<td>" + objectCount + "</td>" );
            buf.append( "<td>" + inMemoryHits + "</td>" );
            buf.append( "<td>" + onDiskHits + "</td>" );

            buf.append( "<td>" + ( cache.isEternal() ? "&bull;" : "" ) + "</td>" );
            buf.append( "<td>" + ( cache.isOverflowToDisk() ? "&bull;" : "" ) + "</td>" );
            buf.append( "<td>" + cache.getMaxElementsInMemory() + "</td>" );

            buf.append( "<td>" + cache.getTimeToIdleSeconds() + "</td>" );
            buf.append( "<td>" + cache.getTimeToLiveSeconds() + "</td>" );
            buf.append( "</tr>" );
        }
        buf.append( "</table>" );
        return buf.toString();

    }

}
