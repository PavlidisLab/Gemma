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
package ubic.gemma.model.association.coexpression;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration.Strategy;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.TimeoutBehaviorConfiguration;
import net.sf.ehcache.config.TimeoutBehaviorConfiguration.TimeoutBehaviorType;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.stereotype.Component;

import ubic.gemma.util.Settings;

/**
 * Configures the cache for gene2gene coexpression.
 * 
 * @author paul
 * @version $Id$
 */
@Component
public class CoexpressionCacheImpl implements InitializingBean, CoexpressionCache {

    /**
     * For storing information about gene results that are cached.
     */
    private static class GeneCached implements Serializable {
        /**
         * 
         */
        private static final long serialVersionUID = 915877171652447101L;

        long geneId;

        public GeneCached( long geneId ) {
            super();
            this.geneId = geneId;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if ( this == obj ) return true;
            if ( obj == null ) return false;
            if ( getClass() != obj.getClass() ) return false;
            GeneCached other = ( GeneCached ) obj;

            if ( geneId != other.geneId ) return false;
            return true;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ( int ) ( geneId ^ ( geneId >>> 32 ) );
            return result;
        }

    }

    private static final boolean GENE_COEXPRESSION_CACHE_DEFAULT_ETERNAL = true;

    private static final int GENE_COEXPRESSION_CACHE_DEFAULT_MAX_ELEMENTS = 100000;

    private static final boolean GENE_COEXPRESSION_CACHE_DEFAULT_OVERFLOW_TO_DISK = true;
    private static final int GENE_COEXPRESSION_CACHE_DEFAULT_TIME_TO_IDLE = 604800;
    private static final int GENE_COEXPRESSION_CACHE_DEFAULT_TIME_TO_LIVE = 1209600;
    private static final String GENE_COEXPRESSION_CACHE_NAME = "Gene2GeneCoexpressionCache";
    private static Logger log = LoggerFactory.getLogger( CoexpressionCacheImpl.class );
    private Cache cache;

    @Autowired
    private EhCacheManagerFactoryBean cacheManagerFactory;

