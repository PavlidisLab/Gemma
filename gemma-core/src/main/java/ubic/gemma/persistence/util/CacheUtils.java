package ubic.gemma.persistence.util;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

/**
 * Created by tesarst on 04/04/17.
 * Provides common methods for cache manipulation.
 */
public class CacheUtils {

    /**
     * Either creates new Cache instance of given name, or retrieves it from the CacheManager, if it already exists.
     *
     * @param cacheManager      cache manager
     * @param cacheName         cache name
     * @param eternal           eternal
     * @param maxElements       max elements
     * @param overFlowToDisk    overflow to disk
     * @param timeToIdle        time to idle
     * @param timeToLive        time to live
     * @return newly created Cache instance, or existing one with given name.
     */
    public static Cache createOrLoadCache( CacheManager cacheManager, String cacheName,
            int maxElements, boolean overFlowToDisk, boolean eternal, int timeToIdle, int timeToLive ) {
        if ( !cacheManager.cacheExists( cacheName ) ) {
            Cache cache = new Cache( cacheName, maxElements, overFlowToDisk, eternal, timeToLive, timeToIdle );
            cacheManager.addCache( cache );
            return cache;
        } else {
            return cacheManager.getCache( cacheName );
        }
    }
}
