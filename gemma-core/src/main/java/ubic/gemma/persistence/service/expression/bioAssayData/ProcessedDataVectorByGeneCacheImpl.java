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
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.util.CacheUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

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
class ProcessedDataVectorByGeneCacheImpl implements ProcessedDataVectorByGeneCache {

    private static final String VECTOR_CACHE_NAME = "ProcessedExpressionDataVectorCache";

    private final Cache cache;

    @Autowired
    public ProcessedDataVectorByGeneCacheImpl( CacheManager cacheManager ) {
        this.cache = CacheUtils.getCache( cacheManager, ProcessedDataVectorByGeneCacheImpl.VECTOR_CACHE_NAME );
    }

    @Override
    public Collection<DoubleVectorValueObject> get( BioAssaySet bas, Gene gene ) {
        Assert.notNull( bas.getId() );
        Assert.notNull( gene.getId() );
        return getById( bas.getId(), gene.getId() );
    }

    @Override
    public Collection<DoubleVectorValueObject> getById( Long eeId, Long geneId ) {
        Cache.ValueWrapper element = cache.get( new CacheKey( eeId, geneId ) );
        if ( element == null )
            return null;
        //noinspection unchecked
        return ( Collection<DoubleVectorValueObject> ) element.get();
    }

    @Override
    public void put( BioAssaySet bas, Gene gene, Collection<DoubleVectorValueObject> collection ) {
        Assert.notNull( bas.getId() );
        Assert.notNull( gene.getId() );
        putById( bas.getId(), gene.getId(), collection );
    }

    @Override
    public void putById( Long basId, Long geneId, Collection<DoubleVectorValueObject> collection ) {
        Collection<DoubleVectorValueObject> copy = new ArrayList<>( collection.size() );
        for ( DoubleVectorValueObject vector : collection ) {
            DoubleVectorValueObject c = new DoubleVectorValueObject( vector );
            // See 2878 - we don't want to keep these values cached, so the vectors can be re-used.
            c.setPvalue( null );
            copy.add( c );
        }
        cache.put( new CacheKey( basId, geneId ), Collections.unmodifiableCollection( copy ) );
    }

    @Override
    public void evict( BioAssaySet bas ) {
        Assert.notNull( bas.getId() );
        evictById( bas.getId() );
    }

    @Override
    public void evictById( Long basId ) {
        CacheUtils.evictIf( cache, o -> {
            CacheKey k = ( CacheKey ) o;
            return k.getBasId().equals( basId );
        } );
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Value
    private static class CacheKey implements Serializable {
        private static final long serialVersionUID = -7873367550383853137L;
        Long basId;
        Long geneId;
    }
}