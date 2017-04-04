package ubic.gemma.util;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.*;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

/**
 * Created by tesarst on 04/04/17.
 * Provides common methods for cache manipulation.
 */
public class CacheUtils {
    private static final int DISK_EXPIRY_THREAD_INTERVAL = 600;
    private static final int MAX_ELEMENTS_ON_DISK = 10000;

    /**
     * Either creates new Cache instance of given name, or retrieves it from the CacheManager, if it already exists.
     * @return newly created Cache instance, or existing one with given name.
     */
    public static Cache createOrLoadCache( CacheManager cacheManager, String cacheName, boolean terracottaEnabled,
            int maxElements, boolean overFlowToDisk, boolean eternal, int timeToIdle, int timeToLive,
            boolean diskPersistent ) {
        Cache cache;
        if ( !cacheManager.cacheExists( cacheName ) ) {
            if ( terracottaEnabled ) {
                CacheConfiguration config = new CacheConfiguration( cacheName, maxElements );
                config.setStatistics( false );
                config.setMemoryStoreEvictionPolicy( MemoryStoreEvictionPolicy.LRU.toString() );
                config.addPersistence(
                        new PersistenceConfiguration().strategy( PersistenceConfiguration.Strategy.NONE ) );
                config.setEternal( eternal );
                config.setTimeToIdleSeconds( timeToIdle );
                config.setMaxElementsOnDisk( MAX_ELEMENTS_ON_DISK );
                config.addTerracotta( new TerracottaConfiguration() );
                config.getTerracottaConfiguration().setCoherentReads( false );
                config.clearOnFlush( false );
                config.setTimeToLiveSeconds( timeToLive );
                config.getTerracottaConfiguration().setClustered( true );
                config.getTerracottaConfiguration().setValueMode( "SERIALIZATION" );
                NonstopConfiguration nonstopConfiguration = new NonstopConfiguration();
                TimeoutBehaviorConfiguration timeoutBehaviorConfiguration = new TimeoutBehaviorConfiguration();
                timeoutBehaviorConfiguration.setType( TimeoutBehaviorConfiguration.TimeoutBehaviorType.NOOP.getTypeName() );
                nonstopConfiguration.addTimeoutBehavior( timeoutBehaviorConfiguration );
                config.getTerracottaConfiguration().addNonstop( nonstopConfiguration );
                cache = new Cache( config );
            } else {
                cache = new Cache( cacheName, maxElements, MemoryStoreEvictionPolicy.LRU, overFlowToDisk, null, eternal,
                        timeToLive, timeToIdle, diskPersistent, DISK_EXPIRY_THREAD_INTERVAL, null );
            }
            cacheManager.addCache( cache );
        }else{
            cache = cacheManager.getCache( cacheName );
        }
        return cache;
    }
}
