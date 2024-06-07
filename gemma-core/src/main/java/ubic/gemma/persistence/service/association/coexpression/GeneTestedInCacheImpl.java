/*
 * The gemma project
 *
 * Copyright (c) 2014 University of British Columbia
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

package ubic.gemma.persistence.service.association.coexpression;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionTestedIn;
import ubic.gemma.persistence.cache.CacheUtils;

import java.util.Map;

/**
 * @author paul
 */
@Component
public class GeneTestedInCacheImpl implements InitializingBean, GeneTestedInCache {

    private static final String GENE_COEXPRESSION_TESTED_IN_CACHE_NAME = "Gene2GeneCoexpressionTestedInCache";

    private Cache cache;

    @Autowired
    private CacheManager cacheManager;

    @Override
    public void afterPropertiesSet() {
        this.cache = CacheUtils.getCache( cacheManager, GeneTestedInCacheImpl.GENE_COEXPRESSION_TESTED_IN_CACHE_NAME );
    }

    @Override
    public void cacheTestedIn( GeneCoexpressionTestedIn testedIn ) {
        cache.put( testedIn.getGeneId(), testedIn );

    }

    @Override
    public void clearCache() {
        cache.clear();
    }

    @Override
    public GeneCoexpressionTestedIn get( Long geneId ) {
        if ( cache == null )
            return null;
        if ( geneId == null )
            return null;

        Cache.ValueWrapper o = cache.get( geneId );

        if ( o == null )
            return null;
        assert o.get() != null;

        return ( GeneCoexpressionTestedIn ) o.get();

    }

    @Override
    public void cache( Map<Long, GeneCoexpressionTestedIn> idMap ) {
        for ( GeneCoexpressionTestedIn v : idMap.values() ) {
            this.cacheTestedIn( v );
        }

    }

    @Override
    public boolean contains( Long geneId ) {
        return cache.get( geneId ) != null;
    }

    @Override
    public void remove( Long geneId ) {
        this.cache.evict( geneId );
    }
}
