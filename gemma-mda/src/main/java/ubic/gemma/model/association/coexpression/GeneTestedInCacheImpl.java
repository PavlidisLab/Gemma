/*
 * The gemma-mda project
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

package ubic.gemma.model.association.coexpression;

import java.util.Map;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.TimeoutBehaviorConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration.Strategy;
import net.sf.ehcache.config.TimeoutBehaviorConfiguration.TimeoutBehaviorType;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.stereotype.Component;

import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionTestedIn;
import ubic.gemma.util.Settings;

/**
 * @author paul
 * @version $Id$
 */
@Component
public class GeneTestedInCacheImpl implements InitializingBean, GeneTestedInCache {

    private static final boolean GENE_COEXPRESSIONTESTED_CACHE_DEFAULT_ETERNAL = true;

    private static final int GENE_COEXPRESSIONTESTED_CACHE_DEFAULT_MAX_ELEMENTS = 100000;

    private static final boolean GENE_COEXPRESSIONTESTED_CACHE_DEFAULT_OVERFLOW_TO_DISK = false;

    private static final int GENE_COEXPRESSIONTESTED_CACHE_DEFAULT_TIME_TO_IDLE = 604800;
    private static final int GENE_COEXPRESSIONTESTED_CACHE_DEFAULT_TIME_TO_LIVE = 1209600;
    private static final String GENE_COEXPRESSIONTESTED_CACHE_NAME = "Gene2GeneCoexpressionTestedInCache";
    private Cache cache;

    @Autowired
    private EhCacheManagerFactoryBean cacheManagerFactory;

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        CacheManager cacheManager = cacheManagerFactory.getObject();
        assert cacheManager != null;
        int maxElements = GENE_COEXPRESSIONTESTED_CACHE_DEFAULT_MAX_ELEMENTS; // FIXME no setting for this yet.

        // Other settings just use the gene2gene ones.
        int timeToLive = Settings.getInt( "gemma.cache.gene2gene.timetolive",
                GENE_COEXPRESSIONTESTED_CACHE_DEFAULT_TIME_TO_LIVE );
        int timeToIdle = Settings.getInt( "gemma.cache.gene2gene.timetoidle",
                GENE_COEXPRESSIONTESTED_CACHE_DEFAULT_TIME_TO_IDLE );

        boolean overFlowToDisk = Settings.getBoolean( "gemma.cache.gene2gene.usedisk",
                GENE_COEXPRESSIONTESTED_CACHE_DEFAULT_OVERFLOW_TO_DISK );

        boolean eternal = Settings.getBoolean( "gemma.cache.gene2gene.eternal",
                GENE_COEXPRESSIONTESTED_CACHE_DEFAULT_ETERNAL ) && timeToLive == 0;
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
            CacheConfiguration config = new CacheConfiguration( GENE_COEXPRESSIONTESTED_CACHE_NAME, maxElements );
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
            this.cache = new Cache( GENE_COEXPRESSIONTESTED_CACHE_NAME, maxElements, MemoryStoreEvictionPolicy.LRU,
                    overFlowToDisk, null, eternal, timeToLive, timeToIdle, diskPersistent,
                    diskExpiryThreadIntervalSeconds, null );
        }

        cacheManager.addCache( cache );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.coexpression.GeneTestedInCache#cache(java.util.Map)
     */
    @Override
    public void cache( Map<Long, GeneCoexpressionTestedIn> idMap ) {
        for ( GeneCoexpressionTestedIn v : idMap.values() ) {
            cacheTestedIn( v );
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.association.coexpression.GeneTestedInCache#cacheTestedIn(ubic.gemma.model.analysis.expression
     * .coexpression.GeneCoexpressionTestedIn)
     */
    @Override
    public void cacheTestedIn( GeneCoexpressionTestedIn testedIn ) {
        cache.put( new Element( testedIn.getGeneId(), testedIn ) );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.coexpression.GeneTestedInCache#clearCache()
     */
    @Override
    public void clearCache() {
        cache.removeAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.coexpression.GeneTestedInCache#contains(java.lang.Long)
     */
    @Override
    public boolean contains( Long geneId ) {
        return cache.isKeyInCache( geneId );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.coexpression.GeneTestedInCache#get(java.lang.Long)
     */
    @Override
    public GeneCoexpressionTestedIn get( Long geneId ) {
        if ( cache.isKeyInCache( geneId ) ) return ( GeneCoexpressionTestedIn ) cache.get( geneId ).getObjectValue();

        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.coexpression.GeneTestedInCache#remove(java.lang.Long)
     */
    @Override
    public void remove( Long id ) {
        if ( cache.isKeyInCache( id ) ) this.cache.remove( id );
    }
}
