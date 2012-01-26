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
import java.util.Collection;
import java.util.HashSet;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.stereotype.Component;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.CoexpressionCacheValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.util.ConfigUtils;

/**
 * Configures the cache for data vectors.
 * <p>
 * Implementation note: This uses ehCache. I have decided to make one cache per expression experiment. The reason for
 * this is that having complex keys for cached Elements based on expression experiment AND gene makes it difficult to
 * invalidate the cache when an expression experiment's data changes. The drawback is that there are potentially
 * hundreds of caches; I don't know if there are any performance considerations there.
 * 
 * @author paul
 * @version $Id$
 */
@Component
public class Probe2ProbeCoexpressionCacheImpl implements InitializingBean, Probe2ProbeCoexpressionCache {

    private static final String PROCESSED_DATA_VECTOR_CACHE_NAME_BASE = "Probe2ProbeCache";
    private static final int PROCESSED_DATA_VECTOR_CACHE_DEFAULT_MAX_ELEMENTS = 100000;
    private static final int PROCESSED_DATA_VECTOR_CACHE_DEFAULT_TIME_TO_LIVE = 10000;
    private static final int PROCESSED_DATA_VECTOR_CACHE_DEFAULT_TIME_TO_IDLE = 10000;
    private static final boolean PROCESSED_DATA_VECTOR_CACHE_DEFAULT_ETERNAL = true;
    private static final boolean PROCESSED_DATA_VECTOR_CACHE_DEFAULT_OVERFLOW_TO_DISK = true;

    @Autowired
    private EhCacheManagerFactoryBean cacheManagerFactory;

    private Boolean enabled = true;

    /* (non-Javadoc)
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionCache#isEnabled()
     */
    @Override
    public Boolean isEnabled() {
        return enabled;
    }

    /* (non-Javadoc)
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionCache#setEnabled(java.lang.Boolean)
     */
    @Override
    public void setEnabled( Boolean enabled ) {
        this.enabled = enabled;
    }

    private Cache cache;

    /* (non-Javadoc)
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionCache#addToCache(ubic.gemma.model.genome.CoexpressionCacheValueObject)
     */
    @Override
    public void addToCache( CoexpressionCacheValueObject coExVOForCache ) {
        Long eeID = coExVOForCache.getExpressionExperiment();
        Gene queryGene = coExVOForCache.getQueryGene();

        Collection<CoexpressionCacheValueObject> existingCache = get( eeID, queryGene.getId() );
        if ( existingCache != null ) {
            existingCache.add( coExVOForCache );
        } else {
            Collection<CoexpressionCacheValueObject> cachedValues = new HashSet<CoexpressionCacheValueObject>();
            cachedValues.add( coExVOForCache );
            cache.put( new Element( new CacheKey( eeID, queryGene.getId() ), cachedValues ) );
        }
    }

    /* (non-Javadoc)
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionCache#clearCache()
     */
    @Override
    public void clearCache() {
        cache.removeAll();
    }

    /* (non-Javadoc)
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionCache#clearCache(java.lang.Long)
     */
    @Override
    public void clearCache( Long eeid ) {
        for ( Object o : cache.getKeys() ) {
            CacheKey k = ( CacheKey ) o;
            if ( k.eeid.equals( eeid ) ) {
                cache.remove( k );
            }
        }
    }

    /* (non-Javadoc)
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionCache#get(ubic.gemma.model.expression.experiment.BioAssaySet, ubic.gemma.model.genome.Gene)
     */
    @Override
    public Collection<CoexpressionCacheValueObject> get( BioAssaySet ee, Gene g ) {
        Long eeid = ee.getId();
        Long geneid = g.getId();
        return get( eeid, geneid );
    }

    private Collection<CoexpressionCacheValueObject> get( Long eeid, Long geneid ) {
        assert cache != null;
        Element element = cache.get( new CacheKey( eeid, geneid ) );
        if ( element == null ) return null;
        return ( Collection<CoexpressionCacheValueObject> ) element.getValue();
    }

