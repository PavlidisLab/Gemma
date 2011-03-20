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
package ubic.gemma.model.expression.bioAssayData;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.util.ConfigUtils;

/**
 * Configures the cache for data vectors.
 * <p>
 * Implementation note: This uses ehCache. I have decided to make one cache per expression experiment. The reason for
 * this is that having complex keys for cached Elements based on expression experiment AND gene makes it difficult to
 * invalidate the cache when an expression experiment's data changes. The drawback is that there are potentially
 * hundreds of caches; I don't know if there are any performance considerations there.
 * 
 * @author paul
 * @version $Id$
 */
@Component
public class ProcessedDataVectorCache {

    private static final String VECTOR_CACHE_NAME_BASE = "DataVectorCache";
    private static final int VECTOR_CACHE_DEFAULT_MAX_ELEMENTS = 100000;
    private static final int VECTOR_CACHE_DEFAULT_TIME_TO_LIVE = 10000;
    private static final int VECTOR_CACHE_DEFAULT_TIME_TO_IDLE = 10000;
    private static final boolean VECTOR_CACHE_DEFAULT_ETERNAL = true;
    private static final boolean VECTOR_CACHE_DEFAULT_OVERFLOW_TO_DISK = true;

    /**
     * We retain references to the caches separately from the CacheManager. This _could_ create leaks of caches if the
     * cache manager needs to recreate a cache for some reason. Something to keep in mind.
     */
    private static final Map<Long /* EE id */, Cache> caches = new HashMap<Long, Cache>();

    @Autowired
    private CacheManager cacheManager;

    /**
     * 
     */
    public void clearAllCaches() {
        for ( Long eeid : caches.keySet() ) {
            clearCache( eeid );
        }
    }

    /**
     * Remove all elements from the cache for the given expression experiment, if the cache exists.
     * 
     * @param e the expression experiment - specific cache to be cleared.
     */
    public void clearCache( Long eeid ) {
        Cache cache = cacheManager.getCache( getCacheName( eeid ) );
        if ( cache != null ) cache.removeAll();
    }

    /**
     * @return
     */
    public Collection<Cache> getAllCaches() {
        return caches.values();
    }

    /**
     * Get the coexpression cache for a particular experiment
     * 
     * @param e
     * @return
     */
    public Cache getCache( Long eeid ) {
        if ( !caches.containsKey( eeid ) ) {
            initializeCache( eeid );
        }
        return caches.get( eeid );
    }

    /**
     * @param cacheManager the cacheManager to set
     */
    public void setCacheManager( CacheManager cacheManager ) {
        this.cacheManager = cacheManager;
    }

    private String getCacheName( Long eeid ) {
        return VECTOR_CACHE_NAME_BASE + "_" + eeid;
    }

    /**
     * Initialize the vector cache; if it already exists it will not be recreated.
     * 
     * @return
     */
    private void initializeCache( Long eeid ) {

        if ( caches.containsKey( eeid ) ) {
            return;
        }

        int maxElements = ConfigUtils.getInt( "gemma.cache.vectors.maxelements", VECTOR_CACHE_DEFAULT_MAX_ELEMENTS );
        int timeToLive = ConfigUtils.getInt( "gemma.cache.vectors.timetolive", VECTOR_CACHE_DEFAULT_TIME_TO_LIVE );
        int timeToIdle = ConfigUtils.getInt( "gemma.cache.vectors.timetoidle", VECTOR_CACHE_DEFAULT_TIME_TO_IDLE );

        boolean overFlowToDisk = ConfigUtils.getBoolean( "gemma.cache.vectors.usedisk",
                VECTOR_CACHE_DEFAULT_OVERFLOW_TO_DISK );

        boolean eternal = ConfigUtils.getBoolean( "gemma.cache.vectors.eternal", VECTOR_CACHE_DEFAULT_ETERNAL );

        String cacheName = getCacheName( eeid );

        boolean diskPersistent = ConfigUtils.getBoolean( "gemma.cache.diskpersistent", false );

        if ( !cacheManager.cacheExists( cacheName ) ) {

            cacheManager.addCache( new Cache( cacheName, maxElements, MemoryStoreEvictionPolicy.LRU, overFlowToDisk,
                    null, eternal, timeToLive, timeToIdle, diskPersistent, 600 /* diskExpiryThreadInterval */, null ) );
        }

        caches.put( eeid, cacheManager.getCache( cacheName ) );

    }
}
