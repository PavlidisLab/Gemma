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
package ubic.gemma.persistence.service.expression.bioAssayData;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.persistence.util.CacheUtils;
import ubic.gemma.persistence.util.Settings;

import java.io.Serializable;
import java.util.Collection;

/**
 * Configures the cache for data vectors.
 * Implementation note: This uses ehCache. I have decided to make one cache per expression experiment. The reason for
 * this is that having complex keys for cached Elements based on expression experiment AND gene makes it difficult to
 * invalidate the cache when an expression experiment's data changes. The drawback is that there are potentially
 * hundreds of caches; I don't know if there are any performance considerations there.
 *
 * @author paul
 */
@Component
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class ProcessedDataVectorCacheImpl implements InitializingBean, ProcessedDataVectorCache {

    private static final String VECTOR_CACHE_NAME = "ProcessedExpressionDataVectorCache";
    private static final boolean VECTOR_CACHE_DEFAULT_ETERNAL = true;
    private static final boolean VECTOR_CACHE_DEFAULT_OVERFLOW_TO_DISK = true;
    private static final int VECTOR_CACHE_DEFAULT_MAX_ELEMENTS = 100000;
    private static final int VECTOR_CACHE_DEFAULT_TIME_TO_IDLE = 10000;
    private static final int VECTOR_CACHE_DEFAULT_TIME_TO_LIVE = 10000;

    private Cache cache;

    @Autowired
    private CacheManager cacheManager;

    @Override
    public void addToCache( Long eeId, Long g, Collection<DoubleVectorValueObject> collection ) {
        cache.put( new Element( new CacheKey( eeId, g ), collection ) );
    }

    @Override
    public void clearCache() {
        cache.removeAll();
    }

    @Override
    public void clearCache( Long eeId ) {
        for ( Object o : cache.getKeys() ) {
            CacheKey k = ( CacheKey ) o;
            if ( k.getEeId().equals( eeId ) ) {
                cache.remove( k );
            }
        }
    }

    @Override
    public Collection<DoubleVectorValueObject> get( BioAssaySet ee, Long g ) {
        Element element = cache.get( new CacheKey( ee.getId(), g ) );
        if ( element == null )
            return null;
        @SuppressWarnings("unchecked") Collection<DoubleVectorValueObject> result = ( Collection<DoubleVectorValueObject> ) element
                .getObjectValue();

        /*
         * See 2878 - we don't want to keep these values cached, so the vectors can be re-used.
         */
        for ( DoubleVectorValueObject dvvo : result ) {
            dvvo.setPvalue( null );
        }
        return result;
    }

    @Override
    public int size() {
        return this.cache.getSize();
    }

    @Override
    public void afterPropertiesSet() {
        int maxElements = Settings.getInt( "gemma.cache.vectors.maxelements",
                ProcessedDataVectorCacheImpl.VECTOR_CACHE_DEFAULT_MAX_ELEMENTS );
        int timeToLive = Settings.getInt( "gemma.cache.vectors.timetolive",
                ProcessedDataVectorCacheImpl.VECTOR_CACHE_DEFAULT_TIME_TO_LIVE );
        int timeToIdle = Settings.getInt( "gemma.cache.vectors.timetoidle",
                ProcessedDataVectorCacheImpl.VECTOR_CACHE_DEFAULT_TIME_TO_IDLE );
        boolean overFlowToDisk = Settings.getBoolean( "gemma.cache.vectors.usedisk",
                ProcessedDataVectorCacheImpl.VECTOR_CACHE_DEFAULT_OVERFLOW_TO_DISK );
        boolean eternal = Settings.getBoolean( "gemma.cache.vectors.eternal",
                ProcessedDataVectorCacheImpl.VECTOR_CACHE_DEFAULT_ETERNAL ) && timeToLive == 0;

        this.cache = CacheUtils
                .createOrLoadCache( cacheManager, ProcessedDataVectorCacheImpl.VECTOR_CACHE_NAME,
                        maxElements, overFlowToDisk, eternal, timeToIdle, timeToLive );

    }
}

@SuppressWarnings({ "unused", "WeakerAccess" })
        // Possible external use
class CacheKey implements Serializable {

    private static final long serialVersionUID = -7873367550383853137L;
    private final Long eeId;
    private final Long geneId;

    CacheKey( Long eeId, Long geneId ) {
        this.eeId = eeId;
        this.geneId = geneId;
    }

    public Long getEeId() {
        return eeId;
    }

    public Long getGeneId() {
        return geneId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( eeId == null ? 0 : eeId.hashCode() );
        result = prime * result + ( geneId == null ? 0 : geneId.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( this.getClass() != obj.getClass() )
            return false;
        CacheKey other = ( CacheKey ) obj;
        if ( eeId == null ) {
            if ( other.eeId != null )
                return false;
        } else if ( !eeId.equals( other.eeId ) )
            return false;
        if ( geneId == null ) {
            return other.geneId == null;
        } else
            return geneId.equals( other.geneId );
    }

}