    /**
     * Initialize the cache; if it already exists it will not be recreated.
     * 
     * @return
     */
    @Override
    public void afterPropertiesSet() {
        CacheManager cacheManager = cacheManagerFactory.getObject();
        assert cacheManager != null;
        int maxElements = Settings.getInt( "gemma.cache.gene2gene.maxelements",
                GENE_COEXPRESSION_CACHE_DEFAULT_MAX_ELEMENTS );
        int timeToLive = Settings.getInt( "gemma.cache.gene2gene.timetolive",
                GENE_COEXPRESSION_CACHE_DEFAULT_TIME_TO_LIVE );
        int timeToIdle = Settings.getInt( "gemma.cache.gene2gene.timetoidle",
                GENE_COEXPRESSION_CACHE_DEFAULT_TIME_TO_IDLE );

        boolean overFlowToDisk = Settings.getBoolean( "gemma.cache.gene2gene.usedisk",
                GENE_COEXPRESSION_CACHE_DEFAULT_OVERFLOW_TO_DISK );

        boolean eternal = Settings
                .getBoolean( "gemma.cache.gene2gene.eternal", GENE_COEXPRESSION_CACHE_DEFAULT_ETERNAL )
                && timeToLive == 0;
        boolean terracottaEnabled = Settings.getBoolean( "gemma.cache.clustered", false );

        boolean diskPersistent = Settings.getBoolean( "gemma.cache.diskpersistent", false ) && !terracottaEnabled;

        /*
         * See TerracottaConfiguration.
         */
        int diskExpiryThreadIntervalSeconds = 600;
        int maxElementsOnDisk = 10000;
        boolean terracottaCoherentReads = false;
        boolean clearOnFlush = false;

        if ( terracottaEnabled ) {
            CacheConfiguration config = new CacheConfiguration( GENE_COEXPRESSION_CACHE_NAME, maxElements );
            config.setStatistics( false );
            config.setMemoryStoreEvictionPolicy( MemoryStoreEvictionPolicy.LRU.toString() );
            // replace with config.addPersistence().
            config.addPersistence( new PersistenceConfiguration().strategy( Strategy.NONE ) );
            // config.setOverflowToDisk( false );
            config.setEternal( eternal );
            config.setTimeToIdleSeconds( timeToIdle );
            config.setMaxElementsOnDisk( maxElementsOnDisk );
            config.addTerracotta( new TerracottaConfiguration() );
            config.getTerracottaConfiguration().setCoherentReads( terracottaCoherentReads );
            config.clearOnFlush( clearOnFlush );
            config.setTimeToLiveSeconds( timeToLive );
            config.getTerracottaConfiguration().setClustered( true );
            config.getTerracottaConfiguration().setValueMode( "SERIALIZATION" );
            NonstopConfiguration nonstopConfiguration = new NonstopConfiguration();
            TimeoutBehaviorConfiguration tobc = new TimeoutBehaviorConfiguration();
            tobc.setType( TimeoutBehaviorType.NOOP.getTypeName() );
            nonstopConfiguration.addTimeoutBehavior( tobc );
            config.getTerracottaConfiguration().addNonstop( nonstopConfiguration );
            this.cache = new Cache( config );
        } else {
            this.cache = new Cache( GENE_COEXPRESSION_CACHE_NAME, maxElements, MemoryStoreEvictionPolicy.LRU,
                    overFlowToDisk, null, eternal, timeToLive, timeToIdle, diskPersistent,
                    diskExpiryThreadIntervalSeconds, null );
        }

        cacheManager.addCache( cache );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionCache#cacheCoexpression(java.util.Collection)
     */
    @Override
    public void cacheCoexpression( Long geneId, Collection<CoexpressionValueObject> r ) {
        assert r != null; // but can be empty, if there is no coexpression.
        assert geneId != null;
        List<CoexpressionCacheValueObject> forCache = new ArrayList<>();
        for ( CoexpressionValueObject g2g : r ) {
            if ( g2g.isFromCache() ) continue;
            assert g2g.getNumDatasetsSupporting() > 0;
            forCache.add( new CoexpressionCacheValueObject( g2g ) );
        }
        synchronized ( cache ) {
            this.cache.put( new Element( new GeneCached( geneId ), forCache ) );
        }
    }

    @Override
    public void cacheCoexpression( Map<Long, List<CoexpressionValueObject>> r ) {

        StopWatch timer = new StopWatch();

        timer.start();
        for ( Long id : r.keySet() ) {
            List<CoexpressionValueObject> res = r.get( id );
            assert res != null;
            cacheCoexpression( id, res );
        }

        if ( timer.getTime() > 100 ) {
            log.info( "Caching " + r.size() + " results took: " + timer.getTime() + "ms" );
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionCache#clearCache()
     */
    @Override
    public void clearCache() {
        CacheManager manager = CacheManager.getInstance();
        synchronized ( cache ) {
            manager.getCache( GENE_COEXPRESSION_CACHE_NAME ).removeAll();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionCache#get(java.lang.Long)
     */
    @Override
    public List<CoexpressionValueObject> get( Long g ) {
        synchronized ( cache ) {
            Element element = this.cache.get( new GeneCached( g ) );
            if ( element == null ) return null;
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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.coexpression.CoexpressionCache#remove(java.util.Collection)
     */
    @Override
    public int remove( Collection<Long> genes ) {
        int affected = 0;
        for ( Long long1 : genes ) {
            synchronized ( cache ) {
                if ( this.cache.remove( long1 ) ) affected++;
            }
        }
        return affected;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionCache#remove(java.lang.Long)
     */
    @Override
    public boolean remove( Long id ) {
        synchronized ( cache ) {
            return this.cache.remove( new GeneCached( id ) );
        }
    }

}
