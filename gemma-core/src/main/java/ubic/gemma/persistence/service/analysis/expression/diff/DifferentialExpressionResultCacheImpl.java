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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.*;
import net.sf.ehcache.config.PersistenceConfiguration.Strategy;
import net.sf.ehcache.config.TimeoutBehaviorConfiguration.TimeoutBehaviorType;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.stereotype.Component;
import ubic.gemma.model.analysis.expression.diff.DiffExprGeneSearchResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionValueObject;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.persistence.util.Settings;

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

    private static final String CACHE_NAME_BASE = "DiffExResultCache";
    private static final int CACHE_DEFAULT_MAX_ELEMENTS = 1000000;
    private static final int CACHE_DEFAULT_TIME_TO_LIVE = 10000;
    private static final int CACHE_DEFAULT_TIME_TO_IDLE = 10000;
    private static final boolean CACHE_DEFAULT_ETERNAL = true;
    private static final boolean CACHE_DEFAULT_OVERFLOW_TO_DISK = false;

    private static final String TOP_HIT_CACHE_NAME_BASE = "TopDiffExResultCache";

    @Autowired
    private EhCacheManagerFactoryBean cacheManagerFactory;

    private Boolean enabled = true;

    private Cache cache;

    private Cache topHitsCache;

    @Override
    public void addToCache( DiffExprGeneSearchResult diffExForCache ) {
        Long r = diffExForCache.getResultSetId();
        Long g = diffExForCache.getGeneId();

        cache.put( new Element( new CacheKey( r, g ), diffExForCache ) );

    }

    @Override
    public void addToCache( Collection<DiffExprGeneSearchResult> diffExForCache ) {
        for ( DiffExprGeneSearchResult d : diffExForCache ) {
            this.addToCache( d );
        }
    }

    @Override
    public void clearCache() {
        cache.removeAll();
        topHitsCache.removeAll();
    }

    @Override
    public void clearCache( Long resultSetId ) {
        for ( Object o : cache.getKeys() ) {
            CacheKey k = ( CacheKey ) o;
            if ( k.resultSetId.equals( resultSetId ) ) {
                cache.remove( k );
            }
        }
    }

    @Override
    public void clearTopHitCache( Long resultSetId ) {
        this.topHitsCache.remove( resultSetId );
    }

    @Override
    public Collection<DiffExprGeneSearchResult> get( Long resultSet, Collection<Long> genes ) {
        assert cache != null;
        Collection<DiffExprGeneSearchResult> results = new HashSet<>();
        for ( Long g : genes ) {
            Element element = cache.get( new CacheKey( resultSet, g ) );
            if ( element != null ) {
                results.add( ( DiffExprGeneSearchResult ) element.getObjectValue() );
            }
        }
        return results;
    }

    @Override
    public DiffExprGeneSearchResult get( Long resultSet, Long g ) {
        assert cache != null;
        Element element = cache.get( new CacheKey( resultSet, g ) );
        if ( element == null )
            return null;
        return ( DiffExprGeneSearchResult ) element.getObjectValue();
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
        this.topHitsCache.put( new Element( resultSet.getId(), items ) );

    }

    @SuppressWarnings("unchecked")
    @Override
    public List<DifferentialExpressionValueObject> getTopHits( ExpressionAnalysisResultSet resultSet ) {
        Element element = this.topHitsCache.get( resultSet );
        if ( element == null )
            return null;
        return ( List<DifferentialExpressionValueObject> ) element.getObjectValue();
    }

    @Override
    public void afterPropertiesSet() {
        CacheManager cacheManager = cacheManagerFactory.getObject();
        int maxElements = Settings.getInt( "gemma.cache.diffex.maxelements",
                DifferentialExpressionResultCacheImpl.CACHE_DEFAULT_MAX_ELEMENTS );
        int timeToLive = Settings.getInt( "gemma.cache.diffex.timetolive",
                DifferentialExpressionResultCacheImpl.CACHE_DEFAULT_TIME_TO_LIVE );
        int timeToIdle = Settings.getInt( "gemma.cache.diffex.timetoidle",
                DifferentialExpressionResultCacheImpl.CACHE_DEFAULT_TIME_TO_IDLE );

        boolean eternal = Settings.getBoolean( "gemma.cache.diffex.eternal",
                DifferentialExpressionResultCacheImpl.CACHE_DEFAULT_ETERNAL ) && timeToLive == 0;
        boolean terracottaEnabled = Settings.getBoolean( "gemma.cache.clustered", true );
        boolean overFlowToDisk = Settings.getBoolean( "gemma.cache.diffex.usedisk",
                DifferentialExpressionResultCacheImpl.CACHE_DEFAULT_OVERFLOW_TO_DISK );
        boolean diskPersistent = Settings.getBoolean( "gemma.cache.diskpersistent", false ) && !terracottaEnabled;

        if ( !cacheManager.cacheExists( DifferentialExpressionResultCacheImpl.CACHE_NAME_BASE ) ) {
            /*
             * See TerracottaConfiguration.
             */
            int diskExpiryThreadIntervalSeconds = 600;
            int maxElementsOnDisk = 10000;
            boolean terracottaCoherentReads = false;
            boolean clearOnFlush = false;

            if ( terracottaEnabled ) {

                CacheConfiguration config = new CacheConfiguration(
                        DifferentialExpressionResultCacheImpl.CACHE_NAME_BASE, maxElements );
                config.setStatistics( false );
                config.setMemoryStoreEvictionPolicy( MemoryStoreEvictionPolicy.LRU.toString() );
                config.addPersistence( new PersistenceConfiguration().strategy( Strategy.NONE ) );
                config.setEternal( eternal );
                config.setTimeToIdleSeconds( timeToIdle );
                config.setMaxElementsOnDisk( maxElementsOnDisk );
                config.addTerracotta( new TerracottaConfiguration() );
                //noinspection ConstantConditions // Better readability
                config.getTerracottaConfiguration().setCoherentReads( terracottaCoherentReads );
                //noinspection ConstantConditions // Better readability
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
                this.topHitsCache = new Cache( config );
                this.topHitsCache.setName( DifferentialExpressionResultCacheImpl.TOP_HIT_CACHE_NAME_BASE );

            } else {
                this.cache = new Cache( DifferentialExpressionResultCacheImpl.CACHE_NAME_BASE, maxElements,
                        MemoryStoreEvictionPolicy.LRU, overFlowToDisk, null, eternal, timeToLive, timeToIdle,
                        diskPersistent, diskExpiryThreadIntervalSeconds, null );
                this.topHitsCache = new Cache( DifferentialExpressionResultCacheImpl.TOP_HIT_CACHE_NAME_BASE,
                        maxElements, MemoryStoreEvictionPolicy.LRU, overFlowToDisk, null, eternal, timeToLive,
                        timeToIdle, diskPersistent, diskExpiryThreadIntervalSeconds, null );
            }

            cacheManager.addCache( cache );
            cacheManager.addCache( topHitsCache );
        }

    }

}

class CacheKey implements Serializable {

    private static final long serialVersionUID = 1453661277282349121L;
    final Long resultSetId;
    private final Long geneId;

    CacheKey( Long resultSetId, Long geneId ) {
        this.resultSetId = resultSetId;
        this.geneId = geneId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( resultSetId == null ) ? 0 : resultSetId.hashCode() );
        result = prime * result + ( ( geneId == null ) ? 0 : geneId.hashCode() );
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
        CacheKey other = ( CacheKey ) obj;
        if ( resultSetId == null ) {
            if ( other.resultSetId != null )
                return false;
        } else if ( !resultSetId.equals( other.resultSetId ) )
            return false;
        if ( geneId == null ) {
            return other.geneId == null;
        } else
            return geneId.equals( other.geneId );
    }

}