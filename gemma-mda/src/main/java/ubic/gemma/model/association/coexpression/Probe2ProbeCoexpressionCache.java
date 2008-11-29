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
package ubic.gemma.model.association.coexpression;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import ubic.gemma.model.genome.CoexpressionCacheValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.util.ConfigUtils;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

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
public class Probe2ProbeCoexpressionCache {

    private static final String PROCESSED_DATA_VECTOR_CACHE_NAME = "Probe2ProbeCache";
    private static final int PROCESSED_DATA_VECTOR_CACHE_DEFAULT_MAX_ELEMENTS = 100000;
    private static final int PROCESSED_DATA_VECTOR_CACHE_DEFAULT_TIME_TO_LIVE = 10000;
    private static final int PROCESSED_DATA_VECTOR_CACHE_DEFAULT_TIME_TO_IDLE = 10000;
    private static final boolean PROCESSED_DATA_VECTOR_CACHE_DEFAULT_ETERNAL = true;
    private static final boolean PROCESSED_DATA_VECTOR_CACHE_DEFAULT_OVERFLOW_TO_DISK = true;

    /**
     * We retain references to the caches separately from the CacheManager. This _could_ create leaks of caches if the
     * cache manager needs to recreate a cache for some reason. Something to keep in mind.
     */
    private static final Map<Long, Cache> caches = new HashMap<Long, Cache>();

    private static String getCacheName( Long id ) {
        return PROCESSED_DATA_VECTOR_CACHE_NAME + "_" + id;
    }

    /**
     * Remove all elements from the cache for the given expression experiment, if the cache exists.
     * 
     * @param e the expression experiment - specific cache to be cleared.
     */
    public static void clearCache( Long e ) {
        CacheManager manager = CacheManager.getInstance();
        Cache cache = manager.getCache( getCacheName( e ) );
        if ( cache != null ) cache.removeAll();
    }

    /**
     * 
     */
    public static void clearAllCaches() {
        for ( Long e : caches.keySet() ) {
            clearCache( e );
        }
    }

    /**
     * @return
     */
    public static Collection<Cache> getAllCaches() {
        return caches.values();
    }

    /**
     * @param eeID
     * @param coExVOForCache
     */
    @SuppressWarnings("unchecked")
    public static void addToCache( Long eeID, CoexpressionCacheValueObject coExVOForCache ) {

        Cache c = getCache( eeID );

        Gene queryGene = coExVOForCache.getQueryGene();

        Element element = c.get( queryGene );
        if ( element != null ) {
            ( ( Collection<CoexpressionCacheValueObject> ) element.getObjectValue() ).add( coExVOForCache );
        } else {
            Collection<CoexpressionCacheValueObject> cachedValues = new HashSet<CoexpressionCacheValueObject>();
            cachedValues.add( coExVOForCache );
            c.put( new Element( queryGene, cachedValues ) );
        }
    }

    /**
     * @param eeID
     * @param queryGene
     * @return null if there are no cached results.
     */
    @SuppressWarnings("unchecked")
    public static Collection<CoexpressionCacheValueObject> retrieve( Long eeID, Gene queryGene ) {
        Cache c = getCache( eeID );
        Element element = c.get( queryGene );
        if ( element != null ) {
            return ( Collection<CoexpressionCacheValueObject> ) element.getValue();
        }
        return null;

    }

    /**
     * Get the vector cache for a particular experiment
     * 
     * @param e
     * @return
     */
    public static Cache getCache( Long e ) {
        if ( !caches.containsKey( e ) ) {
            initializeCache( e );
        }
        return caches.get( e );
    }

    /**
     * Initialize the vector cache; if it already exists it will not be recreated.
     * 
     * @return
     */
    private static void initializeCache( Long e ) {

        if ( caches.containsKey( e ) ) {
            return;
        }

        int maxElements = ConfigUtils.getInt( "gemma.cache.probe2probe.maxelements",
                PROCESSED_DATA_VECTOR_CACHE_DEFAULT_MAX_ELEMENTS );
        int timeToLive = ConfigUtils.getInt( "gemma.cache.probe2probe.timetolive",
                PROCESSED_DATA_VECTOR_CACHE_DEFAULT_TIME_TO_LIVE );
        int timeToIdle = ConfigUtils.getInt( "gemma.cache.probe2probe.timetoidle",
                PROCESSED_DATA_VECTOR_CACHE_DEFAULT_TIME_TO_IDLE );

        boolean overFlowToDisk = ConfigUtils.getBoolean( "gemma.cache.probe2probe.usedisk",
                PROCESSED_DATA_VECTOR_CACHE_DEFAULT_OVERFLOW_TO_DISK );

        boolean eternal = ConfigUtils.getBoolean( "gemma.cache.probe2probe.eternal",
                PROCESSED_DATA_VECTOR_CACHE_DEFAULT_ETERNAL );

        String cacheName = getCacheName( e );

        CacheManager manager = CacheManager.getInstance();

        if ( !manager.cacheExists( cacheName ) ) {

            manager.addCache( new Cache( cacheName, maxElements, overFlowToDisk, eternal, timeToLive, timeToIdle ) );
        }
        caches.put( e, manager.getCache( cacheName ) );

    }

}
