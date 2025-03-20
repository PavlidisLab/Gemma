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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.cache.CacheUtils;

import java.io.Serializable;
import java.util.Collection;

import static java.util.Collections.unmodifiableCollection;

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
class ProcessedDataVectorByGeneCacheImpl implements ProcessedDataVectorByGeneCache {

    private static final String VECTOR_CACHE_NAME = "ProcessedExpressionDataVectorByGeneCache";

    private final Cache cache;

    @Autowired
    public ProcessedDataVectorByGeneCacheImpl( CacheManager cacheManager ) {
        this.cache = CacheUtils.getCache( cacheManager, VECTOR_CACHE_NAME );
    }

    @Override
    public Collection<DoubleVectorValueObject> get( ExpressionExperiment ee, Long geneId ) {
        Assert.notNull( ee.getId() );
        Long eeId = ee.getId();
        Cache.ValueWrapper element = cache.get( new CacheKey( eeId, geneId ) );
        if ( element == null )
            return null;
        //noinspection unchecked
        return ( Collection<DoubleVectorValueObject> ) element.get();
    }

    @Override
    public void put( ExpressionExperiment ee, Long geneId, Collection<DoubleVectorValueObject> collection ) {
        Assert.notNull( ee.getId() );
        putById( ee.getId(), geneId, collection );
    }

    @Override
    public void putById( Long eeId, Long geneId, Collection<DoubleVectorValueObject> collection ) {
        cache.put( new CacheKey( eeId, geneId ), unmodifiableCollection( collection ) );
    }

    @Override
    public void evict( ExpressionExperiment ee ) {
        Assert.notNull( ee.getId() );
        Long eeId = ee.getId();
        CacheUtils.evictIf( cache, o -> {
            CacheKey k = ( CacheKey ) o;
            return k.getExpressionExperimentId().equals( eeId );
        } );
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Value
    private static class CacheKey implements Serializable {
        Long expressionExperimentId;
        Long geneId;
    }
}