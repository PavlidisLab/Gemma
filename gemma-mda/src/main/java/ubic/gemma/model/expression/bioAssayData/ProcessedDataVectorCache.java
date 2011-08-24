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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Gene;
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
public class ProcessedDataVectorCache implements InitializingBean {

    private static final String VECTOR_CACHE_NAME = "ProcessedExpressionDataVectorCache";
    private static final int VECTOR_CACHE_DEFAULT_MAX_ELEMENTS = 100000;
    private static final int VECTOR_CACHE_DEFAULT_TIME_TO_LIVE = 10000;
    private static final int VECTOR_CACHE_DEFAULT_TIME_TO_IDLE = 10000;
    private static final boolean VECTOR_CACHE_DEFAULT_ETERNAL = true;
    private static final boolean VECTOR_CACHE_DEFAULT_OVERFLOW_TO_DISK = true;

    /**
     * We retain references to the caches separately from the CacheManager. This _could_ create leaks of caches if the
     * cache manager needs to recreate a cache for some reason. Something to keep in mind.
     */
    // private static final Map<Long /* EE id */, Cache> caches = new HashMap<Long, Cache>();

    private Cache cache;

    @Autowired
    private CacheManager cacheManager;

    /**
     * 
     */
    public void clearAllCaches() {
        // for ( Long eeid : caches.keySet() ) {
        // clearCache( eeid );
        // }
        clearCache();
    }

    public void clearCache() {
        cache.removeAll();
    }

    /**
     * @param cacheManager the cacheManager to set
     */
    public void setCacheManager( CacheManager cacheManager ) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        int maxElements = ConfigUtils.getInt( "gemma.cache.vectors.maxelements", VECTOR_CACHE_DEFAULT_MAX_ELEMENTS );
        int timeToLive = ConfigUtils.getInt( "gemma.cache.vectors.timetolive", VECTOR_CACHE_DEFAULT_TIME_TO_LIVE );
        int timeToIdle = ConfigUtils.getInt( "gemma.cache.vectors.timetoidle", VECTOR_CACHE_DEFAULT_TIME_TO_IDLE );

        boolean overFlowToDisk = ConfigUtils.getBoolean( "gemma.cache.vectors.usedisk",
                VECTOR_CACHE_DEFAULT_OVERFLOW_TO_DISK );

        boolean eternal = ConfigUtils.getBoolean( "gemma.cache.vectors.eternal", VECTOR_CACHE_DEFAULT_ETERNAL );

        String cacheName = VECTOR_CACHE_NAME;

        boolean diskPersistent = ConfigUtils.getBoolean( "gemma.cache.diskpersistent", false );

        if ( !cacheManager.cacheExists( cacheName ) ) {

            cacheManager.addCache( new Cache( cacheName, maxElements, MemoryStoreEvictionPolicy.LRU, overFlowToDisk,
                    null, eternal, timeToLive, timeToIdle, diskPersistent, 600 /* diskExpiryThreadInterval */, null ) );
        }

        this.cache = cacheManager.getCache( VECTOR_CACHE_NAME );

    }

    /**
     * @param ee
     * @param g
     * @return
     */
    @SuppressWarnings("unchecked")
    public Collection<DoubleVectorValueObject> get( BioAssaySet ee, Gene g ) {
        return ( Collection<DoubleVectorValueObject> ) cache.get( new CacheKey( ee.getId(), g.getId() ) ).getValue();
    }

    /**
     * @param eeid
     * @param g
     * @param collection
     */
    public void addToCache( Long eeid, Gene g, Collection<DoubleVectorValueObject> collection ) {
        cache.put( new Element( new CacheKey( eeid, g.getId() ), collection ) );
    }

    /**
     * Remove cached items for experiment with given id.
     * 
     * @param eeid
     */
    public void clearCache( Long eeid ) {
        for ( Object o : cache.getKeys() ) {
            CacheKey k = ( CacheKey ) o;
            if ( k.eeid.equals( eeid ) ) {
                cache.remove( k );
            }
        }
    }
}

class CacheKey {

    Long eeid;
    Long geneId;

    CacheKey( Long eeid, Long geneId ) {
        this.eeid = eeid;
        this.geneId = geneId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( eeid == null ) ? 0 : eeid.hashCode() );
        result = prime * result + ( ( geneId == null ) ? 0 : geneId.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        CacheKey other = ( CacheKey ) obj;
        if ( eeid == null ) {
            if ( other.eeid != null ) return false;
        } else if ( !eeid.equals( other.eeid ) ) return false;
        if ( geneId == null ) {
            if ( other.geneId != null ) return false;
        } else if ( !geneId.equals( other.geneId ) ) return false;
        return true;
    }

}