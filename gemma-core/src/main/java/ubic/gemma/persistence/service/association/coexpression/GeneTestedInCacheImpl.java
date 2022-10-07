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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.stereotype.Component;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionTestedIn;
import ubic.gemma.persistence.util.CacheUtils;
import ubic.gemma.persistence.util.Settings;

import java.util.Map;

/**
 * @author paul
 */
@Component
public class GeneTestedInCacheImpl implements InitializingBean, GeneTestedInCache {

    private static final String GENE_COEXPRESSIONTESTED_CACHE_NAME = "Gene2GeneCoexpressionTestedInCache";
    private static final boolean GENE_COEXPRESSIONTESTED_CACHE_DEFAULT_ETERNAL = true;
    private static final boolean GENE_COEXPRESSIONTESTED_CACHE_DEFAULT_OVERFLOW_TO_DISK = false;
    private static final int GENE_COEXPRESSIONTESTED_CACHE_DEFAULT_MAX_ELEMENTS = 100000;
    private static final int GENE_COEXPRESSIONTESTED_CACHE_DEFAULT_TIME_TO_IDLE = 604800;
    private static final int GENE_COEXPRESSIONTESTED_CACHE_DEFAULT_TIME_TO_LIVE = 1209600;

    private Cache cache;

    @Autowired
    private EhCacheManagerFactoryBean cacheManagerFactory;

    @Override
    public void afterPropertiesSet() {
        CacheManager cacheManager = cacheManagerFactory.getObject();
        assert cacheManager != null;

        // Other settings just use the gene2gene ones.
        int timeToLive = Settings.getInt( "gemma.cache.gene2gene.timetolive",
                GeneTestedInCacheImpl.GENE_COEXPRESSIONTESTED_CACHE_DEFAULT_TIME_TO_LIVE );
        int timeToIdle = Settings.getInt( "gemma.cache.gene2gene.timetoidle",
                GeneTestedInCacheImpl.GENE_COEXPRESSIONTESTED_CACHE_DEFAULT_TIME_TO_IDLE );
        boolean overFlowToDisk = Settings.getBoolean( "gemma.cache.gene2gene.usedisk",
                GeneTestedInCacheImpl.GENE_COEXPRESSIONTESTED_CACHE_DEFAULT_OVERFLOW_TO_DISK );
        boolean eternal = Settings.getBoolean( "gemma.cache.gene2gene.eternal",
                GeneTestedInCacheImpl.GENE_COEXPRESSIONTESTED_CACHE_DEFAULT_ETERNAL ) && timeToLive == 0;

        this.cache = CacheUtils
                .createOrLoadCache( cacheManager, GeneTestedInCacheImpl.GENE_COEXPRESSIONTESTED_CACHE_NAME,
                        GeneTestedInCacheImpl.GENE_COEXPRESSIONTESTED_CACHE_DEFAULT_MAX_ELEMENTS,
                        overFlowToDisk, eternal, timeToIdle, timeToLive );
    }

    @Override
    public void cacheTestedIn( GeneCoexpressionTestedIn testedIn ) {
        cache.put( new Element( testedIn.getGeneId(), testedIn ) );

    }

    @Override
    public void clearCache() {
        cache.removeAll();
    }

    @Override
    public GeneCoexpressionTestedIn get( Long geneId ) {
        if ( cache == null )
            return null;
        if ( geneId == null )
            return null;

        Element o = cache.get( geneId );

        if ( o == null )
            return null;
        assert o.getObjectValue() != null;

        return ( GeneCoexpressionTestedIn ) o.getObjectValue();

    }

    @Override
    public void cache( Map<Long, GeneCoexpressionTestedIn> idMap ) {
        for ( GeneCoexpressionTestedIn v : idMap.values() ) {
            this.cacheTestedIn( v );
        }

    }

    @Override
    public boolean contains( Long geneId ) {
        return cache.isKeyInCache( geneId );
    }

    @Override
    public void remove( Long id ) {
        if ( cache.isKeyInCache( id ) )
            this.cache.remove( id );
    }
}
