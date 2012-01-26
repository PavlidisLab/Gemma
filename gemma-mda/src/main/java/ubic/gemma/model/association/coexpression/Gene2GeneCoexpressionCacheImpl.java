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

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.stereotype.Component;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import ubic.gemma.util.ConfigUtils;

/**
 * Configures the cache for gene2gene coexpression.
 * 
 * @author paul
 * @version $Id$
 */
@Component
public class Gene2GeneCoexpressionCacheImpl implements InitializingBean, Gene2GeneCoexpressionCache {

    @Autowired
    private EhCacheManagerFactoryBean cacheManagerFactory;

    private static final String GENE_COEXPRESSION_CACHE_NAME = "Gene2GeneCoexpressionCache";

    private static final int GENE_COEXPRESSION_CACHE_DEFAULT_MAX_ELEMENTS = 100000;
    private static final int GENE_COEXPRESSION_CACHE_DEFAULT_TIME_TO_LIVE = 10000;
    private static final int GENE_COEXPRESSION_CACHE_DEFAULT_TIME_TO_IDLE = 10000;
    private static final boolean GENE_COEXPRESSION_CACHE_DEFAULT_ETERNAL = true;
    private static final boolean GENE_COEXPRESSION_CACHE_DEFAULT_OVERFLOW_TO_DISK = true;
    private Cache cache;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionCache#clearCache()
     */
    @Override
    public void clearCache() {
        CacheManager manager = CacheManager.getInstance();
        manager.getCache( GENE_COEXPRESSION_CACHE_NAME ).removeAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionCache#getCache()
     */
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
        int maxElements = ConfigUtils.getInt( "gemma.cache.gene2gene.maxelements",
                GENE_COEXPRESSION_CACHE_DEFAULT_MAX_ELEMENTS );
        int timeToLive = ConfigUtils.getInt( "gemma.cache.gene2gene.timetolive",
                GENE_COEXPRESSION_CACHE_DEFAULT_TIME_TO_LIVE );
        int timeToIdle = ConfigUtils.getInt( "gemma.cache.gene2gene.timetoidle",
                GENE_COEXPRESSION_CACHE_DEFAULT_TIME_TO_IDLE );

        boolean overFlowToDisk = ConfigUtils.getBoolean( "gemma.cache.gene2gene.usedisk",
                GENE_COEXPRESSION_CACHE_DEFAULT_OVERFLOW_TO_DISK );

        boolean eternal = ConfigUtils.getBoolean( "gemma.cache.gene2gene.eternal",
                GENE_COEXPRESSION_CACHE_DEFAULT_ETERNAL ) && timeToLive == 0;
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
            CacheConfiguration config = new CacheConfiguration( GENE_COEXPRESSION_CACHE_NAME, maxElements );
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
            // this.cache = new Cache( GENE_COEXPRESSION_CACHE_NAME, maxElements, MemoryStoreEvictionPolicy.LRU,
            // overFlowToDisk, null, eternal, timeToLive, timeToIdle, diskPersistent,
            // diskExpiryThreadIntervalSeconds, null, null, maxElementsOnDisk, 10, clearOnFlush,
            // terracottaEnabled, "SERIALIZATION", terracottaCoherentReads );
        } else {
            this.cache = new Cache( GENE_COEXPRESSION_CACHE_NAME, maxElements, MemoryStoreEvictionPolicy.LRU,
                    overFlowToDisk, null, eternal, timeToLive, timeToIdle, diskPersistent,
                    diskExpiryThreadIntervalSeconds, null );
        }

        cacheManager.addCache( cache );
    }
}
