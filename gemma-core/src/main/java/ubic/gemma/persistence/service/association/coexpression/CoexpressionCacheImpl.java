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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.persistence.util.CacheUtils;
import ubic.gemma.persistence.util.Settings;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Configures the cache for gene2gene coexpression.
 *
 * @author paul
 */
@SuppressWarnings("SynchronizeOnNonFinalField") // Cache has to be initialized in afterPropertiesSet
@Component
public class CoexpressionCacheImpl implements InitializingBean, CoexpressionCache {

    private static final boolean GENE_COEXPRESSION_CACHE_DEFAULT_ETERNAL = true;
    private static final int GENE_COEXPRESSION_CACHE_DEFAULT_MAX_ELEMENTS = 500;
    private static final boolean GENE_COEXPRESSION_CACHE_DEFAULT_OVERFLOW_TO_DISK = true;
    private static final int GENE_COEXPRESSION_CACHE_DEFAULT_TIME_TO_IDLE = 6048;
    private static final int GENE_COEXPRESSION_CACHE_DEFAULT_TIME_TO_LIVE = 9600;
    private static final String GENE_COEXPRESSION_CACHE_NAME = "Gene2GeneCoexpressionCache";
    private static final Logger log = LoggerFactory.getLogger( CoexpressionCacheImpl.class );
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
        int maxElements = Settings.getInt( "gemma.cache.gene2gene.maxelements",
                CoexpressionCacheImpl.GENE_COEXPRESSION_CACHE_DEFAULT_MAX_ELEMENTS );
        int timeToLive = Settings.getInt( "gemma.cache.gene2gene.timetolive",
                CoexpressionCacheImpl.GENE_COEXPRESSION_CACHE_DEFAULT_TIME_TO_LIVE );
        int timeToIdle = Settings.getInt( "gemma.cache.gene2gene.timetoidle",
                CoexpressionCacheImpl.GENE_COEXPRESSION_CACHE_DEFAULT_TIME_TO_IDLE );
        boolean overFlowToDisk = Settings.getBoolean( "gemma.cache.gene2gene.usedisk",
                CoexpressionCacheImpl.GENE_COEXPRESSION_CACHE_DEFAULT_OVERFLOW_TO_DISK );
        boolean eternal = Settings.getBoolean( "gemma.cache.gene2gene.eternal",
                CoexpressionCacheImpl.GENE_COEXPRESSION_CACHE_DEFAULT_ETERNAL ) && timeToLive == 0;
        this.cache = CacheUtils
                .createOrLoadCache( cacheManager, CoexpressionCacheImpl.GENE_COEXPRESSION_CACHE_NAME,
                        maxElements, overFlowToDisk, eternal, timeToIdle, timeToLive );
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
            this.cache.put( new Element( new GeneCached( geneId ), forCache ) );
        }
    }

    @Override
    public void cacheCoexpression( Map<Long, List<CoexpressionValueObject>> r ) {
        if ( !this.enabled.get() )
            return;

        StopWatch timer = new StopWatch();

        timer.start();
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

        CacheManager manager = CacheManager.getInstance();
        synchronized ( cache ) {
            manager.getCache( CoexpressionCacheImpl.GENE_COEXPRESSION_CACHE_NAME ).removeAll();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<CoexpressionValueObject> get( Long g ) {
        if ( !this.enabled.get() )
            return null;

        synchronized ( cache ) {
            Element element = this.cache.get( new GeneCached( g ) );
            if ( element == null )
                return null;
            List<CoexpressionValueObject> result = new ArrayList<>();

            for ( CoexpressionCacheValueObject co : ( List<CoexpressionCacheValueObject> ) element.getObjectValue() ) {
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
                if ( this.cache.remove( long1 ) )
                    affected++;
            }
            return affected;
        }
    }

    @Override
    public boolean remove( Long id ) {
        if ( !this.enabled.get() )
            return false;
        synchronized ( cache ) {
            return this.cache.remove( new GeneCached( id ) );
        }
    }

    @Override
    public void shutdown() {
        synchronized ( cache ) {
            this.enabled.set( false );
            this.cache.dispose();
        }
    }

    /**
     * For storing information about gene results that are cached.
     */
    private static class GeneCached implements Serializable {

        private static final long serialVersionUID = 915877171652447101L;

        final long geneId;

        GeneCached( long geneId ) {
            super();
            this.geneId = geneId;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ( int ) ( geneId ^ ( geneId >>> 32 ) );
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
            GeneCached other = ( GeneCached ) obj;

            return geneId == other.geneId;
        }

    }

}
