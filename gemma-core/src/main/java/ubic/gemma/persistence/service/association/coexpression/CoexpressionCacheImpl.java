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
package ubic.gemma.persistence.service.association.coexpression;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import ubic.gemma.persistence.cache.CacheUtils;
import ubic.gemma.core.config.Settings;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Configures the cache for gene2gene coexpression.
 *
 * @author paul
 */
@SuppressWarnings("SynchronizeOnNonFinalField") // Cache has to be initialized in afterPropertiesSet
@Component
@CommonsLog
public class CoexpressionCacheImpl implements InitializingBean, CoexpressionCache {

    private static final String GENE_COEXPRESSION_CACHE_NAME = "Gene2GeneCoexpressionCache";
    private final AtomicBoolean enabled = new AtomicBoolean(
            Settings.getBoolean( "gemma.cache.gene2gene.enabled", true ) );
    private Cache cache;
    @Autowired
    private CacheManager cacheManager;

    /**
     * Initialize the cache; if it already exists it will not be recreated.
     */
    @Override
    public void afterPropertiesSet() {
        this.cache = CacheUtils.getCache( cacheManager, CoexpressionCacheImpl.GENE_COEXPRESSION_CACHE_NAME );
    }

    @Override
    public void cacheCoexpression( Long geneId, Collection<CoexpressionValueObject> r ) {
        if ( !this.enabled.get() )
            return;

        assert r != null; // but can be empty, if there is no coexpression.
        assert geneId != null;
        List<CoexpressionCacheValueObject> forCache = new ArrayList<>();
        for ( CoexpressionValueObject g2g : r ) {
            if ( g2g.isFromCache() )
                continue;
            assert g2g.getNumDatasetsSupporting() > 0;
            if ( g2g.getNumDatasetsSupporting() < CoexpressionCache.CACHE_QUERY_STRINGENCY )
                continue;
            forCache.add( new CoexpressionCacheValueObject( g2g ) );
        }
        synchronized ( cache ) {
            this.cache.put( geneId, forCache );
        }
    }

    @Override
    public void cacheCoexpression( Map<Long, List<CoexpressionValueObject>> r ) {
        if ( !this.enabled.get() )
            return;

        StopWatch timer = StopWatch.createStarted();

        for ( Long id : r.keySet() ) {
            List<CoexpressionValueObject> res = r.get( id );
            assert res != null;
            this.cacheCoexpression( id, res );
        }

        if ( timer.getTime() > 100 ) {
            CoexpressionCacheImpl.log.debug( "Caching " + r.size() + " results took: " + timer.getTime() + "ms" );
        }
    }

    @Override
    public void clearCache() {
        if ( !this.enabled.get() )
            return;

        synchronized ( cache ) {
            cache.clear();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<CoexpressionValueObject> get( Long geneId ) {
        if ( !this.enabled.get() )
            return null;

        synchronized ( cache ) {
            Cache.ValueWrapper element = this.cache.get( geneId );
            if ( element == null )
                return null;
            List<CoexpressionValueObject> result = new ArrayList<>();

            for ( CoexpressionCacheValueObject co : ( List<CoexpressionCacheValueObject> ) element.get() ) {
                CoexpressionValueObject vo = co.toModifiable();
                vo.setFromCache( true );
                assert vo.getNumDatasetsSupporting() > 0;
                result.add( vo );
            }
            return result;
        }
    }

    @Override
    public boolean isEnabled() {
        return this.enabled.get();
    }

    @Override
    public int remove( Collection<Long> genes ) {
        if ( !this.enabled.get() )
            return 0;
        synchronized ( cache ) {
            int affected = 0;
            for ( Long long1 : genes ) {
                if ( this.cache.get( long1 ) != null ) {
                    this.cache.evict( long1 );
                    affected++;
                }
            }
            return affected;
        }
    }

    @Override
    public boolean remove( Long id ) {
        if ( !this.enabled.get() )
            return false;
        synchronized ( cache ) {
            if ( this.cache.get( id ) != null ) {
                this.cache.evict( id );
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public void shutdown() {
        this.enabled.set( false );
    }
}
