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

import lombok.Value;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.persistence.util.CacheUtils;

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

    private Cache cache;

    @Autowired
    private CacheManager cacheManager;

    @Override
    public void addToCache( Long eeId, Long g, Collection<DoubleVectorValueObject> collection ) {
        cache.put( new CacheKey( eeId, g ), collection );
    }

    @Override
    public void clearCache() {
        cache.clear();
    }

    @Override
    public void clearCache( Long eeId ) {
        CacheUtils.evictIf( cache, o -> {
            CacheKey k = ( CacheKey ) o;
            return k.getEeId().equals( eeId );
        } );
    }

    @Override
    public Collection<DoubleVectorValueObject> get( BioAssaySet ee, Long g ) {
        Cache.ValueWrapper element = cache.get( new CacheKey( ee.getId(), g ) );
        if ( element == null )
            return null;
        @SuppressWarnings("unchecked") Collection<DoubleVectorValueObject> result = ( Collection<DoubleVectorValueObject> ) element
                .get();

        /*
         * See 2878 - we don't want to keep these values cached, so the vectors can be re-used.
         */
        for ( DoubleVectorValueObject dvvo : result ) {
            dvvo.setPvalue( null );
        }
        return result;
    }

    @Override
    public void afterPropertiesSet() {
        this.cache = CacheUtils.getCache( cacheManager, ProcessedDataVectorCacheImpl.VECTOR_CACHE_NAME );
    }

    @Value
    private static class CacheKey implements Serializable {
        private static final long serialVersionUID = -7873367550383853137L;
        Long eeId;
        Long geneId;
    }
}