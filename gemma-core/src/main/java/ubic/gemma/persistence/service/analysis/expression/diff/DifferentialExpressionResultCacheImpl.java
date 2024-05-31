/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
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

package ubic.gemma.persistence.service.analysis.expression.diff;

import lombok.Value;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import ubic.gemma.model.analysis.expression.diff.DiffExprGeneSearchResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionValueObject;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.persistence.cache.CacheUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Cache for data from differential expression result queries.
 *
 * @author Paul
 */
@Component
public class DifferentialExpressionResultCacheImpl implements DifferentialExpressionResultCache, InitializingBean {

    private static final String
            DIFF_EX_RESULT_CACHE_NAME = "DiffExResultCache",
            TOP_HITS_CACHE_NAME = "TopDiffExResultCache";

    @Autowired
    private CacheManager cacheManager;

    private Boolean enabled = true;

    private Cache cache;

    private Cache topHitsCache;

    @Override
    public void addToCache( DiffExprGeneSearchResult diffExForCache ) {
        Long r = diffExForCache.getResultSetId();
        Long g = diffExForCache.getGeneId();
        cache.put( new CacheKey( r, g ), diffExForCache );
    }

    @Override
    public void addToCache( Collection<DiffExprGeneSearchResult> diffExForCache ) {
        for ( DiffExprGeneSearchResult d : diffExForCache ) {
            this.addToCache( d );
        }
    }

    @Override
    public void clearCache() {
        cache.clear();
        topHitsCache.clear();
    }

    @Override
    public void clearCache( Long resultSetId ) {
        CacheUtils.evictIf( cache, o -> {
            CacheKey k = ( CacheKey ) o;
            return k.resultSetId.equals( resultSetId );
        } );
    }

    @Override
    public void clearTopHitCache( Long resultSetId ) {
        this.topHitsCache.evict( resultSetId );
    }

    @Override
    public Collection<DiffExprGeneSearchResult> get( Long resultSet, Collection<Long> genes ) {
        assert cache != null;
        Collection<DiffExprGeneSearchResult> results = new HashSet<>();
        for ( Long g : genes ) {
            Cache.ValueWrapper element = cache.get( new CacheKey( resultSet, g ) );
            if ( element != null ) {
                results.add( ( DiffExprGeneSearchResult ) element.get() );
            }
        }
        return results;
    }

    @Override
    public DiffExprGeneSearchResult get( Long resultSet, Long g ) {
        assert cache != null;
        Cache.ValueWrapper element = cache.get( new CacheKey( resultSet, g ) );
        if ( element == null )
            return null;
        return ( DiffExprGeneSearchResult ) element.get();
    }

    @Override
    public Boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled( Boolean enabled ) {
        this.enabled = enabled;
    }

    @Override
    public void addToTopHitsCache( ExpressionAnalysisResultSet resultSet,
            List<DifferentialExpressionValueObject> items ) {
        this.topHitsCache.put( resultSet.getId(), items );
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<DifferentialExpressionValueObject> getTopHits( ExpressionAnalysisResultSet resultSet ) {
        Cache.ValueWrapper element = this.topHitsCache.get( resultSet.getId() );
        if ( element == null )
            return null;
        return ( List<DifferentialExpressionValueObject> ) element.get();
    }

    @Override
    public void afterPropertiesSet() {
        this.cache = CacheUtils.getCache( cacheManager, DifferentialExpressionResultCacheImpl.DIFF_EX_RESULT_CACHE_NAME );
        this.topHitsCache = CacheUtils.getCache( cacheManager, DifferentialExpressionResultCacheImpl.TOP_HITS_CACHE_NAME );
    }

    @Value
    private static class CacheKey implements Serializable {
        private static final long serialVersionUID = 1453661277282349121L;
        Long resultSetId;
        Long geneId;
    }
}