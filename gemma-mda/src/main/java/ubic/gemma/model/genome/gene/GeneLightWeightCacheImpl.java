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
package ubic.gemma.model.genome.gene;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.stereotype.Component;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.TimeoutBehaviorConfiguration;
import net.sf.ehcache.config.TimeoutBehaviorConfiguration.TimeoutBehaviorType;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import ubic.gemma.util.ConfigUtils;

/**
 * Configures the cache for lightweight Gene objects to speed up coexpression search.
 * coexpression search doesn't need all of the associations of the Gene entity loaded and also reuses 
 * Gene objects frequently(for example a list of highly coexpressed genes)
 * 
 *
 */
@Component
public class GeneLightWeightCacheImpl implements InitializingBean, GeneLightWeightCache {

    @Autowired
    private EhCacheManagerFactoryBean cacheManagerFactory;

    private static final String GENE_LIGHT_WEIGHT_CACHE_NAME = "GeneLightWeightCache";

    private static final int GENE_LIGHT_WEIGHT_CACHE_DEFAULT_MAX_ELEMENTS = 100000;
    private static final int GENE_LIGHT_WEIGHT_CACHE_DEFAULT_TIME_TO_LIVE = 2628000;
    private static final int GENE_LIGHT_WEIGHT_CACHE_DEFAULT_TIME_TO_IDLE = 1314000;
    private static final boolean GENE_LIGHT_WEIGHT_CACHE_DEFAULT_ETERNAL = true;
    private static final boolean GENE_LIGHT_WEIGHT_CACHE_DEFAULT_OVERFLOW_TO_DISK = false;
    private Cache cache;
    
    @Override
    public void clearCache() {
        CacheManager manager = CacheManager.getInstance();
        manager.getCache( GENE_LIGHT_WEIGHT_CACHE_NAME ).removeAll();
    }
    
    @Override
    public Cache getCache() {
        return cache;
    }

    /**
     * Initialize the cache; if it already exists it will not be recreated.
     * 
     * @return
     */
    @Override
    public void afterPropertiesSet() {
        CacheManager cacheManager = cacheManagerFactory.getObject();
        assert cacheManager != null;
        int maxElements = ConfigUtils.getInt( "gemma.cache.genelightweight.maxelements",
        		GENE_LIGHT_WEIGHT_CACHE_DEFAULT_MAX_ELEMENTS );
        int timeToLive = ConfigUtils.getInt( "gemma.cache.genelightweight.timetolive",
        		GENE_LIGHT_WEIGHT_CACHE_DEFAULT_TIME_TO_LIVE );
        int timeToIdle = ConfigUtils.getInt( "gemma.cache.genelightweight.timetoidle",
        		GENE_LIGHT_WEIGHT_CACHE_DEFAULT_TIME_TO_IDLE );

        boolean overFlowToDisk = ConfigUtils.getBoolean( "gemma.cache.genelightweight.usedisk",
        		GENE_LIGHT_WEIGHT_CACHE_DEFAULT_OVERFLOW_TO_DISK );

        boolean eternal = ConfigUtils.getBoolean( "gemma.cache.genelightweight.eternal",
        		GENE_LIGHT_WEIGHT_CACHE_DEFAULT_ETERNAL ) && timeToLive == 0;
        boolean terracottaEnabled = ConfigUtils.getBoolean( "gemma.cache.clustered", false );

        boolean diskPersistent = ConfigUtils.getBoolean( "gemma.cache.diskpersistent", false ) && !terracottaEnabled;

        /*
         * See TerracottaConfiguration.
         */
        int diskExpiryThreadIntervalSeconds = 600;
        int maxElementsOnDisk = 10000;
        boolean terracottaCoherentReads = false;
        boolean clearOnFlush = false;

        if ( terracottaEnabled ) {
            CacheConfiguration config = new CacheConfiguration( GENE_LIGHT_WEIGHT_CACHE_NAME, maxElements );
            config.setStatistics( false );
            config.setMemoryStoreEvictionPolicy( MemoryStoreEvictionPolicy.LRU.toString() );
            config.setOverflowToDisk( false );
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
            this.cache = new Cache( GENE_LIGHT_WEIGHT_CACHE_NAME, maxElements, MemoryStoreEvictionPolicy.LRU,
                    overFlowToDisk, null, eternal, timeToLive, timeToIdle, diskPersistent,
                    diskExpiryThreadIntervalSeconds, null );
        }

        cacheManager.addCache( cache );
    }
}