    /**
     * Initialize the vector cache; if it already exists it will not be recreated.
     * 
     * @return
     */
    public void afterPropertiesSet() throws Exception {
        CacheManager cacheManager = cacheManagerFactory.getObject();
        int maxElements = ConfigUtils.getInt( "gemma.cache.probe2probe.maxelements",
                PROCESSED_DATA_VECTOR_CACHE_DEFAULT_MAX_ELEMENTS );
        int timeToLive = ConfigUtils.getInt( "gemma.cache.probe2probe.timetolive",
                PROCESSED_DATA_VECTOR_CACHE_DEFAULT_TIME_TO_LIVE );
        int timeToIdle = ConfigUtils.getInt( "gemma.cache.probe2probe.timetoidle",
                PROCESSED_DATA_VECTOR_CACHE_DEFAULT_TIME_TO_IDLE );

        boolean eternal = ConfigUtils.getBoolean( "gemma.cache.probe2probe.eternal",
                PROCESSED_DATA_VECTOR_CACHE_DEFAULT_ETERNAL )
                && timeToLive == 0;
        boolean terracottaEnabled = ConfigUtils.getBoolean( "gemma.cache.clustered", true );
        boolean overFlowToDisk = ConfigUtils.getBoolean( "gemma.cache.probe2probe.usedisk",
                PROCESSED_DATA_VECTOR_CACHE_DEFAULT_OVERFLOW_TO_DISK );
        boolean diskPersistent = ConfigUtils.getBoolean( "gemma.cache.diskpersistent", false ) && !terracottaEnabled;

        if ( !cacheManager.cacheExists( PROCESSED_DATA_VECTOR_CACHE_NAME_BASE ) ) {
            /*
             * See TerracottaConfiguration.
             */
            int diskExpiryThreadIntervalSeconds = 600;
            int maxElementsOnDisk = 10000;
            boolean terracottaCoherentReads = false;
            boolean clearOnFlush = false;

            if ( terracottaEnabled ) {

                CacheConfiguration config = new CacheConfiguration( PROCESSED_DATA_VECTOR_CACHE_NAME_BASE, maxElements );
                config.setStatistics( false );
                config.setMemoryStoreEvictionPolicy( MemoryStoreEvictionPolicy.LRU.toString() );
                config.setOverflowToDisk( overFlowToDisk );
                config.setEternal( eternal );
                config.setTimeToIdleSeconds( timeToIdle );
                config.setMaxElementsOnDisk( maxElementsOnDisk );
                config.addTerracotta( new TerracottaConfiguration() );
                config.getTerracottaConfiguration().setCoherentReads( terracottaCoherentReads );
                config.clearOnFlush( clearOnFlush );
                config.setTimeToLiveSeconds( timeToLive );
                config.getTerracottaConfiguration().setClustered( terracottaEnabled );
                config.getTerracottaConfiguration().setValueMode( "SERIALIZATION" );
                config.getTerracottaConfiguration().addNonstop( new NonstopConfiguration() );
                this.cache = new Cache( config );
                // this.cache = new Cache( PROCESSED_DATA_VECTOR_CACHE_NAME_BASE, maxElements,
                // MemoryStoreEvictionPolicy.LRU, overFlowToDisk, null, eternal, timeToLive, timeToIdle,
                // diskPersistent, diskExpiryThreadIntervalSeconds, null, null, maxElementsOnDisk, 10,
                // clearOnFlush, terracottaEnabled, "SERIALIZATION", terracottaCoherentReads );
                // FIXME make it nonstop.

            } else {
                this.cache = new Cache( PROCESSED_DATA_VECTOR_CACHE_NAME_BASE, maxElements,
                        MemoryStoreEvictionPolicy.LRU, overFlowToDisk, null, eternal, timeToLive, timeToIdle,
                        diskPersistent, diskExpiryThreadIntervalSeconds, null );
            }
            cacheManager.addCache( cache );
        }

    }
}

class CacheKey implements Serializable{

    /**
     * 
     */
    private static final long serialVersionUID = 6453661257282349151L;
    Long eeid;
    Long geneId;

    CacheKey( Long eeid, Long geneId ) {
        this.eeid = eeid;
        this.geneId = geneId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( eeid == null ) ? 0 : eeid.hashCode() );
        result = prime * result + ( ( geneId == null ) ? 0 : geneId.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        CacheKey other = ( CacheKey ) obj;
        if ( eeid == null ) {
            if ( other.eeid != null ) return false;
        } else if ( !eeid.equals( other.eeid ) ) return false;
        if ( geneId == null ) {
            if ( other.geneId != null ) return false;
        } else if ( !geneId.equals( other.geneId ) ) return false;
        return true;
    }

}