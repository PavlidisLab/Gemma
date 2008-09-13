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

import ubic.gemma.util.ConfigUtils;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

/**
 * Configures the cache for data vectors.
 * 
 * @author paul
 * @version $Id$
 */
public class ProcessedDataVectorCache {

    private static final String PROCESSED_DATA_VECTOR_CACHE_NAME = "ProcessedDataVectorCache";
    private static final int PROCESSED_DATA_VECTOR_CACHE_DEFAULT_MAX_ELEMENTS = 100000;
    private static final int PROCESSED_DATA_VECTOR_CACHE_DEFAULT_TIME_TO_LIVE = 10000;
    private static final int PROCESSED_DATA_VECTOR_CACHE_DEFAULT_TIME_TO_IDLE = 10000;
    private static final boolean PROCESSED_DATA_VECTOR_CACHE_DEFAULT_ETERNAL = true;
    private static final boolean PROCESSED_DATA_VECTOR_CACHE_DEFAULT_OVERFLOW_TO_DISK = true;

    private ProcessedDataVectorCache() {
    }

    /**
     * Remove all elements from the cache.
     */
    public static void clearCache() {
        CacheManager manager = CacheManager.getInstance();
        manager.getCache( PROCESSED_DATA_VECTOR_CACHE_NAME ).removeAll();
    }

    /**
     * Initialize the vector cache; if it already exists it will not be recreated.
     * 
     * @return
     */
    public static Cache initializeCache() {

        int maxElements = ConfigUtils.getInt( "gemma.cache.vectors.maxelements",
                PROCESSED_DATA_VECTOR_CACHE_DEFAULT_MAX_ELEMENTS );
        int timeToLive = ConfigUtils.getInt( "gemma.cache.vectors.timetolive",
                PROCESSED_DATA_VECTOR_CACHE_DEFAULT_TIME_TO_LIVE );
        int timeToIdle = ConfigUtils.getInt( "gemma.cache.vectors.timetoidle",
                PROCESSED_DATA_VECTOR_CACHE_DEFAULT_TIME_TO_IDLE );

        boolean overFlowToDisk = ConfigUtils.getBoolean( "gemma.cache.vectors.usedisk",
                PROCESSED_DATA_VECTOR_CACHE_DEFAULT_OVERFLOW_TO_DISK );

        boolean eternal = ConfigUtils.getBoolean( "gemma.cache.vectors.eternal",
                PROCESSED_DATA_VECTOR_CACHE_DEFAULT_ETERNAL );

        /*
         * Create a cache for the probe data.s
         */
        CacheManager manager = CacheManager.getInstance();

        if ( manager.cacheExists( PROCESSED_DATA_VECTOR_CACHE_NAME ) ) {
            return manager.getCache( PROCESSED_DATA_VECTOR_CACHE_NAME );
        }

        Cache cache = new Cache( PROCESSED_DATA_VECTOR_CACHE_NAME, maxElements, overFlowToDisk, eternal, timeToLive,
                timeToIdle );

        manager.addCache( cache );
        return manager.getCache( PROCESSED_DATA_VECTOR_CACHE_NAME );
    }

}
